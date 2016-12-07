package edu.ucsb.cs.roots.bi;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import edu.ucsb.cs.roots.anomaly.Anomaly;
import edu.ucsb.cs.roots.anomaly.AnomalyLog;
import edu.ucsb.cs.roots.data.ApiCall;
import edu.ucsb.cs.roots.data.ApplicationRequest;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

public class PercentileBasedVerifier {

    private final Map<Long,List<ApplicationRequest>> requests;
    private final Bottleneck bottleneck;
    private final AnomalyLog anomalyLog;
    private final ImmutableList<ApiCall> apiCalls;
    private final double percentile;

    public PercentileBasedVerifier(Map<Long, List<ApplicationRequest>> requests,
                                   Bottleneck bottleneck, AnomalyLog anomalyLog,
                                   ImmutableList<ApiCall> apiCalls, double percentile) {
        this.requests = requests;
        this.bottleneck = bottleneck;
        this.anomalyLog = anomalyLog;
        this.apiCalls = apiCalls;
        this.percentile = percentile;
    }

    public void verify() {
        Anomaly anomaly = bottleneck.getAnomaly();
        Date onsetTime = bottleneck.getOnsetTime();
        if (onsetTime == null) {
            anomalyLog.info(anomaly, "Onset time not available for verification");
        }

        ImmutableList<DescriptiveStatistics> stats = PercentileBasedFinder.initStatistics(apiCalls.size());
        ApplicationRequest cutoff = null;
        for (long timestamp : requests.keySet()) {
            if (timestamp < anomaly.getStart()) {
                continue;
            }
            List<ApplicationRequest> batch = requests.get(timestamp);
            batch.forEach(r -> {
                int[] timeValues = PercentileBasedFinder.getResponseTimeVector(r);
                for (int i = 0; i < timeValues.length; i++) {
                    stats.get(i).addValue(timeValues[i]);
                }
            });
            cutoff = Iterables.getLast(batch);
        }
        if (cutoff != null) {
            anomalyLog.info(anomaly, "Cut off request vector: {}",
                    Arrays.toString(RelativeImportanceBasedFinder.getResponseTimeVector(cutoff)));
        }

        double[] percentileResults = new double[stats.size()];
        for (int i = 0; i < stats.size(); i++) {
            double value = stats.get(i).getPercentile(percentile);
            String apiCall;
            if (i != stats.size() - 1) {
                apiCall = apiCalls.get(i).name();
            } else {
                apiCall = "LOCAL";
            }
            percentileResults[i] = value;
            anomalyLog.info(anomaly, "{}p for {}: {}", percentile, apiCall, value);
        }

        int maxIndex = -1;
        double maxValue = -1D;
        for (int i = 0; i < percentileResults.length; i++) {
            if (percentileResults[i] > maxValue) {
                maxIndex = i;
                maxValue = percentileResults[i];
            }
        }
        anomalyLog.info(anomaly, "Verification result; onset: {}, percentiles: {} ri: {} match: {}",
                onsetTime != null, maxIndex, bottleneck.getIndex(), maxIndex == bottleneck.getIndex());
        comparePercentiles(percentileResults, maxIndex, bottleneck.getAnomaly());
    }

    private void comparePercentiles(double[] percentileResults, int maxIndex, Anomaly anomaly) {
        List<AnomalousValue> anomalousValues = new ArrayList<>();
        AtomicInteger counter = new AtomicInteger(0);
        for (long timestamp : requests.keySet()) {
            if (timestamp < anomaly.getStart()) {
                continue;
            }
            List<ApplicationRequest> batch = requests.get(timestamp);
            batch.forEach(r -> {
                counter.incrementAndGet();
                int[] vector = PercentileBasedFinder.getResponseTimeVector(r);
                for (int i = 0; i < vector.length; i++) {
                    if (vector[i] > percentileResults[i]) {
                        anomalousValues.add(new AnomalousValue(i, vector[i], percentileResults[i],
                                r.getTimestamp()));
                    }
                }
            });
        }
        Collections.sort(anomalousValues, Collections.reverseOrder());
        for (int i = 0; i < 5; i++) {
            if (anomalousValues.size() > i) {
                anomalyLog.info(anomaly, "Top {} anomalous value; {}", i + 1,
                        anomalousValues.get(i).toString());
            }
        }

        if (!anomalousValues.isEmpty()) {
            AnomalousValue top = anomalousValues.get(0);
            anomalyLog.info(anomaly, "Secondary verification result; percentiles: {} " +
                    "percentiles2: {} ri: {} match: {} ri_onset: {} ri_top: {} data_points: {}",
                    maxIndex, top.index, bottleneck.getIndex(), top.index == bottleneck.getIndex(),
                    bottleneck.getOnsetTime() != null, bottleneck.getDetail(), counter.get());
        }
    }

    private static class AnomalousValue implements Comparable<AnomalousValue> {
        private final int index;
        private final double value;
        private final double percentile;
        private final long timestamp;

        private AnomalousValue(int index, double value, double percentile, long timestamp) {
            this.index = index;
            this.value = value;
            this.percentile = percentile;
            this.timestamp = timestamp;
        }

        private double getPercentageIncrease() {
            return (value - percentile)*100.0/percentile;
        }

        @Override
        public int compareTo(AnomalousValue other) {
            return Double.compare(getPercentageIncrease(), other.getPercentageIncrease());
        }

        @Override
        public String toString() {
            return new StringBuilder("index: ").append(index)
                    .append(" timestamp: ").append(new Date(timestamp))
                    .append(" values: ").append(percentile).append(" --> ").append(value)
                    .toString();
        }
    }

}
