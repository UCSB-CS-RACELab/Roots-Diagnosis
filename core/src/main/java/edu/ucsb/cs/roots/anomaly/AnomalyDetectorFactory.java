package edu.ucsb.cs.roots.anomaly;

import com.google.common.base.Strings;
import edu.ucsb.cs.roots.RootsEnvironment;

import java.util.Properties;
import java.util.concurrent.TimeUnit;

import static com.google.common.base.Preconditions.checkArgument;

public class AnomalyDetectorFactory {

    private static final String APPLICATION = "application";
    private static final String DETECTOR = "detector";
    private static final String DETECTOR_PERIOD = "period";
    private static final String DETECTOR_PERIOD_TIME_UNIT = DETECTOR_PERIOD + ".timeUnit";
    private static final String DETECTOR_DATA_STORE = "dataStore";

    private static final String DETECTOR_HISTORY_LENGTH = "history";
    private static final String DETECTOR_HISTORY_LENGTH_TIME_UNIT = DETECTOR_HISTORY_LENGTH + ".timeUnit";
    private static final String DETECTOR_CORRELATION_THRESHOLD = "correlationThreshold";
    private static final String DETECTOR_DTW_INCREASE_THRESHOLD = "dtwIncreaseThreshold";
    private static final String DETECTOR_DTW_MEAN_THRESHOLD = "dtwMeanThreshold";
    private static final String DETECTOR_DTW_ANALYSIS = "dtwAnalysis";

    private static final String DETECTOR_RESPONSE_TIME_UPPER_BOUND = "responseTimeUpperBound";
    private static final String DETECTOR_SLO_PERCENTAGE = "sloPercentage";
    private static final String DETECTOR_MIN_SAMPLES = "minimumSamples";

    private static final String DETECTOR_MEAN_THRESHOLD = "meanThreshold";
    private static final String DETECTOR_OPERATION_ANOMALIES = "operationAnomalies";

    public static AnomalyDetector create(RootsEnvironment environment, Properties properties) {
        String application = properties.getProperty(APPLICATION);
        checkArgument(!Strings.isNullOrEmpty(application), "Application name is required");

        String detectorType = properties.getProperty(DETECTOR);
        checkArgument(!Strings.isNullOrEmpty(detectorType), "Detector type is required");

        AnomalyDetectorBuilder builder;
        if (CorrelationBasedDetector.class.getSimpleName().equals(detectorType)) {
            builder = initCorrelationBasedDetector(properties);
        } else if (SLOBasedDetector.class.getSimpleName().equals(detectorType)) {
            builder = initSLOBasedDetector(properties);
        } else if (PathAnomalyDetector.class.getSimpleName().equals(detectorType)) {
            builder = initPathAnomalyDetector(properties);
        } else {
            throw new IllegalArgumentException("Unknown anomaly detector type: " + detectorType);
        }

        String period = properties.getProperty(DETECTOR_PERIOD);
        if (!Strings.isNullOrEmpty(period)) {
            TimeUnit timeUnit = TimeUnit.valueOf(properties.getProperty(
                    DETECTOR_PERIOD_TIME_UNIT, "SECONDS"));
            builder.setPeriodInSeconds((int) timeUnit.toSeconds(Integer.parseInt(period)));
        }

        String dataStore = properties.getProperty(DETECTOR_DATA_STORE);
        if (!Strings.isNullOrEmpty(dataStore)) {
            builder.setDataStore(dataStore);
        }

        return builder.setApplication(application)
                .setProperties(properties).build(environment);
    }

    private static CorrelationBasedDetector.Builder initCorrelationBasedDetector(
            Properties properties) {
        CorrelationBasedDetector.Builder builder = CorrelationBasedDetector.newBuilder();
        String historyLength = properties.getProperty(DETECTOR_HISTORY_LENGTH);
        if (!Strings.isNullOrEmpty(historyLength)) {
            TimeUnit historyTimeUnit = TimeUnit.valueOf(properties.getProperty(
                    DETECTOR_HISTORY_LENGTH_TIME_UNIT, "SECONDS"));
            builder.setHistoryLengthInSeconds((int) historyTimeUnit.toSeconds(
                    Integer.parseInt(historyLength)));
        }

        String correlationThreshold = properties.getProperty(DETECTOR_CORRELATION_THRESHOLD);
        if (!Strings.isNullOrEmpty(correlationThreshold)) {
            builder.setCorrelationThreshold(Double.parseDouble(correlationThreshold));
        }

        String dtwMeanThreshold = properties.getProperty(DETECTOR_DTW_MEAN_THRESHOLD);
        if (!Strings.isNullOrEmpty(dtwMeanThreshold)) {
            builder.setDtwMeanThreshold(Double.parseDouble(dtwMeanThreshold));
        }

        String dtwIncreaseThreshold = properties.getProperty(DETECTOR_DTW_INCREASE_THRESHOLD);
        if (!Strings.isNullOrEmpty(dtwIncreaseThreshold)) {
            builder.setDtwIncreaseThreshold(Double.parseDouble(dtwIncreaseThreshold));
        }

        String dtwAnalysis = properties.getProperty(DETECTOR_DTW_ANALYSIS);
        if (!Strings.isNullOrEmpty(dtwAnalysis)) {
            builder.setDtwAnalysis(dtwAnalysis);
        }
        return builder;
    }

    private static SLOBasedDetector.Builder initSLOBasedDetector(Properties properties) {
        SLOBasedDetector.Builder builder = SLOBasedDetector.newBuilder();
        String historyLength = properties.getProperty(DETECTOR_HISTORY_LENGTH);
        if (!Strings.isNullOrEmpty(historyLength)) {
            TimeUnit historyTimeUnit = TimeUnit.valueOf(properties.getProperty(
                    DETECTOR_HISTORY_LENGTH_TIME_UNIT, "SECONDS"));
            builder.setHistoryLengthInSeconds((int) historyTimeUnit.toSeconds(
                    Integer.parseInt(historyLength)));
        }

        String responseTimeUpperBound = properties.getProperty(DETECTOR_RESPONSE_TIME_UPPER_BOUND);
        checkArgument(!Strings.isNullOrEmpty(responseTimeUpperBound),
                "Response time upper bound is required");
        builder.setResponseTimeUpperBound(Integer.parseInt(responseTimeUpperBound));

        String sloPercentage = properties.getProperty(DETECTOR_SLO_PERCENTAGE);
        if (!Strings.isNullOrEmpty(sloPercentage)) {
            builder.setSloPercentage(Double.parseDouble(sloPercentage));
        }

        String minimumSamples = properties.getProperty(DETECTOR_MIN_SAMPLES);
        if (!Strings.isNullOrEmpty(minimumSamples)) {
            builder.setMinimumSamples(Integer.parseInt(minimumSamples));
        }

        return builder;
    }

    private static PathAnomalyDetector.Builder initPathAnomalyDetector(Properties properties) {
        PathAnomalyDetector.Builder builder = PathAnomalyDetector.newBuilder();
        String historyLength = properties.getProperty(DETECTOR_HISTORY_LENGTH);
        if (!Strings.isNullOrEmpty(historyLength)) {
            TimeUnit historyTimeUnit = TimeUnit.valueOf(properties.getProperty(
                    DETECTOR_HISTORY_LENGTH_TIME_UNIT, "SECONDS"));
            builder.setHistoryLengthInSeconds((int) historyTimeUnit.toSeconds(
                    Integer.parseInt(historyLength)));
        }

        String meanThreshold = properties.getProperty(DETECTOR_MEAN_THRESHOLD);
        if (!Strings.isNullOrEmpty(meanThreshold)) {
            builder.setMeanThreshold(Double.parseDouble(meanThreshold));
        }

        String operationAnomalies = properties.getProperty(DETECTOR_OPERATION_ANOMALIES);
        if (!Strings.isNullOrEmpty(operationAnomalies)) {
            builder.setOperationAnomalies(Boolean.parseBoolean(operationAnomalies));
        }
        return builder;
    }

}
