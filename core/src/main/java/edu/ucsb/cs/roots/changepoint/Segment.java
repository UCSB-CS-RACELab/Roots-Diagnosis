package edu.ucsb.cs.roots.changepoint;

import java.util.Arrays;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkPositionIndexes;

public final class Segment {

    private final int start;
    private final int end;
    private final double mean;

    /**
     * Create a new data segment.
     *
     * @param start Start index for the data segment (inclusive)
     * @param end End index for the data segment (exclusive)
     * @param data Array of data entries
     */
    public Segment(int start, int end, double[] data) {
        checkArgument(start >= 0 && start < end, "Invalid start: %s or end: %s", start, end);
        checkPositionIndexes(start, end, data.length);
        this.start = start;
        this.end = end;
        this.mean = Arrays.stream(data, start, end).average().getAsDouble();
    }

    public int getStart() {
        return start;
    }

    public int getEnd() {
        return end;
    }

    public double getMean() {
        return mean;
    }

    public double percentageIncrease(Segment other) {
        return (other.mean - this.mean) * 100.0 / this.mean;
    }
}
