package edu.ucsb.cs.roots.changepoint;

public abstract class ChangePointDetector {

    protected abstract int[] computeChangePoints(double[] data) throws Exception;

    public final Segment[] computeSegments(double[] data) throws Exception {
        int[] changePoints = computeChangePoints(data);
        if (changePoints.length == 0) {
            return new Segment[]{ new Segment(0, data.length, data) };
        }
        Segment[] segments = new Segment[changePoints.length + 1];
        int prevIndex = 0;
        for (int i = 0; i <= changePoints.length; i++) {
            int cp;
            if (i == changePoints.length) {
                cp = data.length;
            } else {
                cp = changePoints[i] + 1;
            }
            segments[i] = new Segment(prevIndex, cp, data);
            prevIndex = cp;
        }
        return segments;
    }

}
