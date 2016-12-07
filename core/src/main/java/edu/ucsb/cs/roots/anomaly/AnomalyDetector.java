package edu.ucsb.cs.roots.anomaly;

import com.google.common.base.Strings;
import edu.ucsb.cs.roots.RootsEnvironment;
import edu.ucsb.cs.roots.scheduling.ScheduledItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

public abstract class AnomalyDetector extends ScheduledItem {

    protected final RootsEnvironment environment;
    protected final int historyLengthInSeconds;
    protected final String dataStore;
    protected final Properties properties;
    protected final Logger log = LoggerFactory.getLogger(getClass());

    private final Map<String,Long> lastAnomalyAt = new HashMap<>();

    public AnomalyDetector(RootsEnvironment environment, AnomalyDetectorBuilder builder) {
        super(builder.application, builder.periodInSeconds);
        checkNotNull(environment, "Environment must not be null");
        checkArgument(builder.historyLengthInSeconds > 0, "Period must be a positive integer");
        checkArgument(builder.historyLengthInSeconds % builder.periodInSeconds == 0,
                "History length must be a multiple of period");
        checkArgument(!Strings.isNullOrEmpty(builder.dataStore), "DataStore name is required");
        checkNotNull(builder.properties, "Properties must not be null");
        this.environment = environment;
        this.historyLengthInSeconds = builder.historyLengthInSeconds;
        this.dataStore = builder.dataStore;
        this.properties = builder.properties;
    }

    public final String getDataStore() {
        return dataStore;
    }

    public final String getProperty(String key, String def) {
        return properties.getProperty(key, def);
    }

    public long getLastAnomalyTime(String operation) {
        Long lastAnomaly = lastAnomalyAt.get(operation);
        if (lastAnomaly != null) {
            return lastAnomaly;
        }
        return -1L;
    }

    protected final Anomaly.Builder newAnomaly(long start, long end, String operation) {
        return Anomaly.newBuilder()
                .setDetector(this)
                .setStart(start)
                .setEnd(end)
                .setOperation(operation)
                .setPreviousAnomalyTime(getLastAnomalyTime(operation));
    }

    protected final void reportAnomaly(Anomaly anomaly) {
        lastAnomalyAt.put(anomaly.getOperation(), anomaly.getEnd());
        environment.publishEvent(anomaly);
    }

}
