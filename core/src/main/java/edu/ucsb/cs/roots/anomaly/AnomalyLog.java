package edu.ucsb.cs.roots.anomaly;

import org.slf4j.Logger;

public final class AnomalyLog {

    private static  final String PREFIX = "Anomaly ({}, {}, {}): ";

    private final Logger log;

    public AnomalyLog(Logger log) {
        this.log = log;
    }

    public void info(Anomaly anomaly, String msg, Object... args) {
        log.info(PREFIX + msg, getObjects(anomaly, args));
    }

    public void warn(Anomaly anomaly, String msg, Object... args) {
        log.warn(PREFIX + msg, getObjects(anomaly, args));
    }

    public void error(Anomaly anomaly, String msg, Object... args) {
        log.error(PREFIX + msg, getObjects(anomaly, args));
    }

    private Object[] getObjects(Anomaly anomaly, Object[] args) {
        Object[] objects = new Object[3 + args.length];
        objects[0] = anomaly.getId();
        objects[1] = anomaly.getApplication();
        objects[2] = anomaly.getOperation();
        System.arraycopy(args, 0, objects, 3, args.length);
        return objects;
    }
}
