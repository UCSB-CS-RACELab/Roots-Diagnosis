package edu.ucsb.cs.roots.data;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.ListMultimap;
import edu.ucsb.cs.roots.RootsEnvironment;

import java.util.ArrayList;
import java.util.List;

public class TestDataStore implements DataStore {

    public static final String GET_BENCHMARK_RESULTS = "GET_BENCHMARK_RESULTS";

    private final List<DataStoreCall> calls = new ArrayList<>();

    private ListMultimap<String,BenchmarkResult> benchmarkResults = ArrayListMultimap.create();

    private final String name;

    public TestDataStore(RootsEnvironment environment, String name) {
        this.name = name;
        environment.getDataStoreService().put(name, this);
    }

    public String getName() {
        return name;
    }

    public void addBenchmarkResult(String operation, BenchmarkResult entry) {
        benchmarkResults.put(operation, entry);
    }

    @Override
    public ImmutableListMultimap<String,BenchmarkResult> getBenchmarkResults(
            String application, long start, long end) throws DataStoreException {
        calls.add(new DataStoreCall(start, end, GET_BENCHMARK_RESULTS, application));
        ImmutableListMultimap.Builder<String,BenchmarkResult> builder = ImmutableListMultimap.builder();
        builder.putAll(benchmarkResults);
        benchmarkResults.clear();
        return builder.build();
    }

    public int callCount() {
        return calls.size();
    }

    public List<DataStoreCall> getCallsAndClear() {
        ImmutableList<DataStoreCall> copy = ImmutableList.copyOf(calls);
        calls.clear();
        return copy;
    }

}
