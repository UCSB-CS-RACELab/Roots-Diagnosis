package edu.ucsb.cs.roots;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

public abstract class ManagedService {

    protected final RootsEnvironment environment;
    protected final Logger log = LoggerFactory.getLogger(getClass());

    private State state;

    public ManagedService(RootsEnvironment environment) {
        checkNotNull(environment);
        this.environment = environment;
        this.state = State.STANDBY;
    }

    public final void init() throws Exception {
        checkState(state == State.STANDBY);
        doInit();
        state = State.INITIALIZED;
    }

    public final void destroy() {
        checkState(state == State.INITIALIZED);
        doDestroy();
        state = State.DESTROYED;
    }

    public final State getState() {
        return state;
    }

    protected abstract void doInit() throws Exception;
    protected abstract void doDestroy();
}
