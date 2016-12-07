package edu.ucsb.cs.roots.data;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.ImmutableMap;

public interface DataStore {

    default void init() {
    }

    default void destroy() {
    }

    /**
     * Retrieve the response time statistics for the specified application by analyzing
     * the request traffic within the specified interval. Returns a map of request types
     * and response time data corresponding to each request type.
     *
     * @param application Name of the application
     * @param start Start time of the interval (inclusive)
     * @param end End time of the interval (exclusive)
     * @return A Map of request types (String) and response time data (ResponseTimeSummary)
     */
    default ImmutableMap<String,ResponseTimeSummary> getResponseTimeSummary(
            String application, long start, long end) throws DataStoreException {
        return ImmutableMap.of();
    }

    default ImmutableListMultimap<String,ResponseTimeSummary> getResponseTimeHistory(
            String application, long start, long end, long period) throws DataStoreException {
        return ImmutableListMultimap.of();
    }

    default ImmutableList<Double> getWorkloadSummary(
            String application, String operation, long start, long end,
            long period) throws DataStoreException {
        return ImmutableList.of();
    }

    /**
     * Retrieve the HTTP API benchmark results for the specified application by analyzing the
     * data gathered during the specified interval. Returns a map of request types and benchmarking
     * results for each request type.
     *
     * @param application Name of the application
     * @param start Start time of the interval (inclusive)
     * @param end End time of the interval (exclusive)
     * @return A Map of request types (String) and benchmark results for each type
     */
    default ImmutableListMultimap<String,BenchmarkResult> getBenchmarkResults(
            String application, long start, long end) throws DataStoreException {
        return ImmutableListMultimap.of();
    }

    /**
     * Retrieve the requests processed by the specified application, during the specified time
     * interval. Returns a map of application requests keyed by the operations. ApplicationRequests
     * for each operation are sorted by the timestamp.
     *
     * @param application Name of the application
     * @param start Start time of the interval (inclusive)
     * @param end End time of the interval (exclusive)
     * @return A Map of operation names and sorted ApplicationRequest instances
     */
    default ImmutableListMultimap<String,ApplicationRequest> getRequestInfo(
            String application, long start, long end) throws DataStoreException {
        return ImmutableListMultimap.of();
    }

    /**
     * Retrieve the requests processed by the specified application and operation, during the
     * specified time interval. Returns a list of application requests sorted by timestamp.
     *
     * @param application Name of the application
     * @param operation Name of the operation
     * @param start Start time of the interval (inclusive)
     * @param end End time of the interval (exclusive)
     * @return A sorted list of ApplicationRequest instances
     */
    default ImmutableList<ApplicationRequest> getRequestInfo(
            String application, String operation, long start, long end) throws DataStoreException {
        return ImmutableList.of();
    }

    default void recordBenchmarkResult(BenchmarkResult result) throws DataStoreException {
    }

}
