package edu.ucsb.cs.roots.anomaly;

import edu.ucsb.cs.roots.ConfigLoader;
import edu.ucsb.cs.roots.RootsEnvironment;
import edu.ucsb.cs.roots.data.BenchmarkResult;
import edu.ucsb.cs.roots.data.DataStoreCall;
import edu.ucsb.cs.roots.data.TestDataStore;
import junit.framework.Assert;
import org.junit.Test;

import java.util.List;
import java.util.Properties;

public class SLOBasedDetectorTest {

    @Test
    public void testDetectorTimestampUpdate() throws Exception {
        RootsEnvironment environment = new RootsEnvironment("Test", new ConfigLoader(){});
        try {
            environment.init();
            TestDataStore dataStore = new TestDataStore(environment, "test-ds");
            SLOBasedDetector detector = SLOBasedDetector.newBuilder()
                    .setApplication("test-app")
                    .setPeriodInSeconds(1)
                    .setHistoryLengthInSeconds(5)
                    .setResponseTimeUpperBound(20)
                    .setDataStore("test-ds")
                    .build(environment);

            Assert.assertEquals(0, dataStore.callCount());
            detector.run(70000);
            Assert.assertEquals(1, dataStore.callCount());
            DataStoreCall call = dataStore.getCallsAndClear().get(0);
            Assert.assertEquals(TestDataStore.GET_BENCHMARK_RESULTS, call.getType());
            Assert.assertEquals("test-app", call.getApplication());
            Assert.assertEquals(10000L, call.getEnd());
            Assert.assertEquals(5000L, call.getStart());

            Assert.assertEquals(0, dataStore.callCount());
            detector.run(71000);
            Assert.assertEquals(1, dataStore.callCount());
            call = dataStore.getCallsAndClear().get(0);
            Assert.assertEquals(TestDataStore.GET_BENCHMARK_RESULTS, call.getType());
            Assert.assertEquals("test-app", call.getApplication());
            Assert.assertEquals(11000L, call.getEnd());
            Assert.assertEquals(10000L, call.getStart());
        } finally {
            environment.destroy();
        }
    }

    @Test
    public void testDetector() throws Exception {
        RootsEnvironment environment = new RootsEnvironment("Test", new ConfigLoader(){
            @Override
            public Properties loadGlobalProperties() throws Exception {
                Properties properties = new Properties();
                properties.setProperty(RootsEnvironment.EVENT_BUS_TYPE, "sync");
                return properties;
            }
        });
        try {
            environment.init();
            AnomalyRecorder recorder = new AnomalyRecorder();
            environment.subscribe(recorder);
            TestDataStore dataStore = new TestDataStore(environment, "test-ds");
            SLOBasedDetector detector = SLOBasedDetector.newBuilder()
                    .setApplication("test-app")
                    .setPeriodInSeconds(1)
                    .setHistoryLengthInSeconds(5)
                    .setMinimumSamples(5)
                    .setResponseTimeUpperBound(20)
                    .setDataStore("test-ds")
                    .build(environment);

            detector.run(70000);
            for (int i = 0; i < 5; i++) {
                dataStore.addBenchmarkResult("GET /", new BenchmarkResult((i+10) * 1000,
                        "test-app", "GET", "/", 15));
                detector.run(70000 + (i+1) * 1000);
                Assert.assertTrue(recorder.getAndClearAnomalies().isEmpty());
            }

            for (int i = 0; i < 5; i++) {
                if (i == 4) {
                    dataStore.addBenchmarkResult("GET /", new BenchmarkResult((i+15) * 1000,
                            "test-app", "GET", "/", 21));
                } else {
                    dataStore.addBenchmarkResult("GET /", new BenchmarkResult((i+15) * 1000,
                            "test-app", "GET", "/", 15));
                }
                detector.run(75000 + (i+1) * 1000);

                if (i == 4) {
                    List<Anomaly> anomalies = recorder.getAndClearAnomalies();
                    Assert.assertEquals(1, anomalies.size());
                    Anomaly anomaly = anomalies.get(0);
                    Assert.assertEquals("test-app", anomaly.getApplication());
                    Assert.assertEquals("GET /", anomaly.getOperation());
                    Assert.assertEquals(15000L, anomaly.getStart());
                    Assert.assertEquals(20000L, anomaly.getEnd());
                    Assert.assertEquals(1, anomaly.getPeriodInSeconds());
                } else {
                    Assert.assertTrue(recorder.getAndClearAnomalies().isEmpty());
                }
            }
        } finally {
            environment.destroy();
        }
    }

    @Test
    public void testMinimumSamples() throws Exception {
        RootsEnvironment environment = new RootsEnvironment("Test", new ConfigLoader(){
            @Override
            public Properties loadGlobalProperties() throws Exception {
                Properties properties = new Properties();
                properties.setProperty(RootsEnvironment.EVENT_BUS_TYPE, "sync");
                return properties;
            }
        });
        try {
            environment.init();
            AnomalyRecorder recorder = new AnomalyRecorder();
            environment.subscribe(recorder);
            TestDataStore dataStore = new TestDataStore(environment, "test-ds");
            SLOBasedDetector detector = SLOBasedDetector.newBuilder()
                    .setApplication("test-app")
                    .setPeriodInSeconds(1)
                    .setHistoryLengthInSeconds(5)
                    .setMinimumSamples(5)
                    .setResponseTimeUpperBound(20)
                    .setDataStore("test-ds")
                    .build(environment);

            detector.run(70000);
            for (int i = 0; i < 10; i++) {
                dataStore.addBenchmarkResult("GET /", new BenchmarkResult((i+10) * 1000,
                        "test-app", "GET", "/", 21));
                detector.run(70000 + (i+1) * 1000);
                if (i != 4 && i != 9) {
                    Assert.assertTrue(recorder.getAndClearAnomalies().isEmpty());
                } else {
                    Assert.assertEquals(1, recorder.getAndClearAnomalies().size());
                }
            }
        } finally {
            environment.destroy();
        }
    }

}
