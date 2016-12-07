package edu.ucsb.cs.roots.anomaly;

import com.google.common.collect.*;
import edu.ucsb.cs.roots.RootsEnvironment;
import edu.ucsb.cs.roots.data.BenchmarkResult;
import edu.ucsb.cs.roots.data.DataStore;
import edu.ucsb.cs.roots.data.DataStoreException;
import edu.ucsb.cs.roots.utils.ImmutableCollectors;

import java.util.*;

import static com.google.common.base.Preconditions.checkArgument;

public final class SLOBasedDetector extends AnomalyDetector {

    private final ListMultimap<String,BenchmarkResult> history;
    private final int responseTimeUpperBound;
    private final double sloPercentage;
    private final double minimumSamples;

    private long end = -1L;

    private SLOBasedDetector(RootsEnvironment environment, Builder builder) {
        super(environment, builder);
        checkArgument(builder.historyLengthInSeconds > 0, "History length must be positive");
        checkArgument(builder.minimumSamples > 0, "Minimum samples must be positive");
        checkArgument(builder.responseTimeUpperBound > 0,
                "Response time upper bound must be positive");
        checkArgument(builder.sloPercentage > 0 && builder.sloPercentage < 100,
                "SLO percentage must be in the interval (0,100)");
        this.history = ArrayListMultimap.create();
        this.responseTimeUpperBound = builder.responseTimeUpperBound;
        this.sloPercentage = builder.sloPercentage;
        this.minimumSamples = builder.minimumSamples;
    }

    @Override
    public void run(long now) {
        Collection<String> requestTypes;
        try {
            long tempStart, tempEnd;
            if (end < 0) {
                tempEnd = now - 60 * 1000;
                tempStart = tempEnd - historyLengthInSeconds * 1000;
            } else {
                tempStart = end;
                tempEnd = end + periodInSeconds * 1000;
            }

            requestTypes = updateHistory(tempStart, tempEnd);
            end = tempEnd;
        } catch (DataStoreException e) {
            String msg = "Error while retrieving data";
            log.error(msg, e);
            throw new RuntimeException(msg, e);
        }

        long cutoff = end - historyLengthInSeconds * 1000;
        history.values().removeIf(l -> l.getTimestamp() < cutoff);
        history.keySet().stream()
                .filter(op -> requestTypes.contains(op) &&
                        history.get(op).size() >= minimumSamples)
                .forEach(op -> computeSLO(cutoff, end, op, history.get(op)));
    }

    private Collection<String> updateHistory(long windowStart,
                                             long windowEnd) throws DataStoreException {
        checkArgument(windowStart < windowEnd, "Start time must precede end time");
        if (log.isDebugEnabled()) {
            log.debug("Updating history for {} ({} - {})", application, new Date(windowStart),
                    new Date(windowEnd));
        }
        DataStore ds = environment.getDataStoreService().get(this.dataStore);
        ImmutableListMultimap<String,BenchmarkResult> summaries =
                ds.getBenchmarkResults(application, windowStart, windowEnd);
        history.putAll(summaries);
        return ImmutableList.copyOf(summaries.keySet());
    }

    private void computeSLO(long start, long end, String operation,
                            Collection<BenchmarkResult> results) {
        long lastAnomaly = getLastAnomalyTime(operation);
        ImmutableList<BenchmarkResult> filteredResults = results.stream()
                .filter(r -> r.getTimestamp() >= lastAnomaly)
                .collect(ImmutableCollectors.toList());

        int sampleSize = filteredResults.size();
        if (sampleSize < minimumSamples) {
            log.debug("Insufficient samples to calculate SLO. Required {}, got {}",
                    minimumSamples, sampleSize);
            return;
        }
        log.debug("Calculating SLO with {} data points.", sampleSize);
        long satisfied = filteredResults.stream()
                .filter(r -> r.getResponseTime() <= responseTimeUpperBound)
                .count();
        double sloSupported = satisfied * 100.0 / sampleSize;
        log.info("SLO metrics. Supported: {}, Expected: {}", sloSupported, sloPercentage);
        if (sloSupported < sloPercentage) {
            Anomaly anomaly = newAnomaly(start, end, operation)
                    .setType(Anomaly.TYPE_PERFORMANCE)
                    .setDescription(String.format("SLA satisfaction: %.4f", sloSupported))
                    .build();
            reportAnomaly(anomaly);
        }
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    public static class Builder extends AnomalyDetectorBuilder<SLOBasedDetector, Builder> {

        private int responseTimeUpperBound;
        private double sloPercentage = 95.0;
        private int minimumSamples = 30;

        private Builder() {
        }

        public Builder setResponseTimeUpperBound(int responseTimeUpperBound) {
            this.responseTimeUpperBound = responseTimeUpperBound;
            return this;
        }

        public Builder setSloPercentage(double sloPercentage) {
            this.sloPercentage = sloPercentage;
            return this;
        }

        public Builder setMinimumSamples(int minimumSamples) {
            this.minimumSamples = minimumSamples;
            return this;
        }

        @Override
        public SLOBasedDetector build(RootsEnvironment environment) {
            return new SLOBasedDetector(environment, this);
        }

        @Override
        protected Builder getThisObj() {
            return this;
        }
    }
}
