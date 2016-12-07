package edu.ucsb.cs.roots.rlang;

import edu.ucsb.cs.roots.ManagedService;
import edu.ucsb.cs.roots.RootsEnvironment;
import org.apache.commons.pool2.impl.GenericObjectPool;

public final class RService extends ManagedService {

    private static final String R_MAX_TOTAL = "r.maxTotal";
    private static final String R_MAX_IDLE = "r.maxIdle";
    private static final String R_MIN_IDLE_TIME_MILLIS = "r.minIdleTimeMillis";

    private final GenericObjectPool<RClient> rClientPool;

    public RService(RootsEnvironment environment) {
        this(environment, new RClientPoolFactory());
    }

    RService(RootsEnvironment environment, RClientPoolFactory factory) {
        super(environment);
        this.rClientPool = new GenericObjectPool<>(factory);
    }

    public RClient borrow() {
        try {
            return rClientPool.borrowObject();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void release(RClient client) {
        if (client != null) {
            rClientPool.returnObject(client);
        }
    }

    @Override
    protected void doInit() throws Exception {
        this.rClientPool.setMaxTotal(Integer.parseInt(
                environment.getProperty(R_MAX_TOTAL, "10")));
        this.rClientPool.setMaxIdle(Integer.parseInt(
                environment.getProperty(R_MAX_IDLE, "2")));
        this.rClientPool.setMinEvictableIdleTimeMillis(Long.parseLong(
                environment.getProperty(R_MIN_IDLE_TIME_MILLIS, "10000")));
    }

    @Override
    protected void doDestroy() {
        rClientPool.close();
    }
}
