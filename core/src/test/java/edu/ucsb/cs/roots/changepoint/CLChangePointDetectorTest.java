package edu.ucsb.cs.roots.changepoint;

import edu.ucsb.cs.roots.ConfigLoader;
import edu.ucsb.cs.roots.RootsEnvironment;
import edu.ucsb.cs.roots.rlang.RService;
import edu.ucsb.cs.roots.rlang.RTestClient;
import org.junit.Assert;
import org.junit.Test;

public class CLChangePointDetectorTest {

    @Test
    public void testDetectorNoChangePoints() throws Exception {
        RootsEnvironment environment = new RootsEnvironment("Test", new ConfigLoader() {});
        RService rService = RTestClient.newRService(environment, RTestClient.newBuilder()
                .register("result$outliers[,2]", new int[]{1})
                .register("result$outliers[,2]", new int[]{}));
        CLChangePointDetector detector = new CLChangePointDetector(rService);
        int[] result = detector.computeChangePoints(new double[]{1,2,3});
        Assert.assertArrayEquals(new int[]{}, result);

        result = detector.computeChangePoints(new double[]{1,2,3});
        Assert.assertArrayEquals(new int[]{}, result);
    }

    @Test
    public void testDetectorOneChangePoint() throws Exception {
        RootsEnvironment environment = new RootsEnvironment("Test", new ConfigLoader() {});
        RService rService = RTestClient.newRService(environment, RTestClient.newBuilder()
                .register("result$outliers[,2]", new int[]{5}));
        CLChangePointDetector detector = new CLChangePointDetector(rService);
        int[] result = detector.computeChangePoints(new double[]{1,2,3});
        Assert.assertArrayEquals(new int[]{3}, result);
    }

    @Test
    public void testDetectorMultipleChangePoints() throws Exception {
        RootsEnvironment environment = new RootsEnvironment("Test", new ConfigLoader() {});
        RService rService = RTestClient.newRService(environment, RTestClient.newBuilder()
                .register("result$outliers[,2]", new int[]{5, 18, 30}));
        CLChangePointDetector detector = new CLChangePointDetector(rService);
        int[] result = detector.computeChangePoints(new double[]{1,2,3});
        Assert.assertArrayEquals(new int[]{3, 16, 28}, result);
    }

}
