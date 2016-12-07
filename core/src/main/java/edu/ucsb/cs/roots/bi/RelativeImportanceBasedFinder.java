package edu.ucsb.cs.roots.bi;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.ListMultimap;
import com.google.common.primitives.Doubles;
import edu.ucsb.cs.roots.RootsEnvironment;
import edu.ucsb.cs.roots.anomaly.Anomaly;
import edu.ucsb.cs.roots.changepoint.CustomPELTChangePointDetector;
import edu.ucsb.cs.roots.changepoint.Segment;
import edu.ucsb.cs.roots.data.ApiCall;
import edu.ucsb.cs.roots.data.ApplicationRequest;
import edu.ucsb.cs.roots.data.DataStore;
import edu.ucsb.cs.roots.data.DataStoreException;
import edu.ucsb.cs.roots.rlang.RClient;
import edu.ucsb.cs.roots.utils.ImmutableCollectors;
import org.apache.commons.math3.stat.descriptive.SummaryStatistics;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static com.google.common.base.Preconditions.checkArgument;

public final class RelativeImportanceBasedFinder extends BottleneckFinder {

    public static final String BI_PELT_PENALTY = "bi.pelt.penalty";
    public static final String BI_VERIFY_PERCENTILE = "bi.verify.percentile";

    private final double peltPenalty;
    private final double verifyPercentile;

    public RelativeImportanceBasedFinder(RootsEnvironment environment, Anomaly anomaly) {
        super(environment, anomaly);
        this.peltPenalty = getDoubleProperty(BI_PELT_PENALTY, 0.1);
        this.verifyPercentile = getDoubleProperty(BI_VERIFY_PERCENTILE, 99.0);
        checkArgument(this.verifyPercentile > 0 && this.verifyPercentile < 100,
                "Verify percentile must be in the interval (0,100)");
    }

    @Override
    void analyze() {
        long history = anomaly.getEnd() - anomaly.getStart();
        long start = Math.max(anomaly.getEnd() - 2 * history, anomaly.getPreviousAnomalyTime());
        DataStore ds = environment.getDataStoreService().get(anomaly.getDataStore());
        try {
            ImmutableList<ApplicationRequest> requests = ds.getRequestInfo(
                    anomaly.getApplication(), anomaly.getOperation(), start, anomaly.getEnd());
            log.debug("Received {} requests for analysis", requests.size());
            Map<String,List<ApplicationRequest>> perPathRequests = requests.stream().collect(
                    Collectors.groupingBy(ApplicationRequest::getPathAsString));
            perPathRequests.forEach((path,list) -> analyzePath(path, list, start));
        } catch (DataStoreException e) {
            anomalyLog.error(anomaly, "Error while retrieving API call data", e);
        }
    }

    private void analyzePath(String path, List<ApplicationRequest> requests, long start) {
        ImmutableList<ApiCall> apiCalls = requests.get(0).getApiCalls();
        int callCount = apiCalls.size();
        if (callCount == 0) {
            return;
        } else if (requests.size() < callCount + 2) {
            anomalyLog.warn(anomaly, "Insufficient data to perform a bottleneck identification");
            return;
        } else if (!requests.stream().anyMatch(r -> r.getTimestamp() >= anomaly.getStart())) {
            anomalyLog.info(anomaly, "No data points in anomaly window");
            return;
        }

        long period = anomaly.getPeriodInSeconds() * 1000;
        Map<Long,List<ApplicationRequest>> groupedByTime = requests.stream()
                .collect(Collectors.groupingBy(r -> groupByTime(r, start, period),
                        TreeMap::new, Collectors.toList()));

        try {
            ListMultimap<Long,RelativeImportance> results = computeRankings(apiCalls, groupedByTime);
            if (results.isEmpty()) {
                return;
            }
            ImmutableList<Long> sortedTimestamps = results.keySet().stream()
                    .sorted().collect(ImmutableCollectors.toList());
            Long lastTimestamp = Iterables.getLast(sortedTimestamps);
            List<RelativeImportance> lastRankings = results.get(lastTimestamp);
            anomalyLog.info(anomaly, getLogEntry(path, lastRankings));

            Bottleneck bottleneck = null;
            for (int i = 0; i < callCount + 1; i++) {
                final int rank = i + 1;
                int indexAtPos = findIndexByRank(lastRankings, rank);
                String apiCall = lastRankings.get(indexAtPos).getApiCall();
                if (log.isDebugEnabled()) {
                    log.debug("Analyzing historical trend for API call {} with ranking {}",
                            apiCall, rank);
                }
                Date onsetTime = analyzeHistory(results, sortedTimestamps, indexAtPos, apiCall);
                if (onsetTime != null) {
                    bottleneck = new Bottleneck(anomaly, apiCall, indexAtPos, onsetTime);
                    anomalyLog.info(anomaly, "Bottleneck identified; index: {}, {}", indexAtPos,
                            bottleneck.toString());
                    break;
                }
            }

            int indexAtPos = findIndexByRank(lastRankings, 1);
            if (bottleneck == null) {
                bottleneck = new Bottleneck(anomaly, lastRankings.get(indexAtPos).getApiCall(),
                        indexAtPos);
                anomalyLog.info(anomaly, "Bottleneck identified; index: {}, {}", indexAtPos,
                        bottleneck.toString());
            }
            bottleneck.setDetail(indexAtPos);

            PercentileBasedVerifier verifier = new PercentileBasedVerifier(groupedByTime,
                    bottleneck, anomalyLog, apiCalls, verifyPercentile);
            verifier.verify();

        } catch (Exception e) {
            anomalyLog.error(anomaly, "Error while computing rankings", e);
        }
    }

    private int findIndexByRank(List<RelativeImportance> lastRankings, int position) {
        return IntStream.range(0, lastRankings.size())
                .filter(index -> lastRankings.get(index).getRanking() == position)
                .findFirst().getAsInt();
    }

    private Date analyzeHistory(ListMultimap<Long, RelativeImportance> results,
                                List<Long> sortedTimestamps, int index, String apiCall) throws Exception {
        int offset = (int) sortedTimestamps.stream()
                .filter(timestamp -> timestamp < anomaly.getStart())
                .count();
        double[] trend = sortedTimestamps.subList(offset, sortedTimestamps.size()).stream()
                .mapToDouble(timestamp -> results.get(timestamp).get(index).getImportance())
                .toArray();
        log.debug("Performing change point analysis using {} data points", trend.length);
        CustomPELTChangePointDetector changePointDetector = new CustomPELTChangePointDetector(
                environment.getRService(), peltPenalty);
        Segment[] segments = changePointDetector.computeSegments(trend);
        return analyzeSegments(sortedTimestamps, offset, segments, apiCall);
    }

    private Date analyzeSegments(List<Long> sortedTimestamps,
                                 int offset, Segment[] segments, String apiCall) {
        int length = segments.length;
        if (length == 1) {
            anomalyLog.info(anomaly,
                    "No significant changes in relative importance to report for {}", apiCall);
            return null;
        }

        Date onsetTime = null;
        double maxDiff = 0D;
        for (int i = 1; i < length; i++) {
            Date date = new Date(sortedTimestamps.get(offset + segments[i].getStart()));
            anomalyLog.info(anomaly, "Relative importance level shift at {} for {}: {} --> {}",
                    date, apiCall, segments[i-1].getMean(), segments[i].getMean());
            double diff = segments[i].getMean() - segments[i-1].getMean();
            if (diff > maxDiff) {
                maxDiff = diff;
                onsetTime = date;
            }
        }
        anomalyLog.info(anomaly, "Net change in relative importance for {}: {} --> {} [{}%]",
                apiCall, segments[0].getMean(), segments[length -1].getMean(),
                segments[0].percentageIncrease(segments[length - 1]));
        return onsetTime;
    }

    private long groupByTime(ApplicationRequest r, long start, long period) {
        long index = (r.getTimestamp() - start) / period;
        return index * period + start;
    }

    private ListMultimap<Long,RelativeImportance> computeRankings(
            List<ApiCall> apiCalls, Map<Long, List<ApplicationRequest>> groupedByTime) throws Exception {
        long requestCount = 0;

        VectorDataSummary vectorSummary = new VectorDataSummary();
        groupedByTime.keySet().stream().forEach(timestamp ->
            groupedByTime.get(timestamp).forEach(vectorSummary::add)
        );
        if (log.isDebugEnabled()) {
            log.debug("Cutoff limit on local: {}", vectorSummary.getLimit());
        }

        ListMultimap<Long,RelativeImportance> results = ArrayListMultimap.create();
        List<Exception> rankingErrors = new ArrayList<>();
        RClient client = environment.getRService().borrow();
        try {
            client.evalAndAssign("df", "data.frame()");
            for (long timestamp : groupedByTime.keySet()) {
                for (ApplicationRequest request : groupedByTime.get(timestamp)) {
                    double[] responseTimeVector = getResponseTimeVector(request);
                    if (vectorSummary.isOutlier(responseTimeVector)) {
                        if (log.isDebugEnabled()) {
                            log.debug("Response time vector: {} (skipped)",
                                    Arrays.toString(responseTimeVector));
                        }
                        continue;
                    }
                    if (log.isDebugEnabled()) {
                        log.debug("Response time vector: {}", Arrays.toString(responseTimeVector));
                    }
                    client.assign("x", responseTimeVector);
                    client.evalAndAssign("df", "rbind(df, x)");
                    if (requestCount == 0) {
                        client.assign("df_names", getColumnNames(apiCalls.size(), true));
                        client.eval("names(df) = df_names");
                    }
                    requestCount++;
                }

                if (requestCount > apiCalls.size() + 1) {
                    try {
                        results.putAll(timestamp, computeRankings(client, apiCalls));
                    } catch (Exception e) {
                        rankingErrors.add(e);
                    }
                }
            }
        } finally {
            environment.getRService().release(client);
        }

        if (results.isEmpty() && !rankingErrors.isEmpty()) {
            throw new RuntimeException(rankingErrors.size() + " errors encountered",
                    Iterables.getLast(rankingErrors));
        }
        return results;
    }

    /**
     * Returns a List of RelativeImportance objects (one object per ApiCall). The returned list's
     * order corresponds to the order of the input ApiCall list. The rankings attribute on each
     * RelativeImportance instance is set according to the decreasing order of the relative
     * importance metric.
     */
    private List<RelativeImportance> computeRankings(RClient client,
                                                     List<ApiCall> apiCalls) throws Exception {
        client.evalAndAssign("model", "lm(Total ~ ., data=df)");
        client.evalAndAssign("rankings", "calc.relimp(model, type=c('lmg'))");
        double[] rankings = client.evalToDoubles("rankings$lmg");
        List<RelativeImportance> result = new ArrayList<>(rankings.length);
        for (int i = 0; i < rankings.length; i++) {
            result.add(new RelativeImportance(apiCalls.get(i).name(), rankings[i]));
        }
        result.add(new RelativeImportance(LOCAL, 1.0 - result.stream()
                .mapToDouble(RelativeImportance::getImportance).sum()));

        // Set rankings based on the importance score
        List<RelativeImportance> sorted = result.stream().sorted(Collections.reverseOrder())
                .collect(Collectors.toList());
        int rank = 1;
        for (RelativeImportance ri : sorted) {
            ri.setRanking(rank++);
        }
        return result;
    }

    private String[] getColumnNames(int callCount, boolean total) {
        String[] names;
        if (total) {
            names = new String[callCount + 1];
            names[callCount] = "Total";
        } else {
            names = new String[callCount];
        }
        for (int i = 0; i < callCount; i++) {
            names[i] = String.format("X%d", i + 1);
        }
        return names;
    }

    static double[] getResponseTimeVector(ApplicationRequest request) {
        List<Integer> vector = request.getApiCalls().stream().map(ApiCall::getTimeElapsed)
                .collect(Collectors.toCollection(ArrayList::new));
        vector.add(request.getResponseTime());
        return Doubles.toArray(vector);
    }

    private String getLogEntry(String path, List<RelativeImportance> result) {
        StringBuilder sb = new StringBuilder();
        sb.append("Relative importance metrics for path: ").append(path).append('\n');
        result.forEach(r -> sb.append(r).append('\n'));
        sb.append('\n');
        sb.append("Total variance explained: ").append(result.stream()
                .filter(r -> !r.getApiCall().equals(LOCAL))
                .mapToDouble(RelativeImportance::getImportance).sum());
        return sb.toString();
    }

    private static class VectorDataSummary {

        private final SummaryStatistics totalSummary = new SummaryStatistics();

        public void add(ApplicationRequest r) {
            int[] vector = PercentileBasedFinder.getResponseTimeVector(r);
            totalSummary.addValue(vector[vector.length - 1]);
        }

        private double getLimit() {
            return totalSummary.getMean() + 1.65 * totalSummary.getStandardDeviation();
        }

        public boolean isOutlier(double[] vector) {
            double local = vector[vector.length - 1] - IntStream.range(0, vector.length - 1)
                    .mapToDouble(i -> vector[i]).sum();
            return local > getLimit();
        }

    }
}
