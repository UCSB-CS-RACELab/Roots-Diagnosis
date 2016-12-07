package edu.ucsb.cs.roots.data;

import com.google.common.base.Strings;

import static com.google.common.base.Preconditions.checkArgument;

public final class AccessLogEntry extends BenchmarkResult {

    private final String requestId;

    public AccessLogEntry(String requestId, long timestamp, String application, String method,
                          String path, int responseTime) {
        super(timestamp, application, method, path, responseTime);
        checkArgument(!Strings.isNullOrEmpty(requestId), "Request ID is required");
        this.requestId = requestId;
    }

    public String getRequestId() {
        return requestId;
    }
}
