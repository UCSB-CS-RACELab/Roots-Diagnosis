package edu.ucsb.cs.roots.data;

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;

import java.util.Comparator;
import java.util.Optional;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

public final class ApplicationRequest {

    public static final Comparator<ApplicationRequest> TIME_ORDER = (o1, o2) ->
            Long.compare(o1.getTimestamp(), o2.getTimestamp());

    private final String requestId;
    private final long timestamp;
    private final String application;
    private final String operation;
    private final ImmutableList<ApiCall> apiCalls;
    private final int responseTime;

    public ApplicationRequest(String requestId, long timestamp, String application,
                              String operation, ImmutableList<ApiCall> apiCalls) {
        this(requestId, timestamp, application, operation, apiCalls,
                apiCalls.stream().mapToInt(ApiCall::getTimeElapsed).sum());
    }

    public ApplicationRequest(String requestId, long timestamp, String application,
                              String operation, ImmutableList<ApiCall> apiCalls, int responseTime) {
        checkArgument(!Strings.isNullOrEmpty(requestId), "RequestID is required");
        checkArgument(timestamp > 0, "Timestamp must be positive");
        checkArgument(!Strings.isNullOrEmpty(application), "Application is required");
        checkArgument(!Strings.isNullOrEmpty(operation), "Operation is required");
        checkNotNull(apiCalls, "ApiCall list must not be null");
        checkArgument(responseTime >= 0, "Response time must be non-negative");
        this.requestId = requestId;
        this.timestamp = timestamp;
        this.application = application;
        this.operation = operation;
        this.apiCalls = apiCalls;
        this.responseTime = responseTime;
    }

    public String getRequestId() {
        return requestId;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public String getApplication() {
        return application;
    }

    public String getOperation() {
        return operation;
    }

    public ImmutableList<ApiCall> getApiCalls() {
        return apiCalls;
    }

    public int getResponseTime() {
        return responseTime;
    }

    public String getPathAsString() {
        Optional<String> path = apiCalls.stream().map(ApiCall::name).reduce((a, b) -> a + ", " + b);
        if (path.isPresent()) {
            return path.get();
        } else {
            return "";
        }
    }
}
