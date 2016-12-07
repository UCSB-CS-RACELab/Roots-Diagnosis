package edu.ucsb.cs.roots.utils;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicLong;

public final class RootsThreadFactory implements ThreadFactory {

    private final String prefix;
    private final AtomicLong count = new AtomicLong(0);

    public RootsThreadFactory(String prefix) {
        this.prefix = prefix;
    }

    @Override
    public Thread newThread(Runnable r) {
        return new Thread(r, prefix + "-" + count.getAndIncrement());
    }

}
