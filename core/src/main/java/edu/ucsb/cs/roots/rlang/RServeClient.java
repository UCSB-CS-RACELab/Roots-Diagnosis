package edu.ucsb.cs.roots.rlang;

import org.rosuda.REngine.REngineException;
import org.rosuda.REngine.Rserve.RConnection;
import org.rosuda.REngine.Rserve.RserveException;

import java.util.HashSet;
import java.util.Set;

public final class RServeClient implements RClient {

    private final RConnection r;
    private final Set<String> symbols = new HashSet<>();

    public RServeClient() throws RserveException {
        r = new RConnection();
        r.eval("library('dtw')");
        r.eval("library('changepoint')");
        r.eval("library('relaimpo')");
        r.eval("library('tsoutliers')");
    }

    @Override
    public void assign(String symbol, double[] values) throws REngineException {
        r.assign(symbol, values);
        symbols.add(symbol);
    }

    @Override
    public void assign(String symbol, String[] values) throws REngineException {
        r.assign(symbol, values);
        symbols.add(symbol);
    }

    @Override
    public void eval(String cmd) throws REngineException {
        r.eval(cmd);
    }

    @Override
    public void evalAndAssign(String symbol, String cmd) throws REngineException {
        r.eval(symbol + " <- " + cmd);
        symbols.add(symbol);
    }

    @Override
    public double evalToDouble(String cmd) throws Exception {
        return r.eval(cmd).asDouble();
    }

    @Override
    public double[] evalToDoubles(String cmd) throws Exception {
        return r.eval(cmd).asDoubles();
    }

    @Override
    public int[] evalToInts(String cmd) throws Exception {
        return r.eval(cmd).asIntegers();
    }

    @Override
    public String[] evalToStrings(String cmd) throws Exception {
        return r.eval(cmd).asStrings();
    }

    @Override
    public void cleanup() throws REngineException {
        for (String s : symbols) {
            r.eval("rm(" + s + ")");
        }
        symbols.clear();
    }

    @Override
    public void close() {
        r.close();
    }
}
