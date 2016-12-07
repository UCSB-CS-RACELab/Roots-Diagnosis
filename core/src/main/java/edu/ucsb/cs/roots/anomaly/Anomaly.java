package edu.ucsb.cs.roots.anomaly;

import com.google.common.base.Strings;

import java.util.UUID;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

public final class Anomaly {

    public static final int TYPE_PERFORMANCE = 100;
    public static final int TYPE_WORKLOAD = 101;

    private final String id;
    private final AnomalyDetector detector;
    private final long start;
    private final long end;
    private final int type;
    private final String operation;
    private final String description;
    private final long previousAnomalyTime;

    private Anomaly(Builder builder) {
        checkNotNull(builder.detector, "Detector is required");
        checkArgument(builder.start > 0 && builder.end > 0 && builder.start < builder.end,
                "Time interval is invalid");
        checkArgument(!Strings.isNullOrEmpty(builder.operation), "Operation is required");
        checkArgument(!Strings.isNullOrEmpty(builder.description), "Description is required");
        this.id = UUID.randomUUID().toString();
        this.detector = builder.detector;
        this.start = builder.start;
        this.end = builder.end;
        this.type = builder.type;
        this.operation = builder.operation;
        this.description = builder.description;
        this.previousAnomalyTime = builder.previousAnomalyTime;
    }

    public String getId() {
        return id;
    }

    public long getStart() {
        return start;
    }

    public long getEnd() {
        return end;
    }

    public long getPreviousAnomalyTime() {
        return previousAnomalyTime;
    }

    public int getType() {
        return type;
    }

    public String getApplication() {
        return detector.getApplication();
    }

    public String getOperation() {
        return operation;
    }

    public String getDataStore() {
        return detector.getDataStore();
    }

    public int getPeriodInSeconds() {
        return detector.getPeriodInSeconds();
    }

    public String getDescription() {
        return description;
    }

    public String getDetectorProperty(String key, String def) {
        return detector.getProperty(key, def);
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    public static class Builder {

        private AnomalyDetector detector;
        private long start;
        private long end;
        private int type;
        private String operation;
        private String description;
        private long previousAnomalyTime = -1L;

        private Builder() {
        }

        public Builder setDetector(AnomalyDetector detector) {
            this.detector = detector;
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

        public Builder setType(int type) {
            this.type = type;
            return this;
        }

        public Builder setOperation(String operation) {
            this.operation = operation;
            return this;
        }

        public Builder setDescription(String description) {
            this.description = description;
            return this;
        }

        public Builder setPreviousAnomalyTime(long previousAnomalyTime) {
            this.previousAnomalyTime = previousAnomalyTime;
            return this;
        }

        public Anomaly build() {
            return new Anomaly(this);
        }
    }
}
