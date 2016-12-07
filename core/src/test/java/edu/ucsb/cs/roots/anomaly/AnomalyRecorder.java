package edu.ucsb.cs.roots.anomaly;

import com.google.common.collect.ImmutableList;
import com.google.common.eventbus.Subscribe;

import java.util.ArrayList;
import java.util.List;

public class AnomalyRecorder {

    private final List<Anomaly> anomalies = new ArrayList<>();

    @Subscribe
    public void record(Anomaly anomaly) {
        anomalies.add(anomaly);
    }

    public List<Anomaly> getAndClearAnomalies() {
        ImmutableList<Anomaly> copy = ImmutableList.copyOf(anomalies);
        anomalies.clear();
        return copy;
    }

}
