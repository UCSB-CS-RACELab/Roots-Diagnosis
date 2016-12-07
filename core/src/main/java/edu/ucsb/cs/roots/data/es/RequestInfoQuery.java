package edu.ucsb.cs.roots.data.es;

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.ImmutableSetMultimap;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import edu.ucsb.cs.roots.data.ApiCall;
import edu.ucsb.cs.roots.data.ApplicationRequest;
import edu.ucsb.cs.roots.data.ElasticSearchConfig;

import java.io.IOException;

import static com.google.common.base.Preconditions.checkArgument;

public class RequestInfoQuery extends Query<ImmutableListMultimap<String, ApplicationRequest>> {

    public static final String API_CALL_REQ_TIMESTAMP = "field.apiCall.requestTimestamp";
    public static final String API_CALL_REQ_OPERATION = "field.apiCall.requestOperation";
    public static final String API_CALL_TIMESTAMP = "field.apiCall.timestamp";
    public static final String API_CALL_SEQ_NUMBER = "field.apiCall.sequenceNumber";
    public static final String API_CALL_APPLICATION = "field.apiCall.application";
    public static final String API_CALL_SERVICE = "field.apiCall.service";
    public static final String API_CALL_OPERATION = "field.apiCall.operation";
    public static final String API_CALL_RESPONSE_TIME = "field.apiCall.responseTime";
    public static final String API_CALL_REQ_ID = "field.apiCall.requestId";

    private static final String REQUEST_INFO_QUERY = loadTemplate(
            "request_info_query.json");

    private final long start;
    private final long end;
    private final String application;

    private RequestInfoQuery(Builder builder) {
        checkArgument(builder.start <= builder.end);
        checkArgument(!Strings.isNullOrEmpty(builder.application));
        this.start = builder.start;
        this.end = builder.end;
        this.application = builder.application;
    }

    @Override
    public ImmutableListMultimap<String, ApplicationRequest> run(ElasticSearchConfig es) throws IOException {
        String path = String.format("/%s/%s/_search?scroll=1m", es.getApiCallIndex(), application);
        ImmutableListMultimap<String,ApiCall> apiCalls = getRequestInfo(this, es, path);

        ImmutableSetMultimap.Builder<String,ApplicationRequest> builder = ImmutableSetMultimap
                .<String,ApplicationRequest>builder().orderValuesBy(ApplicationRequest.TIME_ORDER);
        apiCalls.keySet().forEach(requestId -> {
            ImmutableList<ApiCall> calls = apiCalls.get(requestId);
            ApiCall firstCall = calls.get(0);
            ApplicationRequest req = new ApplicationRequest(requestId, firstCall.getRequestTimestamp(),
                    application, firstCall.getRequestOperation(), calls);
            builder.put(firstCall.getRequestOperation(), req);
        });
        return ImmutableListMultimap.copyOf(builder.build());
    }

    static ImmutableListMultimap<String,ApiCall> getRequestInfo(
            Query q, ElasticSearchConfig es, String path) throws IOException {
        ImmutableListMultimap.Builder<String,ApiCall> builder = ImmutableListMultimap.builder();
        JsonElement results = q.makeHttpCall(es, path);
        String scrollId = results.getAsJsonObject().get("_scroll_id").getAsString();
        long total = results.getAsJsonObject().getAsJsonObject("hits").get("total").getAsLong();
        long received = 0L;
        while (true) {
            received += parseApiCalls(es, results, builder);
            if (received >= total) {
                break;
            }
            results = q.nextBatch(es, scrollId);
        }
        return builder.build();
    }

    private static int parseApiCalls(ElasticSearchConfig es, JsonElement element,
                              ImmutableListMultimap.Builder<String,ApiCall> builder) {
        JsonArray hits = element.getAsJsonObject().getAsJsonObject("hits")
                .getAsJsonArray("hits");
        for (JsonElement hitElement : hits) {
            JsonObject hit = hitElement.getAsJsonObject().getAsJsonObject("_source");
            ApiCall call = ApiCall.newBuilder()
                    .setRequestTimestamp(
                            hit.get(es.field(API_CALL_REQ_TIMESTAMP, "requestTimestamp")).getAsLong())
                    .setRequestOperation(
                            hit.get(es.field(API_CALL_REQ_OPERATION, "requestOperation")).getAsString())
                    .setTimestamp(
                            hit.get(es.field(API_CALL_TIMESTAMP, "timestamp")).getAsLong())
                    .setService(
                            hit.get(es.field(API_CALL_SERVICE, "service")).getAsString())
                    .setOperation(
                            hit.get(es.field(API_CALL_OPERATION, "operation")).getAsString())
                    .setTimeElapsed(
                            hit.get(es.field(API_CALL_RESPONSE_TIME, "elapsed")).getAsInt())
                    .build();
            builder.put(hit.get(es.field(API_CALL_REQ_ID, "requestId")).getAsString(), call);
        }
        return hits.size();
    }

    @Override
    public String jsonString(ElasticSearchConfig es) {
        return String.format(REQUEST_INFO_QUERY,
                es.field(API_CALL_REQ_TIMESTAMP, "requestTimestamp"), start, end,
                es.field(API_CALL_REQ_TIMESTAMP, "requestTimestamp"),
                es.field(API_CALL_SEQ_NUMBER, "sequenceNumber"));
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

        public RequestInfoQuery build() {
            return new RequestInfoQuery(this);
        }
    }
}
