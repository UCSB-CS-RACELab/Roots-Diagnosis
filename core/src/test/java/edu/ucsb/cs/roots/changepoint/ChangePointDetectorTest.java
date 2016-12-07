package edu.ucsb.cs.roots.changepoint;

import junit.framework.Assert;
import org.junit.Test;

import java.util.Arrays;

public class ChangePointDetectorTest {

    @Test
    public void testNoChangePoints() throws Exception {
        TestDetector detector = new TestDetector(new int[]{});
        Segment[] segments = detector.computeSegments(new double[]{1,2,1,2});
        Assert.assertEquals(1, segments.length);
        Assert.assertEquals(0, segments[0].getStart());
        Assert.assertEquals(4, segments[0].getEnd());
        Assert.assertEquals(1.5, segments[0].getMean());
    }

    @Test
    public void testOneChangePoint() throws Exception {
        TestDetector detector = new TestDetector(new int[]{1});
        Segment[] segments = detector.computeSegments(new double[]{1,2,1,2});
        Assert.assertEquals(2, segments.length);
        Assert.assertEquals(0, segments[0].getStart());
        Assert.assertEquals(2, segments[0].getEnd());
        Assert.assertEquals(1.5, segments[0].getMean());
        Assert.assertEquals(2, segments[1].getStart());
        Assert.assertEquals(4, segments[1].getEnd());
        Assert.assertEquals(1.5, segments[1].getMean());
    }

    @Test
    public void testTwoChangePoints() throws Exception {
        TestDetector detector = new TestDetector(new int[]{1,3});
        Segment[] segments = detector.computeSegments(new double[]{1,2,1,2,1,2});
        Assert.assertEquals(3, segments.length);
        Assert.assertEquals(0, segments[0].getStart());
        Assert.assertEquals(2, segments[0].getEnd());
        Assert.assertEquals(1.5, segments[0].getMean());
        Assert.assertEquals(2, segments[1].getStart());
        Assert.assertEquals(4, segments[1].getEnd());
        Assert.assertEquals(1.5, segments[1].getMean());
        Assert.assertEquals(4, segments[2].getStart());
        Assert.assertEquals(6, segments[2].getEnd());
        Assert.assertEquals(1.5, segments[2].getMean());
    }

    @Test
    public void testError() throws Exception {
        int[] indices = {2, 4, 5, 6, 7, 9, 10, 11, 12, 13, 14, 15, 16, 17, 20, 21, 23};
        TestDetector detector = new TestDetector(Arrays.stream(indices).map(i -> i - 2).toArray());
        Segment[] segments = detector.computeSegments(new double[]{1, 2, 3, 4, 5, 6, 7, 8, 9, 10,
                11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23});
        Assert.assertEquals(18, segments.length);

        Segment s = segments[0];
        Assert.assertEquals(0, s.getStart());
        Assert.assertEquals(1, s.getEnd());
        Assert.assertEquals(1.0, s.getMean());

        s = segments[1];
        Assert.assertEquals(1, s.getStart());
        Assert.assertEquals(3, s.getEnd());
        Assert.assertEquals(2.5, s.getMean());

        s = segments[16];
        Assert.assertEquals(20, s.getStart());
        Assert.assertEquals(22, s.getEnd());
        Assert.assertEquals(21.5, s.getMean());

        s = segments[17];
        Assert.assertEquals(22, s.getStart());
        Assert.assertEquals(23, s.getEnd());
        Assert.assertEquals(23.0, s.getMean());
    }

    private static class TestDetector extends ChangePointDetector {

        private final int[] indices;

        TestDetector(int[] indices) {
            this.indices = indices;
        }

        @Override
        protected int[] computeChangePoints(double[] data) throws Exception {
            return indices;
        }
    }

}
