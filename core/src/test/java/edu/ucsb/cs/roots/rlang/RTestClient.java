package edu.ucsb.cs.roots.rlang;

import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.ListMultimap;
import edu.ucsb.cs.roots.RootsEnvironment;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.google.common.base.Preconditions.checkArgument;

public final class RTestClient implements RClient {

    private final ListMultimap<String,Object> mocks;
    private final Map<String,Integer> counts = new HashMap<>();

    private RTestClient(Builder builder) {
        this.mocks = builder.mocks.build();
        this.mocks.keySet().forEach(k -> counts.put(k, 0));
    }

    private <T> T lookup(String cmd, Class<T> clazz) {
        checkArgument(mocks.containsKey(cmd), "No mocks registered for: %s", cmd);
        List<Object> list = mocks.get(cmd);
        int current = counts.get(cmd);
        checkArgument(current < list.size(), "Insufficient mocks registered for: %s", cmd);
        counts.put(cmd, current + 1);
        return clazz.cast(list.get(current));
    }

    @Override
    public void assign(String symbol, double[] values) throws Exception {
    }

    @Override
    public void assign(String symbol, String[] values) throws Exception {
    }

    @Override
    public void eval(String cmd) throws Exception {
    }

    @Override
    public void evalAndAssign(String symbol, String cmd) throws Exception {
    }

    @Override
    public double evalToDouble(String cmd) throws Exception {
        return lookup(cmd, Double.class);
    }

    @Override
    public double[] evalToDoubles(String cmd) throws Exception {
        return lookup(cmd, double[].class);
    }

    @Override
    public int[] evalToInts(String cmd) throws Exception {
        return lookup(cmd, int[].class);
    }

    @Override
    public String[] evalToStrings(String cmd) throws Exception {
        return lookup(cmd, String[].class);
    }

    @Override
    public void cleanup() throws Exception {
    }

    @Override
    public void close() {
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    public static class Builder {

        private final ImmutableListMultimap.Builder<String,Object> mocks = ImmutableListMultimap.builder();

        private Builder() {
        }

        public Builder register(String cmd, Object mock) {
            mocks.put(cmd, mock);
            return this;
        }

        RTestClient build() {
            return new RTestClient(this);
        }

    }

    public static RService newRService(RootsEnvironment environment, Builder builder) {
        return new RService(environment, new RTestClientPoolFactory(builder));
    }
}
