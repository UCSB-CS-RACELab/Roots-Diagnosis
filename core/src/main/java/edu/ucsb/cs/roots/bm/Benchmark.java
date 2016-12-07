package edu.ucsb.cs.roots.bm;

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import edu.ucsb.cs.roots.RootsEnvironment;
import edu.ucsb.cs.roots.data.BenchmarkResult;
import edu.ucsb.cs.roots.data.DataStore;
import edu.ucsb.cs.roots.data.DataStoreException;
import edu.ucsb.cs.roots.scheduling.ScheduledItem;
import org.apache.http.impl.client.CloseableHttpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

public final class Benchmark extends ScheduledItem {

    private static final Logger log = LoggerFactory.getLogger(Benchmark.class);

    private final RootsEnvironment environment;
    private final String dataStore;
    private final CloseableHttpClient client;
    private final List<BenchmarkCall> calls;

    private Benchmark(RootsEnvironment environment, Builder builder) {
        super(builder.application, builder.periodInSeconds);
        checkNotNull(environment, "Environment is required");
        checkNotNull(builder.client, "HTTP client is required");
        checkArgument(!Strings.isNullOrEmpty(builder.dataStore), "DataStore is required");
        checkNotNull(builder.calls, "Calls list is required");
        this.environment = environment;
        this.client = builder.client;
        this.dataStore = builder.dataStore;
        this.calls = ImmutableList.copyOf(builder.calls);
    }

    public void run(long now) {
        DataStore ds = environment.getDataStoreService().get(dataStore);
        calls.forEach(c -> {
            try {
                long time = c.execute(client);
                if (log.isDebugEnabled()) {
                    log.debug("Benchmark result for {} [{} {}]: {} ms", application, c.getMethod(),
                            c.getPath(), time);
                }
                BenchmarkResult result = new BenchmarkResult(now, application, c.getMethod(),
                        c.getPath(), (int) time);
                ds.recordBenchmarkResult(result);
            } catch (IOException e) {
                log.error("Error while calling {} [{} {}]", application, c.getMethod(),
                        c.getPath(), e);
            } catch (DataStoreException e) {
                log.error("Error while recording benchmark result for {} [{} {}]", application,
                        c.getMethod(), c.getPath(), e);
            }
        });
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    public static class Builder {
        private String application;
        private int periodInSeconds = 60;
        private String dataStore = "default";
        private CloseableHttpClient client;
        private final List<BenchmarkCall> calls = new ArrayList<>();

        private Builder() {
        }

        public Builder setApplication(String application) {
            this.application = application;
            return this;
        }

        public Builder setPeriodInSeconds(int periodInSeconds) {
            this.periodInSeconds = periodInSeconds;
            return this;
        }

        public Builder setDataStore(String dataStore) {
            this.dataStore = dataStore;
            return this;
        }

        public Builder setClient(CloseableHttpClient client) {
            this.client = client;
            return this;
        }

        public Builder addCall(BenchmarkCall call) {
            this.calls.add(call);
            return this;
        }

        public Benchmark build(RootsEnvironment environment) {
            return new Benchmark(environment, this);
        }
    }
}
