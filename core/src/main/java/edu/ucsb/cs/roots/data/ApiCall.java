package edu.ucsb.cs.roots.data;

import com.google.common.base.Strings;

import static com.google.common.base.Preconditions.checkArgument;

/**
 * A cloud SDK call (a PaaS kernel invocation) made by an HTTP request. Includes data
 * about the SDK service and the operation invoked, along with timestamp and execution
 * time.
 */
public final class ApiCall {

    private final long requestTimestamp;
    private final long timestamp;
    private final String service;
    private final String operation;
    private final String requestOperation;
    private final int timeElapsed;

    private ApiCall(Builder builder) {
        checkArgument(builder.requestTimestamp > 0, "Request timestamp must be positive");
        checkArgument(builder.timestamp > 0, "Timestamp must be positive");
        checkArgument(!Strings.isNullOrEmpty(builder.service), "Service is required");
        checkArgument(!Strings.isNullOrEmpty(builder.operation), "Operation is required");
        checkArgument(!Strings.isNullOrEmpty(builder.requestOperation), "Request operation is required");
        checkArgument(builder.timeElapsed >= 0, "Time elapsed must be non-negative");
        this.requestTimestamp = builder.requestTimestamp;
        this.timestamp = builder.timestamp;
        this.service = builder.service;
        this.operation = builder.operation;
        this.requestOperation = builder.requestOperation;
        this.timeElapsed = builder.timeElapsed;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public long getRequestTimestamp() {
        return requestTimestamp;
    }

    public String getRequestOperation() {
        return requestOperation;
    }

    public String name() {
        return service + ":" + operation;
    }

    public int getTimeElapsed() {
        return timeElapsed;
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    public static final class Builder {

        private long requestTimestamp;
        private long timestamp;
        private String service;
        private String operation;
        private String requestOperation;
        private int timeElapsed;

        private Builder() {
        }

        public Builder setRequestTimestamp(long requestTimestamp) {
            this.requestTimestamp = requestTimestamp;
            return this;
        }

        public Builder setTimestamp(long timestamp) {
            this.timestamp = timestamp;
            return this;
        }

        public Builder setService(String service) {
            this.service = service;
            return this;
        }

        public Builder setOperation(String operation) {
            this.operation = operation;
            return this;
        }

        public Builder setRequestOperation(String requestOperation) {
            this.requestOperation = requestOperation;
            return this;
        }

        public Builder setTimeElapsed(int timeElapsed) {
            this.timeElapsed = timeElapsed;
            return this;
        }

        public ApiCall build() {
            return new ApiCall(this);
        }
    }

}
