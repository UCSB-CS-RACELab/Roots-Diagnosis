package edu.ucsb.cs.roots.data.es;

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import edu.ucsb.cs.roots.data.ElasticSearchConfig;
import edu.ucsb.cs.roots.data.ResponseTimeSummary;

import java.io.IOException;

import static com.google.common.base.Preconditions.checkArgument;

public class ResponseTimeSummaryQuery extends Query<ImmutableMap<String,ResponseTimeSummary>> {

    public static final String ACCESS_LOG_REQ_ID = "field.accessLog.requestId";
    public static final String ACCESS_LOG_TIMESTAMP = "field.accessLog.timestamp";
    public static final String ACCESS_LOG_METHOD = "field.accessLog.method";
    public static final String ACCESS_LOG_PATH = "field.accessLog.path";
    public static final String ACCESS_LOG_RESPONSE_TIME = "field.accessLog.responseTime";

    static final ImmutableList<String> METHODS = ImmutableList.of(
            "GET", "POST", "PUT", "DELETE");
    private static final String RESPONSE_TIME_SUMMARY_QUERY = loadTemplate(
            "response_time_summary_query.json");

    private final long start;
    private final long end;
    private final String application;

    private ResponseTimeSummaryQuery(Builder builder) {
        checkArgument(builder.start <= builder.end);
        checkArgument(!Strings.isNullOrEmpty(builder.application));
        this.start = builder.start;
        this.end = builder.end;
        this.application = builder.application;

    }

    @Override
    public ImmutableMap<String, ResponseTimeSummary> run(ElasticSearchConfig es) throws IOException {
        String path = String.format("/%s/%s/_search", es.getAccessLogIndex(), application);
        ImmutableMap.Builder<String,ResponseTimeSummary> builder = ImmutableMap.builder();
        JsonElement results = makeHttpCall(es, path);
        parseResponseTimeSummary(results, start, builder);
        return builder.build();
    }

    @Override
    protected String jsonString(ElasticSearchConfig es) {
        return String.format(RESPONSE_TIME_SUMMARY_QUERY,
                es.field(ACCESS_LOG_TIMESTAMP, "@timestamp"), start, end,
                es.stringField(ACCESS_LOG_METHOD, "http_verb"),
                es.stringField(ACCESS_LOG_PATH, "http_request"),
                es.field(ACCESS_LOG_RESPONSE_TIME, "time_duration"));
    }

    private void parseResponseTimeSummary(JsonElement element, long timestamp,
                                          ImmutableMap.Builder<String,ResponseTimeSummary> builder) {
        JsonArray methods = element.getAsJsonObject().getAsJsonObject("aggregations")
                .getAsJsonObject("methods").getAsJsonArray("buckets");
        for (int i = 0; i < methods.size(); i++) {
            JsonObject method = methods.get(i).getAsJsonObject();
            String methodName = method.get("key").getAsString().toUpperCase();
            if (!METHODS.contains(methodName)) {
                continue;
            }
            JsonArray paths = method.getAsJsonObject("paths").getAsJsonArray("buckets");
            for (JsonElement pathElement : paths) {
                JsonObject path = pathElement.getAsJsonObject();
                String key = methodName + " " + path.get("key").getAsString();
                builder.put(key, newResponseTimeSummary(timestamp, path));
            }
        }
    }

    static ResponseTimeSummary newResponseTimeSummary(long timestamp, JsonObject bucket) {
        double responseTime = bucket.getAsJsonObject("avg_time").get("value")
                .getAsDouble() * 1000.0;
        double requestCount = bucket.get("doc_count").getAsDouble();
        return new ResponseTimeSummary(timestamp, responseTime, requestCount);
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

        public ResponseTimeSummaryQuery build() {
            return new ResponseTimeSummaryQuery(this);
        }
    }
}
