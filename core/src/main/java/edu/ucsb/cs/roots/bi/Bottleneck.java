package edu.ucsb.cs.roots.bi;

import com.google.common.base.Strings;
import edu.ucsb.cs.roots.anomaly.Anomaly;

import java.util.Date;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

public final class Bottleneck {

    private final Anomaly anomaly;
    private final String apiCall;
    private final int index;
    private final Date onsetTime;
    private Object detail;

    public Bottleneck(Anomaly anomaly, String apiCall, int index, Date onsetTime) {
        checkNotNull(anomaly, "Anomaly is required");
        checkArgument(!Strings.isNullOrEmpty(apiCall), "API call is required");
        checkArgument(index >= 0, "Index cannot be negative");
        this.anomaly = anomaly;
        this.apiCall = apiCall;
        this.index = index;
        this.onsetTime = onsetTime;
    }

    public Bottleneck(Anomaly anomaly, String apiCall, int index) {
        this(anomaly, apiCall, index, null);
    }

    public Object getDetail() {
        return detail;
    }

    public Bottleneck setDetail(Object detail) {
        this.detail = detail;
        return this;
    }

    public Anomaly getAnomaly() {
        return anomaly;
    }

    public String getApiCall() {
        return apiCall;
    }

    public int getIndex() {
        return index;
    }

    public Date getOnsetTime() {
        return onsetTime;
    }

    @Override
    public String toString() {
        return String.format("apiCall: %s, onsetTime: %s", apiCall,
                onsetTime != null ? onsetTime.toString() : "Unknown");
    }
}
