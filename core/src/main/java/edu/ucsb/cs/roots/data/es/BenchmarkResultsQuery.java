package edu.ucsb.cs.roots.data.es;

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableListMultimap;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import edu.ucsb.cs.roots.data.BenchmarkResult;
import edu.ucsb.cs.roots.data.ElasticSearchConfig;

import java.io.IOException;

import static com.google.common.base.Preconditions.checkArgument;

public final class BenchmarkResultsQuery extends Query<ImmutableListMultimap<String, BenchmarkResult>> {

    public static final String BENCHMARK_TIMESTAMP = "field.benchmark.timestamp";
    public static final String BENCHMARK_METHOD = "field.benchmark.method";
    public static final String BENCHMARK_PATH = "field.benchmark.path";
    public static final String BENCHMARK_RESPONSE_TIME = "field.benchmark.responseTime";

    private static final String BENCHMARK_RESULTS_QUERY = loadTemplate(
            "benchmark_results_query.json");

    private final long start;
    private final long end;
    private final String application;

    private BenchmarkResultsQuery(Builder builder) {
        checkArgument(builder.start <= builder.end);
        checkArgument(!Strings.isNullOrEmpty(builder.application));
        this.start = builder.start;
        this.end = builder.end;
        this.application = builder.application;
    }

    @Override
    public ImmutableListMultimap<String, BenchmarkResult> run(ElasticSearchConfig es) throws IOException {
        String path = String.format("/%s/%s/_search?scroll=1m", es.getBenchmarkIndex(), application);
        ImmutableListMultimap.Builder<String,BenchmarkResult> builder = ImmutableListMultimap.builder();
        JsonElement results = makeHttpCall(es, path);
        String scrollId = results.getAsJsonObject().get("_scroll_id").getAsString();
        long total = results.getAsJsonObject().getAsJsonObject("hits").get("total").getAsLong();
        long received = 0L;
        while (true) {
            received += parseBenchmarkResults(es, results, builder);
            if (received >= total) {
                break;
            }
            results = nextBatch(es, scrollId);
        }
        return builder.build();
    }

    private int parseBenchmarkResults(ElasticSearchConfig es, JsonElement element,
                                      ImmutableListMultimap.Builder<String,BenchmarkResult> builder) {
        JsonArray hits = element.getAsJsonObject().getAsJsonObject("hits")
                .getAsJsonArray("hits");
        for (JsonElement hitElement : hits) {
            JsonObject hit = hitElement.getAsJsonObject().getAsJsonObject("_source");
            BenchmarkResult result = new BenchmarkResult(
                    hit.get(es.field(BENCHMARK_TIMESTAMP, "timestamp")).getAsLong(),
                    application,
                    hit.get(es.field(BENCHMARK_METHOD, "method")).getAsString(),
                    hit.get(es.field(BENCHMARK_PATH, "path")).getAsString(),
                    hit.get(es.field(BENCHMARK_RESPONSE_TIME, "responseTime")).getAsInt());
            builder.put(result.getRequestType(), result);
        }
        return hits.size();
    }

    @Override
    protected String jsonString(ElasticSearchConfig es) {
        return String.format(BENCHMARK_RESULTS_QUERY, es.field(BENCHMARK_TIMESTAMP, "timestamp"),
                start, end, es.field(BENCHMARK_TIMESTAMP, "timestamp"));
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    public static class Builder {

        private long start;
        private long end;
        private String application;

        private Builder() {
        }

        public Builder setStart(long start) {
            this.start = start;
            return this;
        }

        public Builder setEnd(long end) {
            this.end = end;
            return this;
        }

        public Builder setApplication(String application) {
            this.application = application;
            return this;
        }

        public BenchmarkResultsQuery build() {
            return new BenchmarkResultsQuery(this);
        }
    }
}
