package edu.ucsb.cs.roots.rlang;

import org.apache.commons.pool2.BasePooledObjectFactory;
import org.apache.commons.pool2.PooledObject;
import org.apache.commons.pool2.impl.DefaultPooledObject;

public class RClientPoolFactory extends BasePooledObjectFactory<RClient> {

    @Override
    public RClient create() throws Exception {
        return new RServeClient();
    }

    @Override
    public final PooledObject<RClient> wrap(RClient rClient) {
        return new DefaultPooledObject<>(rClient);
    }

    @Override
    public final void passivateObject(PooledObject<RClient> p) throws Exception {
        super.passivateObject(p);
        p.getObject().cleanup();
    }

    @Override
    public final void destroyObject(PooledObject<RClient> p) throws Exception {
        super.destroyObject(p);
        p.getObject().close();
    }
}
