package edu.ucsb.cs.roots.bi;

import com.google.common.base.Strings;
import edu.ucsb.cs.roots.RootsEnvironment;
import edu.ucsb.cs.roots.anomaly.Anomaly;
import edu.ucsb.cs.roots.anomaly.AnomalyLog;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.google.common.base.Preconditions.checkNotNull;

public abstract class BottleneckFinder {

    protected static final String LOCAL = "LOCAL";

    protected final RootsEnvironment environment;
    protected final Anomaly anomaly;

    protected final Logger log = LoggerFactory.getLogger(getClass());
    protected final AnomalyLog anomalyLog = new AnomalyLog(log);

    public BottleneckFinder(RootsEnvironment environment, Anomaly anomaly) {
        checkNotNull(environment, "Environment is required");
        checkNotNull(anomaly, "Anomaly is required");
        this.environment = environment;
        this.anomaly = anomaly;
    }

    abstract void analyze();

    protected final double getDoubleProperty(String name, double def) {
        String value = anomaly.getDetectorProperty(name, null);
        if (Strings.isNullOrEmpty(value)) {
            value = environment.getProperty(name, String.valueOf(def));
        }
        return Double.parseDouble(value);
    }
}
