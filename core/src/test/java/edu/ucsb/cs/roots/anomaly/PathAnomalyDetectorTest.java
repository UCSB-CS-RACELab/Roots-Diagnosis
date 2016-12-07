package edu.ucsb.cs.roots.anomaly;

import edu.ucsb.cs.roots.ConfigLoader;
import edu.ucsb.cs.roots.RootsEnvironment;
import org.junit.Test;

import java.util.Properties;
import java.util.stream.Stream;

public class PathAnomalyDetectorTest {

    public void testPathAnomalyDetector() throws Exception {
        RootsEnvironment environment = new RootsEnvironment("Test", new ConfigLoader() {
            @Override
            public Stream<Properties> loadItems(int type, boolean ignoreFaults) {
                if (type == ConfigLoader.DATA_STORES) {
                    Properties properties = new Properties();
                    properties.setProperty("name", "default");
                    properties.setProperty("type", "RandomDataStore");
                    return Stream.of(properties);
                }
                return Stream.empty();
            }
        });
        environment.init();
        try {
            PathAnomalyDetector detector = PathAnomalyDetector.newBuilder()
                    .setPeriodInSeconds(1)
                    .setHistoryLengthInSeconds(10)
                    .setApplication("app")
                    .build(environment);
            for (int i = 0; i < 20; i++) {
                detector.run(100000 + i * 100);
                Thread.sleep(100);
            }
        } finally {
            environment.destroy();
        }
    }

}
