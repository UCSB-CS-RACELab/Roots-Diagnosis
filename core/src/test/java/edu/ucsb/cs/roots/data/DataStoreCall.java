package edu.ucsb.cs.roots.data;

public final class DataStoreCall {

    private final long start;
    private final long end;
    private String type;
    private String application;

    public DataStoreCall(long start, long end, String type, String application) {
        this.start = start;
        this.end = end;
        this.type = type;
        this.application = application;
    }

    public long getStart() {
        return start;
    }

    public long getEnd() {
        return end;
    }

    public String getType() {
        return type;
    }

    public String getApplication() {
        return application;
    }
}
