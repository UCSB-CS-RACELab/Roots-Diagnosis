package edu.ucsb.cs.roots;

import java.util.Properties;
import java.util.stream.Stream;

public interface ConfigLoader {

    int DETECTORS = 100;
    int DATA_STORES = 101;
    int BENCHMARKS = 102;

    default Properties loadGlobalProperties() throws Exception {
        return new Properties();
    }

    default Stream<Properties> loadItems(int type, boolean ignoreFaults) {
        return Stream.empty();
    }

}
