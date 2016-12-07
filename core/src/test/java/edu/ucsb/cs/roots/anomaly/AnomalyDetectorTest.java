package edu.ucsb.cs.roots.anomaly;

import edu.ucsb.cs.roots.ConfigLoader;
import edu.ucsb.cs.roots.RootsEnvironment;
import edu.ucsb.cs.roots.data.TestDataStore;
import org.junit.Assert;
import org.junit.Test;

import java.util.List;
import java.util.Properties;

public class AnomalyDetectorTest {

    @Test
    public void testLastAnomalyTime() throws Exception {
        RootsEnvironment environment = getEnvironment();
        try {
            environment.init();
            AnomalyRecorder recorder = new AnomalyRecorder();
            environment.subscribe(recorder);
            TestDataStore dataStore = new TestDataStore(environment, "default");
            TestAnomalyDetector detector = TestAnomalyDetector.newBuilder()
                    .setApplication("test-app")
                    .setPeriodInSeconds(1)
                    .setHistoryLengthInSeconds(5)
                    .setDataStore(dataStore.getName())
                    .setFunction((now,d) ->
                            d.reportAnomaly(d.newAnomaly(1, now, "foo")
                                    .setType(Anomaly.TYPE_PERFORMANCE)
                                    .setDescription("test")
                                    .build()))
                    .setWaitDuration(10 * 1000L)
                    .build(environment);

            Assert.assertEquals(-1L, detector.getLastAnomalyTime("foo"));
            detector.run(70000);
            Assert.assertEquals(70000, detector.getLastAnomalyTime("foo"));
            List<Anomaly> anomalies = recorder.getAndClearAnomalies();
            Assert.assertEquals(1, anomalies.size());
            Assert.assertEquals(-1L, anomalies.get(0).getPreviousAnomalyTime());

            detector.run(75000);
            Assert.assertEquals(75000, detector.getLastAnomalyTime("foo"));
            anomalies = recorder.getAndClearAnomalies();
            Assert.assertEquals(1, anomalies.size());
            Assert.assertEquals(70000L, anomalies.get(0).getPreviousAnomalyTime());

            detector.run(80000);
            Assert.assertEquals(80000, detector.getLastAnomalyTime("foo"));
            anomalies = recorder.getAndClearAnomalies();
            Assert.assertEquals(1, anomalies.size());
            Assert.assertEquals(75000L, anomalies.get(0).getPreviousAnomalyTime());
        } finally {
            environment.destroy();
        }
    }

    @Test
    public void testNewAnomaly() throws Exception {
        RootsEnvironment environment = getEnvironment();
        try {
            environment.init();
            AnomalyRecorder recorder = new AnomalyRecorder();
            environment.subscribe(recorder);
            TestDataStore dataStore = new TestDataStore(environment, "default");
            TestAnomalyDetector detector = TestAnomalyDetector.newBuilder()
                    .setApplication("test-app")
                    .setPeriodInSeconds(1)
                    .setHistoryLengthInSeconds(5)
                    .setDataStore(dataStore.getName())
                    .setFunction((now,d) ->
                            d.reportAnomaly(d.newAnomaly(1, now, "foo")
                                    .setType(Anomaly.TYPE_PERFORMANCE)
                                    .setDescription("test")
                                    .build()))
                    .setWaitDuration(10 * 1000L)
                    .build(environment);

            detector.run(70000);
            List<Anomaly> anomalies = recorder.getAndClearAnomalies();
            Assert.assertEquals(1, anomalies.size());
            Anomaly anomaly = anomalies.get(0);
            Assert.assertEquals(1, anomaly.getStart());
            Assert.assertEquals(70000, anomaly.getEnd());
            Assert.assertEquals("foo", anomaly.getOperation());
            Assert.assertEquals(-1L, anomaly.getPreviousAnomalyTime());

            detector.run(75000);
            Assert.assertEquals(1, recorder.getAndClearAnomalies().size());

            detector.run(80000);
            anomalies = recorder.getAndClearAnomalies();
            Assert.assertEquals(1, anomalies.size());
            anomaly = anomalies.get(0);
            Assert.assertEquals(1, anomaly.getStart());
            Assert.assertEquals(80000, anomaly.getEnd());
            Assert.assertEquals("foo", anomaly.getOperation());
            Assert.assertEquals(75000, anomaly.getPreviousAnomalyTime());
        } finally {
            environment.destroy();
        }
    }

    private RootsEnvironment getEnvironment() throws Exception {
        return new RootsEnvironment("Test", new ConfigLoader(){
            @Override
            public Properties loadGlobalProperties() throws Exception {
                Properties properties = new Properties();
                properties.setProperty(RootsEnvironment.EVENT_BUS_TYPE, "sync");
                return properties;
            }
        });
    }

}
