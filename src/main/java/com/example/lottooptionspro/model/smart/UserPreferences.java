package com.example.lottooptionspro.model.smart;

import java.util.List;

public class UserPreferences {
    private boolean avoidConsecutive = false;
    private List<Integer> preferredNumbers;
    private List<Integer> excludeNumbers;
    private int minHotPatterns = 3;
    private int maxColdPatterns = 2;
    private double targetCoveragePercentage = 80.0;
    private double minQualityScore = 72.0;
    private boolean preferBalancedDistribution = true;
    private boolean enableMultiBatch = true;
    private int maxBatchVariance = 3;
    private boolean preventDuplicates = true;

    public boolean isAvoidConsecutive() {
        return avoidConsecutive;
    }

    public void setAvoidConsecutive(boolean avoidConsecutive) {
        this.avoidConsecutive = avoidConsecutive;
    }

    public List<Integer> getPreferredNumbers() {
        return preferredNumbers;
    }

    public void setPreferredNumbers(List<Integer> preferredNumbers) {
        this.preferredNumbers = preferredNumbers;
    }

    public List<Integer> getExcludeNumbers() {
        return excludeNumbers;
    }

    public void setExcludeNumbers(List<Integer> excludeNumbers) {
        this.excludeNumbers = excludeNumbers;
    }

    public int getMinHotPatterns() {
        return minHotPatterns;
    }

    public void setMinHotPatterns(int minHotPatterns) {
        this.minHotPatterns = minHotPatterns;
    }

    public int getMaxColdPatterns() {
        return maxColdPatterns;
    }

    public void setMaxColdPatterns(int maxColdPatterns) {
        this.maxColdPatterns = maxColdPatterns;
    }

    public double getTargetCoveragePercentage() {
        return targetCoveragePercentage;
    }

    public void setTargetCoveragePercentage(double targetCoveragePercentage) {
        this.targetCoveragePercentage = targetCoveragePercentage;
    }

    public double getMinQualityScore() {
        return minQualityScore;
    }

    public void setMinQualityScore(double minQualityScore) {
        this.minQualityScore = minQualityScore;
    }

    public boolean isPreferBalancedDistribution() {
        return preferBalancedDistribution;
    }

    public void setPreferBalancedDistribution(boolean preferBalancedDistribution) {
        this.preferBalancedDistribution = preferBalancedDistribution;
    }

    public boolean isEnableMultiBatch() {
        return enableMultiBatch;
    }

    public void setEnableMultiBatch(boolean enableMultiBatch) {
        this.enableMultiBatch = enableMultiBatch;
    }

    public int getMaxBatchVariance() {
        return maxBatchVariance;
    }

    public void setMaxBatchVariance(int maxBatchVariance) {
        this.maxBatchVariance = maxBatchVariance;
    }

    public boolean isPreventDuplicates() {
        return preventDuplicates;
    }

    public void setPreventDuplicates(boolean preventDuplicates) {
        this.preventDuplicates = preventDuplicates;
    }
}