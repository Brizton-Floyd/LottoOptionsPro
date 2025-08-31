package com.example.lottooptionspro.model.smart;

import java.util.Map;

public class QualityMetrics {
    private double optimizationScore;
    private String qualityGrade;
    private Map<String, Double> patternDistribution;
    private double coveragePercentage;
    private double consecutiveRate;
    private double repeatRate;
    private double uniquenessScore;
    private double overlapPercentage;
    private int totalTicketsAnalyzed;
    private String analysisTimestamp;
    private String lotteryConfigId;
    private ImprovementMetrics improvementMetrics;
    private boolean highQuality;
    private String summary;
    private double hotPatternPercentage;
    private double warmPatternPercentage;
    private double coldPatternPercentage;

    public double getOptimizationScore() {
        return optimizationScore;
    }

    public void setOptimizationScore(double optimizationScore) {
        this.optimizationScore = optimizationScore;
    }

    public String getQualityGrade() {
        return qualityGrade;
    }

    public void setQualityGrade(String qualityGrade) {
        this.qualityGrade = qualityGrade;
    }

    public Map<String, Double> getPatternDistribution() {
        return patternDistribution;
    }

    public void setPatternDistribution(Map<String, Double> patternDistribution) {
        this.patternDistribution = patternDistribution;
    }

    public double getCoveragePercentage() {
        return coveragePercentage;
    }

    public void setCoveragePercentage(double coveragePercentage) {
        this.coveragePercentage = coveragePercentage;
    }

    public double getConsecutiveRate() {
        return consecutiveRate;
    }

    public void setConsecutiveRate(double consecutiveRate) {
        this.consecutiveRate = consecutiveRate;
    }

    public double getRepeatRate() {
        return repeatRate;
    }

    public void setRepeatRate(double repeatRate) {
        this.repeatRate = repeatRate;
    }

    public double getUniquenessScore() {
        return uniquenessScore;
    }

    public void setUniquenessScore(double uniquenessScore) {
        this.uniquenessScore = uniquenessScore;
    }

    public double getOverlapPercentage() {
        return overlapPercentage;
    }

    public void setOverlapPercentage(double overlapPercentage) {
        this.overlapPercentage = overlapPercentage;
    }

    public int getTotalTicketsAnalyzed() {
        return totalTicketsAnalyzed;
    }

    public void setTotalTicketsAnalyzed(int totalTicketsAnalyzed) {
        this.totalTicketsAnalyzed = totalTicketsAnalyzed;
    }

    public String getAnalysisTimestamp() {
        return analysisTimestamp;
    }

    public void setAnalysisTimestamp(String analysisTimestamp) {
        this.analysisTimestamp = analysisTimestamp;
    }

    public String getLotteryConfigId() {
        return lotteryConfigId;
    }

    public void setLotteryConfigId(String lotteryConfigId) {
        this.lotteryConfigId = lotteryConfigId;
    }

    public ImprovementMetrics getImprovementMetrics() {
        return improvementMetrics;
    }

    public void setImprovementMetrics(ImprovementMetrics improvementMetrics) {
        this.improvementMetrics = improvementMetrics;
    }

    public boolean isHighQuality() {
        return highQuality;
    }

    public void setHighQuality(boolean highQuality) {
        this.highQuality = highQuality;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public double getHotPatternPercentage() {
        return hotPatternPercentage;
    }

    public void setHotPatternPercentage(double hotPatternPercentage) {
        this.hotPatternPercentage = hotPatternPercentage;
    }

    public double getWarmPatternPercentage() {
        return warmPatternPercentage;
    }

    public void setWarmPatternPercentage(double warmPatternPercentage) {
        this.warmPatternPercentage = warmPatternPercentage;
    }

    public double getColdPatternPercentage() {
        return coldPatternPercentage;
    }

    public void setColdPatternPercentage(double coldPatternPercentage) {
        this.coldPatternPercentage = coldPatternPercentage;
    }

    public static class ImprovementMetrics {
        private double generatedPerformance;
        private double baselinePerformance;
        private double improvementPercentage;
        private boolean twentyPercentTargetMet;
        private String targetTier;

        public double getGeneratedPerformance() {
            return generatedPerformance;
        }

        public void setGeneratedPerformance(double generatedPerformance) {
            this.generatedPerformance = generatedPerformance;
        }

        public double getBaselinePerformance() {
            return baselinePerformance;
        }

        public void setBaselinePerformance(double baselinePerformance) {
            this.baselinePerformance = baselinePerformance;
        }

        public double getImprovementPercentage() {
            return improvementPercentage;
        }

        public void setImprovementPercentage(double improvementPercentage) {
            this.improvementPercentage = improvementPercentage;
        }

        public boolean isTwentyPercentTargetMet() {
            return twentyPercentTargetMet;
        }

        public void setTwentyPercentTargetMet(boolean twentyPercentTargetMet) {
            this.twentyPercentTargetMet = twentyPercentTargetMet;
        }

        public String getTargetTier() {
            return targetTier;
        }

        public void setTargetTier(String targetTier) {
            this.targetTier = targetTier;
        }
    }
}