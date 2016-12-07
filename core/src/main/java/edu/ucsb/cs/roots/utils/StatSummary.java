package edu.ucsb.cs.roots.utils;

import org.apache.commons.math3.stat.descriptive.SummaryStatistics;

import java.util.stream.DoubleStream;

public final class StatSummary {

    private final double mean;
    private final double standardDeviation;

    private StatSummary(double mean, double standardDeviation) {
        this.mean = mean;
        this.standardDeviation = standardDeviation;
    }

    public double getMean() {
        return mean;
    }

    public double getStandardDeviation() {
        return standardDeviation;
    }

    public double getUpperBound(double factor) {
        return mean + factor * standardDeviation;
    }

    public double getLowerBound(double factor) {
        return mean - factor * standardDeviation;
    }

    public boolean isAnomaly(double value, double factor) {
        return value > getUpperBound(factor) || value < getLowerBound(factor);
    }

    public double percentageDifference(double value) {
        return (value - mean)*100.0 / mean;
    }

    public static StatSummary calculate(DoubleStream stream) {
        SummaryStatistics statistics = new SummaryStatistics();
        stream.forEach(statistics::addValue);
        return new StatSummary(statistics.getMean(), statistics.getStandardDeviation());
    }
}
