package edu.ucsb.cs.roots.data;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSortedSet;
import edu.ucsb.cs.roots.utils.ImmutableCollectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

public class RandomDataStore implements DataStore {

    private static final Logger log = LoggerFactory.getLogger(RandomDataStore.class);

    private static final Random RAND = new Random();

    @Override
    public ImmutableMap<String, ResponseTimeSummary> getResponseTimeSummary(
            String application, long start, long end) {
        List<BenchmarkResult> logEntries = generateRandomResults(application, start, end);
        Map<String,List<BenchmarkResult>> groupedEntries = logEntries.stream()
                .collect(Collectors.groupingBy(BenchmarkResult::getRequestType));
        return groupedEntries.entrySet().stream().collect(ImmutableCollectors.toMap(
                        Map.Entry::getKey, e -> new ResponseTimeSummary(start, e.getValue())));
    }

    @Override
    public ImmutableList<Double> getWorkloadSummary(
            String application, String operation, long start, long end,
            long period) throws DataStoreException {
        ImmutableList.Builder<Double> builder = ImmutableList.builder();
        double changePoint = start + (end - start) * 0.7;
        boolean injectChange = RAND.nextBoolean();
        for (long i = start; i < end; i += period) {
            Double element = (double) RAND.nextInt(10);
            if (injectChange && i >= changePoint) {
                element += 20;
            }
            builder.add(element);
        }
        return builder.build();
    }

    @Override
    public ImmutableListMultimap<String,ResponseTimeSummary> getResponseTimeHistory(
            String application, long start, long end, long period) {
        ImmutableMap<String,ResponseTimeSummary> lastPeriod = getResponseTimeSummary(
                application, end - period, end);
        ImmutableListMultimap.Builder<String,ResponseTimeSummary> builder =
                ImmutableListMultimap.builder();
        lastPeriod.forEach(builder::put);
        return builder.build();
    }

    @Override
    public ImmutableListMultimap<String,BenchmarkResult> getBenchmarkResults(
            String application, long start, long end) {
        List<BenchmarkResult> logEntries = generateRandomResults(application, start, end, 2);
        Map<String,List<BenchmarkResult>> groupedEntries = logEntries.stream()
                .collect(Collectors.groupingBy(BenchmarkResult::getRequestType));
        ImmutableListMultimap.Builder<String,BenchmarkResult> builder =
                ImmutableListMultimap.builder();
        groupedEntries.forEach(builder::putAll);
        return builder.build();
    }

    @Override
    public ImmutableListMultimap<String,ApplicationRequest> getRequestInfo(
            String application, long start, long end) {
        ImmutableListMultimap.Builder<String,ApplicationRequest> builder =
                ImmutableListMultimap.builder();
        builder.putAll("GET /", getApplicationRequests(
                application, "GET /", start, end, RAND.nextInt(50), 3));
        builder.putAll("POST /", getApplicationRequests(
                application, "POST /", start, end, RAND.nextInt(50), 2));
        return builder.build();
    }

    @Override
    public ImmutableList<ApplicationRequest> getRequestInfo(
            String application, String operation, long start, long end) throws DataStoreException {
        return getApplicationRequests(application, operation, start, end, RAND.nextInt(50), 3);
    }

    @Override
    public void recordBenchmarkResult(BenchmarkResult entry) {
        log.info("Recording access log entry for {} {} {} {}", entry.getApplication(),
                entry.getMethod(), entry.getPath(), entry.getResponseTime());
    }

    private ImmutableList<ApplicationRequest> getApplicationRequests(
            String application, String operation, long start, long end, int recordCount,
            int apiCalls) {
        ImmutableSortedSet.Builder<ApplicationRequest> builder = ImmutableSortedSet.orderedBy(
                ApplicationRequest.TIME_ORDER);
        for (int i = 0; i < recordCount; i++) {
            long offset = RAND.nextInt((int) (end - start));
            ImmutableList.Builder<ApiCall> callBuilder = ImmutableList.builder();
            for (int j = 0; j < apiCalls; j++) {
                ApiCall call = ApiCall.newBuilder()
                        .setService("datastore")
                        .setOperation("op" + j)
                        .setRequestTimestamp(start + offset)
                        .setTimeElapsed(RAND.nextInt(30))
                        .setTimestamp(start + offset)
                        .setRequestOperation(operation)
                        .build();
                callBuilder.add(call);
            }
            ImmutableList<ApiCall> apiCallList = callBuilder.build();
            int total = apiCallList.stream().mapToInt(ApiCall::getTimeElapsed).sum()
                    + RAND.nextInt(10);
            ApplicationRequest record = new ApplicationRequest(UUID.randomUUID().toString(),
                    start + offset, application, operation, apiCallList, total);
            builder.add(record);
        }
        return ImmutableList.copyOf(builder.build());
    }

    private List<BenchmarkResult> generateRandomResults(
            String application, long start, long end) {
        return generateRandomResults(application, start, end, RAND.nextInt(100));
    }

    private ImmutableList<BenchmarkResult> generateRandomResults(
            String application, long start, long end, int recordCount) {
        ImmutableList.Builder<BenchmarkResult> builder = ImmutableList.builder();
        for (int i = 0; i < recordCount; i++) {
            long offset = RAND.nextInt((int) (end - start));
            String method;
            if (i % 2 == 0) {
                method = "GET";
            } else {
                method = "POST";
            }
            BenchmarkResult record = new BenchmarkResult(start + offset, application,
                    method, "/", RAND.nextInt(50));
            builder.add(record);
        }
        return builder.build();
    }
}
