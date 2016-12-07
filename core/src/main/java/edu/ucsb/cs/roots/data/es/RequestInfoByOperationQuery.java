package edu.ucsb.cs.roots.data.es;

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSortedSet;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import edu.ucsb.cs.roots.data.*;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;

import static com.google.common.base.Preconditions.checkArgument;

public class RequestInfoByOperationQuery extends Query<ImmutableList<ApplicationRequest>> {

    private static final String REQUEST_INFO_QUERY = loadTemplate(
            "request_info_by_operation_query.json");
    private static final String ACCESS_LOG_QUERY = loadTemplate(
            "benchmark_results_query.json");

    private static final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");

    private final long start;
    private final long end;
    private final String application;
    private final String operation;

    private RequestInfoByOperationQuery(Builder builder) {
        checkArgument(builder.start <= builder.end);
        checkArgument(!Strings.isNullOrEmpty(builder.application));
        checkArgument(!Strings.isNullOrEmpty(builder.operation));
        this.start = builder.start;
        this.end = builder.end;
        this.application = builder.application;
        this.operation = builder.operation;
    }

    @Override
    public ImmutableList<ApplicationRequest> run(ElasticSearchConfig es) throws IOException {
        String path = String.format("/%s/%s/_search?scroll=1m", es.getApiCallIndex(), application);
        ImmutableListMultimap<String,ApiCall> apiCalls = RequestInfoQuery.getRequestInfo(this, es, path);

        String json = String.format(ACCESS_LOG_QUERY,
                es.field(ResponseTimeSummaryQuery.ACCESS_LOG_TIMESTAMP, "@timestamp"),
                start, end, es.field(ResponseTimeSummaryQuery.ACCESS_LOG_TIMESTAMP, "@timestamp"));
        path = String.format("/%s/%s/_search?scroll=1m", es.getAccessLogIndex(), application);
        ImmutableMap<String,AccessLogEntry> accessLogEntries = getAccessLogEntries(
                es, application, path, json);

        ImmutableSortedSet.Builder<ApplicationRequest> builder = ImmutableSortedSet.orderedBy(
                ApplicationRequest.TIME_ORDER);
        apiCalls.keySet().stream().filter(accessLogEntries::containsKey).forEach(requestId -> {
            ImmutableList<ApiCall> calls = apiCalls.get(requestId);
            ApplicationRequest req = new ApplicationRequest(requestId, calls.get(0).getRequestTimestamp(),
                    application, operation, calls, accessLogEntries.get(requestId).getResponseTime());
            builder.add(req);
        });
        return ImmutableList.copyOf(builder.build());
    }

    @Override
    public String jsonString(ElasticSearchConfig es) {
        return String.format(REQUEST_INFO_QUERY,
                es.stringField(RequestInfoQuery.API_CALL_REQ_OPERATION, "requestOperation"),
                operation,
                es.field(RequestInfoQuery.API_CALL_REQ_TIMESTAMP, "requestTimestamp"),
                start, end,
                es.field(RequestInfoQuery.API_CALL_REQ_TIMESTAMP, "requestTimestamp"),
                es.field(RequestInfoQuery.API_CALL_SEQ_NUMBER, "sequenceNumber"));
    }

    private ImmutableMap<String,AccessLogEntry> getAccessLogEntries(ElasticSearchConfig es,
            String application, String path, String query) throws IOException {

        ImmutableMap.Builder<String,AccessLogEntry> builder = ImmutableMap.builder();
        JsonElement results = makeHttpCall(es, path, query);
        String scrollId = results.getAsJsonObject().get("_scroll_id").getAsString();
        long total = results.getAsJsonObject().getAsJsonObject("hits").get("total").getAsLong();
        long received = 0L;
        while (true) {
            received += parseAccessLogEntries(es, application, results, builder);
            if (received >= total) {
                break;
            }
            results = nextBatch(es, scrollId);
        }
        return builder.build();
    }

    private int parseAccessLogEntries(ElasticSearchConfig es, String application, JsonElement element,
                                      ImmutableMap.Builder<String,AccessLogEntry> builder) {
        JsonArray hits = element.getAsJsonObject().getAsJsonObject("hits")
                .getAsJsonArray("hits");
        for (JsonElement hitElement : hits) {
            JsonObject hit = hitElement.getAsJsonObject().getAsJsonObject("_source");
            JsonElement requestIdElem = hit.get(es.field(ResponseTimeSummaryQuery.ACCESS_LOG_REQ_ID,
                    "request_id"));
            if (requestIdElem == null) {
                continue;
            }
            String requestId = requestIdElem.getAsString();
            String timeString = hit.get(es.field(ResponseTimeSummaryQuery.ACCESS_LOG_TIMESTAMP,
                    "@timestamp")).getAsString();
            String method = hit.get(es.field(ResponseTimeSummaryQuery.ACCESS_LOG_METHOD,
                    "http_verb")).getAsString();
            String path = hit.get(es.field(ResponseTimeSummaryQuery.ACCESS_LOG_PATH,
                    "http_request")).getAsString();
            int timeDuration = (int) (hit.get(es.field(ResponseTimeSummaryQuery.ACCESS_LOG_RESPONSE_TIME,
                    "time_duration")).getAsDouble() * 1000);
            try {
                AccessLogEntry entry = new AccessLogEntry(requestId,
                        dateFormat.parse(timeString).getTime(), application,
                        method, path, timeDuration);
                builder.put(entry.getRequestId(), entry);
            } catch (ParseException e) {
                throw new RuntimeException(e);
            }
        }
        return hits.size();
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    public static class Builder {

        private long start;
        private long end;
        private String application;
        private String operation;

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

        public Builder setOperation(String operation) {
            this.operation = operation;
            return this;
        }

        public RequestInfoByOperationQuery build() {
            return new RequestInfoByOperationQuery(this);
        }
    }
}
