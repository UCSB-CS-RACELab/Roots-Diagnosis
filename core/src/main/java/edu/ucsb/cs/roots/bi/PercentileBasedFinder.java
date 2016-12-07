package edu.ucsb.cs.roots.bi;

import com.google.common.collect.ImmutableList;
import edu.ucsb.cs.roots.RootsEnvironment;
import edu.ucsb.cs.roots.anomaly.Anomaly;
import edu.ucsb.cs.roots.data.ApiCall;
import edu.ucsb.cs.roots.data.ApplicationRequest;
import edu.ucsb.cs.roots.data.DataStore;
import edu.ucsb.cs.roots.data.DataStoreException;
import edu.ucsb.cs.roots.utils.ImmutableCollectors;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.apache.commons.math3.stat.descriptive.rank.Percentile;

import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static com.google.common.base.Preconditions.checkArgument;

public final class PercentileBasedFinder extends BottleneckFinder {

    public static final String BI_PERCENTILE = "bi.percentile";

    private final double percentile;

    public PercentileBasedFinder(RootsEnvironment environment, Anomaly anomaly) {
        super(environment, anomaly);
        this.percentile = getDoubleProperty(BI_PERCENTILE, 95.0);
        checkArgument(this.percentile > 0 && this.percentile < 100,
                "Percentile must be in the interval (0,100)");
    }

    @Override
    void analyze() {
        long history = anomaly.getEnd() - anomaly.getStart();
        long start = anomaly.getEnd() - 3 * history;
        DataStore ds = environment.getDataStoreService().get(anomaly.getDataStore());
        try {
            ImmutableList<ApplicationRequest> requests = ds.getRequestInfo(
                    anomaly.getApplication(), anomaly.getOperation(), start, anomaly.getEnd());
            log.debug("Received {} requests for analysis", requests.size());
            Map<String,List<ApplicationRequest>> perPathRequests = requests.stream().collect(
                    Collectors.groupingBy(ApplicationRequest::getPathAsString));
            perPathRequests.forEach(this::analyze);
        } catch (DataStoreException e) {
            anomalyLog.error(anomaly, "Error while retrieving API call data", e);
        }
    }

    private void analyze(String path, List<ApplicationRequest> requests) {
        if (log.isDebugEnabled()) {
            log.debug("Analyzing program path: {} ({})", path, path.hashCode());
        }

        ImmutableList<ApplicationRequest> oldRequests = requests.stream()
                .filter(r -> r.getTimestamp() < anomaly.getStart())
                .collect(ImmutableCollectors.toList());
        if (oldRequests.isEmpty()) {
            log.warn("Insufficient data to compute percentiles");
            return;
        }
        double[] percentiles = computePercentiles(oldRequests);
        if (log.isDebugEnabled()) {
            log.debug("Percentiles computed using {} data points: {}",
                    oldRequests.size(), Arrays.toString(percentiles));
        }
        requests.stream().filter(r -> r.getTimestamp() >= anomaly.getStart())
                .forEach(r -> checkForAnomalies(r, percentiles, path));
    }

    /**
     * Returns an array of length n+1 where the first n entries correspond to the response time of
     * n API calls made by the request. The last entry corresponds to the LOCAL response time
     * (i.e. Total - sum(apiCalls)).
     */
    static int[] getResponseTimeVector(ApplicationRequest r) {
        int apiCalls = r.getApiCalls().size();
        int[] timeValues = new int[apiCalls + 1];
        for (int i = 0; i < apiCalls; i++) {
            timeValues[i] = r.getApiCalls().get(i).getTimeElapsed();
        }
        timeValues[apiCalls] = r.getResponseTime() - IntStream.of(timeValues).sum();
        return timeValues;
    }

    private void checkForAnomalies(ApplicationRequest r, double[] percentiles, String path) {
        int[] timeValues = getResponseTimeVector(r);
        if (log.isDebugEnabled()) {
            log.debug("Response time vector (check): {}", Arrays.toString(timeValues));
        }

        int apiCalls = r.getApiCalls().size();
        for (int i = 0; i < apiCalls; i++) {
            if (timeValues[i] > percentiles[i]) {
                anomalyLog.info(
                        anomaly, "Anomalous API call execution in path {} at {} in {}: {} [> {} ({}p)]",
                        path.hashCode(), new Date(r.getTimestamp()), r.getApiCalls().get(i).name(),
                        timeValues[i], percentiles[i], percentile);
            }
        }

        if (timeValues[apiCalls] > percentiles[apiCalls]) {
            anomalyLog.info(
                    anomaly, "Anomalous local execution in path {} at {} in LOCAL: {} [> {} ({}p)]",
                    path.hashCode(), new Date(r.getTimestamp()), timeValues[apiCalls],
                    percentiles[apiCalls], percentile);
        }
    }

    private double[] computePercentiles(List<ApplicationRequest> requests) {
        ImmutableList<ApiCall> apiCalls = requests.get(0).getApiCalls();
        ImmutableList<DescriptiveStatistics> stats = initStatistics(apiCalls.size());
        requests.forEach(r -> {
            int[] timeValues = getResponseTimeVector(r);
            if (log.isDebugEnabled()) {
                log.debug("Response time vector (learn): {}", Arrays.toString(timeValues));
            }

            for (int i = 0; i < timeValues.length; i++) {
                stats.get(i).addValue(timeValues[i]);
            }
        });

        return stats.stream().mapToDouble(s -> s.getPercentile(percentile)).toArray();
    }

    static ImmutableList<DescriptiveStatistics> initStatistics(int apiCalls) {
        int size = apiCalls + 1;
        ImmutableList.Builder<DescriptiveStatistics> stats = ImmutableList.builder();
        for (int i = 0; i < size; i++) {
            DescriptiveStatistics statistics = new DescriptiveStatistics();
            statistics.setPercentileImpl(new Percentile()
                    .withEstimationType(Percentile.EstimationType.R_7));
            stats.add(statistics);
        }
        return stats.build();
    }
}
