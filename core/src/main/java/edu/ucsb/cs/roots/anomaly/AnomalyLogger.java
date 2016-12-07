package edu.ucsb.cs.roots.anomaly;

import com.google.common.eventbus.Subscribe;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;

public final class AnomalyLogger {

    private final AnomalyLog anomalyLog;

    AnomalyLogger() {
        Logger log = LoggerFactory.getLogger(AnomalyLogger.class);
        log.info("Initializing AnomalyLogger");
        this.anomalyLog = new AnomalyLog(log);
    }

    @Subscribe
    public void log(Anomaly anomaly) {
        anomalyLog.warn(anomaly, "Detected at {}: {}", new Date(anomaly.getEnd()),
                anomaly.getDescription());
    }

}
