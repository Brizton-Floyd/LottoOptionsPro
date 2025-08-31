package com.example.lottooptionspro.model.smart;

import java.util.Map;

public class GenerationSummary {
    private int totalAttempts;
    private int successfulBatches;
    private int failedBatches;
    private String startTime;
    private String endTime;
    private String stoppingReason;
    private String stoppingCriteriaType;
    private Map<String, Integer> strategyUsageCount;
    private double finalQualityScore;
    private double finalPatternCoverage;
    private double budgetUsed;
    private double successRate;
    private String performanceSummary;
    private double elapsedTimeSeconds;
    private double averageAttemptsPerSuccess;

    public int getTotalAttempts() {
        return totalAttempts;
    }

    public void setTotalAttempts(int totalAttempts) {
        this.totalAttempts = totalAttempts;
    }

    public int getSuccessfulBatches() {
        return successfulBatches;
    }

    public void setSuccessfulBatches(int successfulBatches) {
        this.successfulBatches = successfulBatches;
    }

    public int getFailedBatches() {
        return failedBatches;
    }

    public void setFailedBatches(int failedBatches) {
        this.failedBatches = failedBatches;
    }

    public String getStartTime() {
        return startTime;
    }

    public void setStartTime(String startTime) {
        this.startTime = startTime;
    }

    public String getEndTime() {
        return endTime;
    }

    public void setEndTime(String endTime) {
        this.endTime = endTime;
    }

    public String getStoppingReason() {
        return stoppingReason;
    }

    public void setStoppingReason(String stoppingReason) {
        this.stoppingReason = stoppingReason;
    }

    public String getStoppingCriteriaType() {
        return stoppingCriteriaType;
    }

    public void setStoppingCriteriaType(String stoppingCriteriaType) {
        this.stoppingCriteriaType = stoppingCriteriaType;
    }

    public Map<String, Integer> getStrategyUsageCount() {
        return strategyUsageCount;
    }

    public void setStrategyUsageCount(Map<String, Integer> strategyUsageCount) {
        this.strategyUsageCount = strategyUsageCount;
    }

    public double getFinalQualityScore() {
        return finalQualityScore;
    }

    public void setFinalQualityScore(double finalQualityScore) {
        this.finalQualityScore = finalQualityScore;
    }

    public double getFinalPatternCoverage() {
        return finalPatternCoverage;
    }

    public void setFinalPatternCoverage(double finalPatternCoverage) {
        this.finalPatternCoverage = finalPatternCoverage;
    }

    public double getBudgetUsed() {
        return budgetUsed;
    }

    public void setBudgetUsed(double budgetUsed) {
        this.budgetUsed = budgetUsed;
    }

    public double getSuccessRate() {
        return successRate;
    }

    public void setSuccessRate(double successRate) {
        this.successRate = successRate;
    }

    public String getPerformanceSummary() {
        return performanceSummary;
    }

    public void setPerformanceSummary(String performanceSummary) {
        this.performanceSummary = performanceSummary;
    }

    public double getElapsedTimeSeconds() {
        return elapsedTimeSeconds;
    }

    public void setElapsedTimeSeconds(double elapsedTimeSeconds) {
        this.elapsedTimeSeconds = elapsedTimeSeconds;
    }

    public double getAverageAttemptsPerSuccess() {
        return averageAttemptsPerSuccess;
    }

    public void setAverageAttemptsPerSuccess(double averageAttemptsPerSuccess) {
        this.averageAttemptsPerSuccess = averageAttemptsPerSuccess;
    }
}