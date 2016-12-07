package edu.ucsb.cs.roots.data.es;

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import edu.ucsb.cs.roots.data.ElasticSearchConfig;

import java.io.IOException;

import static com.google.common.base.Preconditions.checkArgument;

public final class WorkloadSummaryQuery extends Query<ImmutableList<Double>> {

    private static final String WORKLOAD_SUMMARY_QUERY = loadTemplate(
            "workload_summary_query.json");

    private final long start;
    private final long end;
    private final long period;
    private final String method;
    private final String path;
    private final String application;

    private WorkloadSummaryQuery(Builder builder) {
        checkArgument(builder.start <= builder.end);
        checkArgument(builder.period > 0);
        checkArgument(!Strings.isNullOrEmpty(builder.method));
        checkArgument(!Strings.isNullOrEmpty(builder.path));
        checkArgument(!Strings.isNullOrEmpty(builder.application));
        this.start = builder.start;
        this.end = builder.end;
        this.period = builder.period;
        this.method = builder.method;
        this.path = builder.path;
        this.application = builder.application;
    }

    @Override
    public ImmutableList<Double> run(ElasticSearchConfig es) throws IOException {
        String path = String.format("/%s/%s/_search", es.getAccessLogIndex(), application);
        ImmutableList.Builder<Double> builder = ImmutableList.builder();
        JsonElement results = makeHttpCall(es, path);
        JsonArray periods = results.getAsJsonObject().getAsJsonObject("aggregations")
                .getAsJsonObject("periods").getAsJsonArray("buckets");
        periods.forEach(p -> builder.add(
                p.getAsJsonObject().get("doc_count").getAsDouble()));
        return builder.build();
    }

    @Override
    protected String jsonString(ElasticSearchConfig es) {
        return String.format(WORKLOAD_SUMMARY_QUERY,
                es.stringField(ResponseTimeSummaryQuery.ACCESS_LOG_METHOD, "http_verb"), method,
                es.stringField(ResponseTimeSummaryQuery.ACCESS_LOG_PATH, "http_request"), path,
                es.field(ResponseTimeSummaryQuery.ACCESS_LOG_TIMESTAMP, "@timestamp"),
                start, end,
                es.field(ResponseTimeSummaryQuery.ACCESS_LOG_TIMESTAMP, "@timestamp"),
                period, start % period, start, end - period);
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    public static class Builder {
        private String method;
        private String path;
        private long start;
        private long end;
        private long period;
        private String application;

        private Builder() {
        }

        public Builder setMethod(String method) {
            this.method = method;
            return this;
        }

        public Builder setPath(String path) {
            this.path = path;
            return this;
        }

        public Builder setStart(long start) {
            this.start = start;
            return this;
        }

        public Builder setEnd(long end) {
            this.end = end;
            return this;
        }

        public Builder setPeriod(long period) {
            this.period = period;
            return this;
        }

        public Builder setApplication(String application) {
            this.application = application;
            return this;
        }

        public WorkloadSummaryQuery build() {
            return new WorkloadSummaryQuery(this);
        }
    }

}
