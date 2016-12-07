package edu.ucsb.cs.roots.anomaly;

import edu.ucsb.cs.roots.ConfigLoader;
import edu.ucsb.cs.roots.RootsEnvironment;
import edu.ucsb.cs.roots.scheduling.SchedulerService;


import java.util.Properties;
import java.util.stream.Stream;

public final class AnomalyDetectorService extends SchedulerService<AnomalyDetector> {

    private static final String ANOMALY_DETECTOR_GROUP = "anomaly-detector";
    private static final String ANOMALY_DETECTOR_THREAD_POOL = "detector.threadPool";
    private static final String ANOMALY_DETECTOR_THREAD_COUNT = "detector.threadCount";

    public AnomalyDetectorService(RootsEnvironment environment) {
        super(environment, environment.getId() + "-anomaly-detector-scheduler",
                environment.getProperty(ANOMALY_DETECTOR_THREAD_POOL, "org.quartz.simpl.SimpleThreadPool"),
                Integer.parseInt(environment.getProperty(ANOMALY_DETECTOR_THREAD_COUNT, "10")),
                ANOMALY_DETECTOR_GROUP);
    }

    @Override
    protected void doInit() throws Exception {
        environment.subscribe(new AnomalyLogger());
        super.doInit();
    }

    @Override
    protected AnomalyDetector createItem(RootsEnvironment environment, Properties properties) {
        return AnomalyDetectorFactory.create(environment, properties);
    }

    @Override
    protected Stream<Properties> loadItems() {
        return environment.getConfigLoader().loadItems(ConfigLoader.DETECTORS, true);
    }
}
