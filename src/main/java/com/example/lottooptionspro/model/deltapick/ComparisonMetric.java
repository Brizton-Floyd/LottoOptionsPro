package com.example.lottooptionspro.model.deltapick;

/**
 * Comparison metric for performance analysis.
 */
public class ComparisonMetric {
    private Double performanceFactor;
    private Double percentile;
    private String description;

    public ComparisonMetric() {
    }

    public Double getPerformanceFactor() {
        return performanceFactor;
    }

    public void setPerformanceFactor(Double performanceFactor) {
        this.performanceFactor = performanceFactor;
    }

    public Double getPercentile() {
        return percentile;
    }

    public void setPercentile(Double percentile) {
        this.percentile = percentile;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
