package com.example.lottooptionspro.model.smart;

public class PerformanceComparison {
    private ComparisonData vsRandomTickets;
    private ComparisonData vsAllPossibleCombinations;

    public ComparisonData getVsRandomTickets() {
        return vsRandomTickets;
    }

    public void setVsRandomTickets(ComparisonData vsRandomTickets) {
        this.vsRandomTickets = vsRandomTickets;
    }

    public ComparisonData getVsAllPossibleCombinations() {
        return vsAllPossibleCombinations;
    }

    public void setVsAllPossibleCombinations(ComparisonData vsAllPossibleCombinations) {
        this.vsAllPossibleCombinations = vsAllPossibleCombinations;
    }

    public static class ComparisonData {
        private double performanceFactor;
        private double percentile;
        private String description;

        public double getPerformanceFactor() {
            return performanceFactor;
        }

        public void setPerformanceFactor(double performanceFactor) {
            this.performanceFactor = performanceFactor;
        }

        public double getPercentile() {
            return percentile;
        }

        public void setPercentile(double percentile) {
            this.percentile = percentile;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }
    }
}