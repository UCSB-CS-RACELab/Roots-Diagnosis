package edu.ucsb.cs.roots.anomaly;

import com.google.common.collect.*;
import edu.ucsb.cs.roots.RootsEnvironment;
import edu.ucsb.cs.roots.data.ApplicationRequest;
import edu.ucsb.cs.roots.data.DataStore;
import edu.ucsb.cs.roots.data.DataStoreException;
import edu.ucsb.cs.roots.utils.StatSummary;

import java.util.*;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkArgument;

public final class PathAnomalyDetector extends AnomalyDetector {

    private final Map<String,ListMultimap<String,PathRatio>> pathLevelHistory = new HashMap<>();
    private final ListMultimap<String,PathRatio> operationLevelHistory = ArrayListMultimap.create();
    private final double meanThreshold;
    private final boolean operationAnomalies;

    private long end = -1L;

    private PathAnomalyDetector(RootsEnvironment environment, Builder builder) {
        super(environment, builder);
        checkArgument(builder.meanThreshold > 0, "Mean threshold must be positive");
        this.meanThreshold = builder.meanThreshold;
        this.operationAnomalies = builder.operationAnomalies;
    }

    @Override
    public void run(long now) {
        long tempStart, tempEnd;
        if (end < 0) {
            tempEnd = now - 60 * 1000;
            tempStart = tempEnd - historyLengthInSeconds * 1000;
            // TODO: Get historical path distribution records
            end = tempEnd;
        }
        tempStart = end;
        tempEnd = end + periodInSeconds * 1000;

        try {
            updateHistory(tempStart, tempEnd);
            end = tempEnd;
        } catch (DataStoreException e) {
            String msg = "Error while retrieving data";
            log.error(msg, e);
            throw new RuntimeException(msg, e);
        }

        long cutoff = end - historyLengthInSeconds * 1000;
        pathLevelHistory.values().forEach(opHistory -> opHistory.values()
                .removeIf(pr -> pr.timestamp < cutoff));
        operationLevelHistory.values().removeIf(pr -> pr.timestamp < cutoff);
        pathLevelHistory.forEach((op, data) ->
                analyzePathDistributions(cutoff, end, op, data));
        if (operationAnomalies) {
            operationLevelHistory.keySet().forEach(op ->
                    analyzePathRatioTrend(String.format("Operation [%s: %s]", application, op),
                            operationLevelHistory.get(op), cutoff, end));
        }
    }

    private void analyzePathDistributions(long start, long end, String op,
                                          ListMultimap<String,PathRatio> pathData) {
        pathData.keySet().forEach(path -> {
            List<PathRatio> pathRatios = pathData.get(path);
            analyzePathRatioTrend(String.format("Path [%s: %s] %s", application, op, path),
                    pathRatios, start, end);
        });
    }

    private void analyzePathRatioTrend(String label, List<PathRatio> ratios, long start, long end) {
        StatSummary statistics = StatSummary.calculate(ratios.stream()
                .mapToDouble(v -> v.ratio));
        log.info("{} - Mean: {}, Std.Dev: {}, Count: {}", label, statistics.getMean(),
                statistics.getStandardDeviation(), ratios.size());
        PathRatio last = Iterables.getLast(ratios);
        if (statistics.isAnomaly(last.ratio, meanThreshold)) {
            String desc = String.format("Request distribution changed for - %s [%f%%]", label,
                    statistics.percentageDifference(last.ratio));
            ratios.removeIf(v -> v.timestamp < last.timestamp);
            Anomaly anomaly = Anomaly.newBuilder()
                    .setDetector(this)
                    .setStart(start)
                    .setEnd(end)
                    .setType(Anomaly.TYPE_WORKLOAD)
                    .setDescription(desc)
                    .setOperation(label)
                    .build();
            reportAnomaly(anomaly);
        }
    }

    private void updateHistory(long windowStart, long windowEnd) throws DataStoreException {
        if (log.isDebugEnabled()) {
            log.debug("Updating history for {} ({} - {})", application, new Date(windowStart),
                    new Date(windowEnd));
        }
        DataStore ds = environment.getDataStoreService().get(this.dataStore);
        ImmutableListMultimap<String,ApplicationRequest> requests = ds.getRequestInfo(
                application, windowStart, windowEnd);
        requests.keySet().forEach(op -> updateOperationHistory(op, requests.get(op), windowStart));

        pathLevelHistory.keySet().stream().filter(op -> !requests.containsKey(op)).forEach(op -> {
            // Inject 0's for operations not invoked in this window
            ListMultimap<String, PathRatio> pathData = pathLevelHistory.get(op);
            pathData.keySet().forEach(path -> {
                log.debug("No requests found for operation: {}, path: {}", op, path);
                pathData.put(path, PathRatio.zero(windowStart));
            });
        });

        long total = requests.size();
        List<PathRatio> longestPathHistory = getLongestHistory(operationLevelHistory);
        requests.keySet().forEach(op -> {
            if (!operationLevelHistory.containsKey(op)) {
                log.info("New operation detected. Application: {}; Operation: {}", application, op);
                longestPathHistory.forEach(p -> operationLevelHistory.put(op, PathRatio.zero(p.timestamp)));
            }
            operationLevelHistory.put(op, new PathRatio(windowStart, requests.get(op).size(), total));
        });

        // Inject 0's for operations not invoked in this window
        operationLevelHistory.keySet().stream()
                .filter(op -> !requests.containsKey(op))
                .forEach(op -> operationLevelHistory.put(op, PathRatio.zero(windowStart)));
    }

    private List<PathRatio> getLongestHistory(ListMultimap<String,PathRatio> map) {
        return map.keySet().stream().reduce((k1, k2) -> {
            if (map.get(k1).size() > map.get(k2).size()) {
                return k1;
            }
            return k2;
        }).map(map::get).orElse(ImmutableList.of());
    }

    private void updateOperationHistory(String op, ImmutableList<ApplicationRequest> perOpRequests,
                                        long windowStart) {
        ListMultimap<String,PathRatio> opHistory;
        if (pathLevelHistory.containsKey(op)) {
            opHistory = pathLevelHistory.get(op);
        } else {
            opHistory = ArrayListMultimap.create();
            pathLevelHistory.put(op, opHistory);
        }

        List<PathRatio> longestPathHistory = getLongestHistory(opHistory);
        Map<String,Integer> pathRequests = perOpRequests.stream()
                .collect(Collectors.groupingBy(ApplicationRequest::getPathAsString))
                .entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().size()));
        long totalRequests = pathRequests.values().stream().mapToInt(Integer::intValue).sum();
        pathRequests.forEach((path,count) -> {
            if (!opHistory.containsKey(path)) {
                log.info("New path detected. Application: {}; Operation: {}, Path: {}",
                        application, op, path);
                longestPathHistory.forEach(p -> opHistory.put(path, PathRatio.zero(p.timestamp)));
            }
            PathRatio pr = new PathRatio(windowStart, count, totalRequests);
            log.debug("Path ratio update. {}: {}", path, pr.ratio);
            opHistory.put(path, pr);
        });
        // Inject 0's for paths not invoked in this window
        opHistory.keySet().stream()
                .filter(path -> !pathRequests.containsKey(path))
                .forEach(path -> {
                    log.debug("No requests found for operation: {}, path: {}", op, path);
                    opHistory.put(path, PathRatio.zero(windowStart));
                });
    }

    private final static class PathRatio {
        private final long timestamp;
        private final double ratio;

        PathRatio(long timestamp, long count, long total) {
            checkArgument(total > 0, "Division by zero");
            this.timestamp = timestamp;
            this.ratio = (count * 100.0)/total;
        }

        static PathRatio zero(long timestamp) {
            return new PathRatio(timestamp, 0, 1);
        }
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    public static class Builder extends AnomalyDetectorBuilder<PathAnomalyDetector,Builder> {

        private double meanThreshold = 2.0;
        private boolean operationAnomalies = false;

        private Builder() {
        }

        public Builder setMeanThreshold(double meanThreshold) {
            this.meanThreshold = meanThreshold;
            return this;
        }

        public Builder setOperationAnomalies(boolean operationAnomalies) {
            this.operationAnomalies = operationAnomalies;
            return this;
        }

        @Override
        public PathAnomalyDetector build(RootsEnvironment environment) {
            return new PathAnomalyDetector(environment, this);
        }

        @Override
        protected Builder getThisObj() {
            return this;
        }
    }
}
