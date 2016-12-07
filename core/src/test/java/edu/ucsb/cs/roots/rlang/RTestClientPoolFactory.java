package edu.ucsb.cs.roots.rlang;

public final class RTestClientPoolFactory extends RClientPoolFactory {

    private final RTestClient.Builder builder;

    public RTestClientPoolFactory(RTestClient.Builder builder) {
        this.builder = builder;
    }

    @Override
    public RClient create() throws Exception {
        return builder.build();
    }
}
