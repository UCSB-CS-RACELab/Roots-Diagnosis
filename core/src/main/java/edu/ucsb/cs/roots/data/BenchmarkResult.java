package edu.ucsb.cs.roots.data;

import com.google.common.base.Strings;

import static com.google.common.base.Preconditions.checkArgument;

/**
 * Represents a benchmark measurement taken at a specific point in time. A benchmark measurement
 * is related to a specific application, and an operation (identified by method and path).
 */
public class BenchmarkResult {

    private final long timestamp;
    private final String application;
    private final String method;
    private final String path;
    private final int responseTime;

    public BenchmarkResult(long timestamp, String application, String method,
                           String path, int responseTime) {
        checkArgument(!Strings.isNullOrEmpty(application), "Application is required");
        checkArgument(!Strings.isNullOrEmpty(method), "Method is required");
        checkArgument(!Strings.isNullOrEmpty(path), "Path is required");
        checkArgument(responseTime >= 0, "Response time must be non-negative");
        this.timestamp = timestamp;
        this.application = application;
        this.method = method;
        this.path = path;
        this.responseTime = responseTime;
    }

    public final String getRequestType() {
        return method + " " + path;
    }

    public final long getTimestamp() {
        return timestamp;
    }

    public final String getApplication() {
        return application;
    }

    public final String getMethod() {
        return method;
    }

    public final String getPath() {
        return path;
    }

    public final int getResponseTime() {
        return responseTime;
    }
}
