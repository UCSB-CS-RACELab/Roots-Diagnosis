package edu.ucsb.cs.roots.changepoint;

import edu.ucsb.cs.roots.rlang.RClient;
import edu.ucsb.cs.roots.rlang.RService;

import java.util.Arrays;

/**
 * A change point detector based on "Joint Estimation of Model Parameters and Outlier
 * Effects in Time Series" by Chen and Liu (1993). Uses the tsoutliers package of R.
 */
public class CLChangePointDetector extends RChangePointDetector {

    public CLChangePointDetector(RService rService) {
        super(rService);
    }

    @Override
    public int[] computeChangePoints(double[] data) throws Exception {
        RClient client = rService.borrow();
        try {
            client.assign("x", data);
            client.evalAndAssign("x_ts", "ts(x)");
            client.evalAndAssign("result", "tso(x_ts, types=c('LS'))");
            int[] result = client.evalToInts("result$outliers[,2]");
            return Arrays.stream(result).filter(i -> i > 1).map(i -> i - 2).toArray();
        } finally {
            rService.release(client);
        }
    }
}
