package edu.ucsb.cs.roots.data.es;

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableListMultimap;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import edu.ucsb.cs.roots.data.ElasticSearchConfig;
import edu.ucsb.cs.roots.data.ResponseTimeSummary;

import java.io.IOException;

import static com.google.common.base.Preconditions.checkArgument;

public class ResponseTimeHistoryQuery extends Query<ImmutableListMultimap<String,ResponseTimeSummary>> {


    private static final String RESPONSE_TIME_HISTORY_QUERY = loadTemplate(
            "response_time_history_query.json");

    private final long start;
    private final long end;
    private final long period;

    private final String application;

    private ResponseTimeHistoryQuery(Builder builder) {
        checkArgument(builder.start <= builder.end);
        checkArgument(builder.period > 0);
        checkArgument(!Strings.isNullOrEmpty(builder.application));
        this.start = builder.start;
        this.end = builder.end;
        this.period = builder.period;
        this.application = builder.application;
    }

    @Override
    public ImmutableListMultimap<String, ResponseTimeSummary> run(ElasticSearchConfig es) throws IOException {
        String path = String.format("/%s/%s/_search", es.getAccessLogIndex(), application);
        ImmutableListMultimap.Builder<String,ResponseTimeSummary> builder = ImmutableListMultimap.builder();
        JsonElement results = makeHttpCall(es, path);
        parseResponseTimeHistory(results, builder);
        return builder.build();
    }

    @Override
    protected String jsonString(ElasticSearchConfig es) {
        return String.format(RESPONSE_TIME_HISTORY_QUERY,
                es.field(ResponseTimeSummaryQuery.ACCESS_LOG_TIMESTAMP, "@timestamp"),
                start, end,
                es.stringField(ResponseTimeSummaryQuery.ACCESS_LOG_METHOD, "http_verb"),
                es.stringField(ResponseTimeSummaryQuery.ACCESS_LOG_PATH, "http_request"),
                es.field(ResponseTimeSummaryQuery.ACCESS_LOG_TIMESTAMP, "@timestamp"),
                period, start % period, start, end - period,
                es.field(ResponseTimeSummaryQuery.ACCESS_LOG_RESPONSE_TIME, "time_duration"));
    }

    private void parseResponseTimeHistory(JsonElement element,
                                          ImmutableListMultimap.Builder<String,ResponseTimeSummary> builder) {
        JsonArray methods = element.getAsJsonObject().getAsJsonObject("aggregations")
                .getAsJsonObject("methods").getAsJsonArray("buckets");
        for (JsonElement methodElement : methods) {
            JsonObject method = methodElement.getAsJsonObject();
            String methodName = method.get("key").getAsString().toUpperCase();
            if (!ResponseTimeSummaryQuery.METHODS.contains(methodName)) {
                continue;
            }
            JsonArray paths = method.getAsJsonObject("paths").getAsJsonArray("buckets");
            for (JsonElement pathElement : paths) {
                JsonObject path = pathElement.getAsJsonObject();
                String key = methodName + " " + path.get("key").getAsString();
                JsonArray periods = path.getAsJsonObject("periods").getAsJsonArray("buckets");
                for (JsonElement periodElement : periods) {
                    JsonObject period = periodElement.getAsJsonObject();
                    double count = period.get("doc_count").getAsDouble();
                    if (count > 0) {
                        long timestamp = period.get("key").getAsLong();
                        builder.put(key, ResponseTimeSummaryQuery.newResponseTimeSummary(timestamp, period));
                    }
                }
            }
        }
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    public static class Builder {
        private long start;
        private long end;
        private long period;
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

        public Builder setPeriod(long period) {
            this.period = period;
            return this;
        }

        public Builder setApplication(String application) {
            this.application = application;
            return this;
        }

        public ResponseTimeHistoryQuery build() {
            return new ResponseTimeHistoryQuery(this);
        }
    }
}
