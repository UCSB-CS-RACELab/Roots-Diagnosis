package edu.ucsb.cs.roots.bm;

import com.google.common.base.Strings;
import edu.ucsb.cs.roots.ConfigLoader;
import edu.ucsb.cs.roots.RootsEnvironment;
import edu.ucsb.cs.roots.scheduling.RootsJob;
import edu.ucsb.cs.roots.scheduling.SchedulerService;
import org.apache.commons.io.IOUtils;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.quartz.Job;

import java.util.Properties;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.google.common.base.Preconditions.checkArgument;

public final class BenchmarkingService extends SchedulerService<Benchmark> {

    private static final String APPLICATION = "application";
    private static final String BENCHMARK_PERIOD = "period";
    private static final String BENCHMARK_PERIOD_TIME_UNIT = "timeUnit";
    private static final String BENCHMARK_DATA_STORE = "dataStore";
    private static final String BENCHMARK_CALL = "call.";

    private static final String BENCHMARK_GROUP = "benchmark";
    private static final String BENCHMARK_THREAD_POOL = "benchmark.threadPool";
    private static final String BENCHMARK_THREAD_COUNT = "benchmark.threadCount";
    private static final String BENCHMARK_URL_PREFIX = "benchmark.urlPrefix";

    private final CloseableHttpClient client;

    public BenchmarkingService(RootsEnvironment environment) {
        super(environment, environment.getId() + "-benchmark-scheduler",
                environment.getProperty(BENCHMARK_THREAD_POOL, "org.quartz.simpl.SimpleThreadPool"),
                Integer.parseInt(environment.getProperty(BENCHMARK_THREAD_COUNT, "10")),
                BENCHMARK_GROUP);
        this.client = HttpClients.createDefault();
    }

    @Override
    protected void doDestroy() {
        super.doDestroy();
        IOUtils.closeQuietly(client);
    }

    @Override
    protected Stream<Properties> loadItems() {
        return environment.getConfigLoader().loadItems(ConfigLoader.BENCHMARKS, true);
    }

    @Override
    protected Benchmark createItem(RootsEnvironment environment, Properties properties) {
        Benchmark.Builder builder = Benchmark.newBuilder();
        builder.setApplication(properties.getProperty(APPLICATION));
        builder.setClient(client);

        String period = properties.getProperty(BENCHMARK_PERIOD);
        if (!Strings.isNullOrEmpty(period)) {
            TimeUnit timeUnit = TimeUnit.valueOf(properties.getProperty(
                    BENCHMARK_PERIOD_TIME_UNIT, "SECONDS"));
            builder.setPeriodInSeconds((int) timeUnit.toSeconds(Integer.parseInt(period)));
        }

        String dataStore = properties.getProperty(BENCHMARK_DATA_STORE, "default");
        if (!Strings.isNullOrEmpty(dataStore)) {
            builder.setDataStore(dataStore);
        }

        Set<String> calls = properties.stringPropertyNames().stream()
                .filter(k -> k.startsWith(BENCHMARK_CALL))
                .map(k -> k.substring(0, k.indexOf('.', BENCHMARK_CALL.length())))
                .collect(Collectors.toSet());
        calls.forEach(call -> {
            String method = properties.getProperty(call + ".method");
            String url = properties.getProperty(call + ".url");
            if (!url.startsWith("http://") && !url.startsWith("https://")) {
                String prefix = environment.getProperty(BENCHMARK_URL_PREFIX, null);
                checkArgument(!Strings.isNullOrEmpty(prefix), "Benchmark URL prefix not configured");
                url = prefix + url;
            }
            int timeout = Integer.parseInt(properties.getProperty(call + ".timeout", "60"));
            String payload = properties.getProperty(call + ".payload");
            if (payload != null) {
                String contentType = properties.getProperty(call + ".contentType");
                RequestContent requestContent = new RequestContent(payload, contentType);
                builder.addCall(new BenchmarkCall(method, url, timeout, requestContent));
            } else {
                builder.addCall(new BenchmarkCall(method, url, timeout));
            }
        });
        return builder.build(environment);
    }

    @Override
    protected Class<? extends Job> jobClass() {
        // A Benchmark may have a timeout longer than its period. If that is the case, and
        // if the Benchmark does get blocked for a long time, we need to allow subsequent
        // triggers to run concurrently.
        return RootsJob.class;
    }
}
