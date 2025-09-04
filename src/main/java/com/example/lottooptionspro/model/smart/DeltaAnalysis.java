package com.example.lottooptionspro.model.smart;

import java.util.List;

public class DeltaAnalysis {
    private String deltaStrategy;
    private double deltaEfficiencyScore;
    private boolean strategicIntelligenceApplied;
    private PatternAnalysis patternAnalysis;
    private StrategicPerformance strategicPerformance;
    private List<String> actionableInsights;

    public DeltaAnalysis() {}

    public String getDeltaStrategy() {
        return deltaStrategy;
    }

    public void setDeltaStrategy(String deltaStrategy) {
        this.deltaStrategy = deltaStrategy;
    }

    public double getDeltaEfficiencyScore() {
        return deltaEfficiencyScore;
    }

    public void setDeltaEfficiencyScore(double deltaEfficiencyScore) {
        this.deltaEfficiencyScore = deltaEfficiencyScore;
    }

    public boolean isStrategicIntelligenceApplied() {
        return strategicIntelligenceApplied;
    }

    public void setStrategicIntelligenceApplied(boolean strategicIntelligenceApplied) {
        this.strategicIntelligenceApplied = strategicIntelligenceApplied;
    }

    public PatternAnalysis getPatternAnalysis() {
        return patternAnalysis;
    }

    public void setPatternAnalysis(PatternAnalysis patternAnalysis) {
        this.patternAnalysis = patternAnalysis;
    }

    public StrategicPerformance getStrategicPerformance() {
        return strategicPerformance;
    }

    public void setStrategicPerformance(StrategicPerformance strategicPerformance) {
        this.strategicPerformance = strategicPerformance;
    }

    public List<String> getActionableInsights() {
        return actionableInsights;
    }

    public void setActionableInsights(List<String> actionableInsights) {
        this.actionableInsights = actionableInsights;
    }

    public String getFormattedEfficiencyScore() {
        return String.format("%.1f%%", deltaEfficiencyScore * 100);
    }

    public boolean hasPatternAnalysis() {
        return patternAnalysis != null;
    }

    public boolean hasStrategicPerformance() {
        return strategicPerformance != null;
    }

    public boolean hasActionableInsights() {
        return actionableInsights != null && !actionableInsights.isEmpty();
    }

    public static class PatternAnalysis {
        private int totalPatterns;
        private int arithmeticProgressions;
        private double averageGapSize;

        public PatternAnalysis() {}

        public int getTotalPatterns() {
            return totalPatterns;
        }

        public void setTotalPatterns(int totalPatterns) {
            this.totalPatterns = totalPatterns;
        }

        public int getArithmeticProgressions() {
            return arithmeticProgressions;
        }

        public void setArithmeticProgressions(int arithmeticProgressions) {
            this.arithmeticProgressions = arithmeticProgressions;
        }

        public double getAverageGapSize() {
            return averageGapSize;
        }

        public void setAverageGapSize(double averageGapSize) {
            this.averageGapSize = averageGapSize;
        }

        public String getFormattedAverageGapSize() {
            return String.format("%.1f", averageGapSize);
        }
    }

    public static class StrategicPerformance {
        private double overallStrategicScore;
        private double droughtOptimizationScore;
        private double tierOptimizationScore;

        public StrategicPerformance() {}

        public double getOverallStrategicScore() {
            return overallStrategicScore;
        }

        public void setOverallStrategicScore(double overallStrategicScore) {
            this.overallStrategicScore = overallStrategicScore;
        }

        public double getDroughtOptimizationScore() {
            return droughtOptimizationScore;
        }

        public void setDroughtOptimizationScore(double droughtOptimizationScore) {
            this.droughtOptimizationScore = droughtOptimizationScore;
        }

        public double getTierOptimizationScore() {
            return tierOptimizationScore;
        }

        public void setTierOptimizationScore(double tierOptimizationScore) {
            this.tierOptimizationScore = tierOptimizationScore;
        }

        public String getFormattedOverallScore() {
            return String.format("%.1f%%", overallStrategicScore * 100);
        }

        public String getFormattedDroughtScore() {
            return String.format("%.1f%%", droughtOptimizationScore * 100);
        }

        public String getFormattedTierScore() {
            return String.format("%.1f%%", tierOptimizationScore * 100);
        }
    }
}