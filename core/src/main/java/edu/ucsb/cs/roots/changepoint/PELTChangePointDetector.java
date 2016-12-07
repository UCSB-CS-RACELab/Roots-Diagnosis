package edu.ucsb.cs.roots.changepoint;

import edu.ucsb.cs.roots.rlang.RClient;
import edu.ucsb.cs.roots.rlang.RService;

import java.util.Arrays;

public class PELTChangePointDetector extends RChangePointDetector {

    public PELTChangePointDetector(RService rService) {
        super(rService);
    }

    @Override
    public final int[] computeChangePoints(double[] data) throws Exception {
        RClient r = rService.borrow();
        try {
            r.assign("x", data);
            r.evalAndAssign("result", getRCall());
            int[] indices = r.evalToInts("cpts(result)");
            if (indices.length == 0 || indices[0] == 0) {
                return new int[]{};
            }

            // Indices returned by the 'changepoints' library represent
            // the R indices of the last values of the segments.
            return Arrays.stream(indices).map(i -> i - 1).toArray();
        } finally {
            rService.release(r);
        }
    }

    protected String getRCall() {
        return "cpt.mean(x, method='PELT')";
    }

}
