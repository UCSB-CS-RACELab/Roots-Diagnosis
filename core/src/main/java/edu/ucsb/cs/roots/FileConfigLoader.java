package edu.ucsb.cs.roots;

import com.google.common.base.Strings;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.util.Objects;
import java.util.Properties;
import java.util.stream.Stream;

import static com.google.common.base.Preconditions.checkArgument;

public final class FileConfigLoader implements ConfigLoader {

    private static final Logger log = LoggerFactory.getLogger(FileConfigLoader.class);

    private final File configDir;

    public FileConfigLoader(String configDirPath) {
        checkArgument(!Strings.isNullOrEmpty(configDirPath), "Config directory path is required");
        configDir = new File(configDirPath).getAbsoluteFile();
        checkArgument(configDir.exists(), "Config directory %s does not exist",
                configDir.getAbsolutePath());
        checkArgument(configDir.isDirectory(), "Path %s does not point to a directory",
                configDir.getAbsolutePath());
    }

    @Override
    public Properties loadGlobalProperties() throws Exception {
        Properties properties = new Properties();
        File file = new File(configDir, "roots.properties");
        try (FileInputStream in = FileUtils.openInputStream(file)) {
            properties.load(in);
        }
        return properties;
    }

    @Override
    public Stream<Properties> loadItems(int type, boolean ignoreFaults) {
        if (type == DETECTORS) {
            return loadFromSubdirectory("detectors", "detector", ignoreFaults);
        } else if (type == DATA_STORES) {
            return loadFromSubdirectory("dataStores", "data store", ignoreFaults);
        } else if (type == BENCHMARKS) {
            return loadFromSubdirectory("benchmarks", "benchmark", ignoreFaults);
        }
        throw new IllegalArgumentException("Invalid item type: " + type);
    }

    private Stream<Properties> loadFromSubdirectory(String directory, String label, boolean ignoreFaults) {
        File subdirectory = new File(configDir, directory);
        if (!subdirectory.exists()) {
            return Stream.empty();
        }
        return FileUtils.listFiles(subdirectory, new String[]{"properties"}, false).stream()
                .map(f -> toItemConfig(f, label, ignoreFaults))
                .filter(Objects::nonNull);
    }

    private Properties toItemConfig(File file, String label, boolean ignoreFaults) {
        Properties properties = new Properties();
        try (FileInputStream in = FileUtils.openInputStream(file)) {
            log.info("Loading {} from: {}", label, file.getAbsolutePath());
            properties.load(in);
            return properties;
        } catch (Exception e) {
            if (ignoreFaults) {
                log.warn("Error while loading {} from: {}", label, file.getAbsolutePath());
                return null;
            } else {
                String msg = "Error while loading configuration from: " + file.getAbsolutePath();
                log.error(msg, e);
                throw new RuntimeException(msg, e);
            }
        }
    }
}
