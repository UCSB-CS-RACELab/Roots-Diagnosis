package edu.ucsb.cs.roots.scheduling;

import com.google.common.base.Strings;

import java.util.concurrent.atomic.AtomicLong;

import static com.google.common.base.Preconditions.checkArgument;

public abstract class ScheduledItem {

    private static final AtomicLong counter = new AtomicLong(0);

    private final String id;
    protected final String application;
    protected final int periodInSeconds;

    public ScheduledItem(String application, int periodInSeconds) {
        checkArgument(!Strings.isNullOrEmpty(application), "Application name is required");
        checkArgument(periodInSeconds > 0, "Period must be a positive integer");
        this.id = String.format("%s:%s:%d", getClass().getSimpleName(), application,
                counter.getAndIncrement());
        this.application = application;
        this.periodInSeconds = periodInSeconds;
    }

    String getId() {
        return id;
    }

    public final String getApplication() {
        return application;
    }

    public final int getPeriodInSeconds() {
        return periodInSeconds;
    }

    public abstract void run(long now);
}
