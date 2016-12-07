package edu.ucsb.cs.roots.bi;

public final class RelativeImportance implements Comparable<RelativeImportance> {

    private final String apiCall;
    private final double importance;
    private int ranking;

    RelativeImportance(String apiCall, double importance) {
        this.apiCall = apiCall;
        this.importance = importance;
    }

    public String getApiCall() {
        return apiCall;
    }

    public double getImportance() {
        return importance;
    }

    public int getRanking() {
        return ranking;
    }

    void setRanking(int ranking) {
        this.ranking = ranking;
    }

    @Override
    public int compareTo(RelativeImportance o) {
        return Double.compare(this.importance, o.importance);
    }

    @Override
    public String toString() {
        return String.format("[%2d] %s %f", ranking, apiCall, importance);
    }
}
