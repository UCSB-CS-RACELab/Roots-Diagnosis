package edu.ucsb.cs.roots.changepoint;

import edu.ucsb.cs.roots.ConfigLoader;
import edu.ucsb.cs.roots.RootsEnvironment;
import edu.ucsb.cs.roots.rlang.RService;
import edu.ucsb.cs.roots.rlang.RTestClient;
import org.junit.Assert;
import org.junit.Test;

public class PELTChangePointDetectorTest {

    @Test
    public void testDetectorNoChangePoints() throws Exception {
        RootsEnvironment environment = new RootsEnvironment("Test", new ConfigLoader() {});
        RService rService = RTestClient.newRService(environment, RTestClient.newBuilder()
                .register("cpts(result)", new int[]{0}));
        PELTChangePointDetector detector = new PELTChangePointDetector(rService);
        int[] result = detector.computeChangePoints(new double[]{1,2,3});
        Assert.assertArrayEquals(new int[]{}, result);
    }

    @Test
    public void testDetectorOneChangePoint() throws Exception {
        RootsEnvironment environment = new RootsEnvironment("Test", new ConfigLoader() {});
        RService rService = RTestClient.newRService(environment, RTestClient.newBuilder()
                .register("cpts(result)", new int[]{5}));
        PELTChangePointDetector detector = new PELTChangePointDetector(rService);
        int[] result = detector.computeChangePoints(new double[]{1,2,3});
        Assert.assertArrayEquals(new int[]{4}, result);
    }

    @Test
    public void testDetectorMultipleChangePoints() throws Exception {
        RootsEnvironment environment = new RootsEnvironment("Test", new ConfigLoader() {});
        RService rService = RTestClient.newRService(environment, RTestClient.newBuilder()
                .register("cpts(result)", new int[]{5, 18, 30}));
        PELTChangePointDetector detector = new PELTChangePointDetector(rService);
        int[] result = detector.computeChangePoints(new double[]{1,2,3});
        Assert.assertArrayEquals(new int[]{4, 17, 29}, result);
    }
}
