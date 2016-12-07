package edu.ucsb.cs.roots.rlang;

public interface RClient {

    void assign(String symbol, double[] values) throws Exception;

    void assign(String symbol, String[] values) throws Exception;

    void eval(String cmd) throws Exception;

    void evalAndAssign(String symbol, String cmd) throws Exception;

    double evalToDouble(String cmd) throws Exception;

    double[] evalToDoubles(String cmd) throws Exception;

    int[] evalToInts(String cmd) throws Exception;

    String[] evalToStrings(String cmd) throws Exception;

    /**
     * Clean up the RClient instance for reuse
     */
    void cleanup() throws Exception;

    /**
     * Permanently terminate the RClient instance
     */
    void close();

}
