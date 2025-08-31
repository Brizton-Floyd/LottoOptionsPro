package com.example.lottooptionspro.model.smart;

import java.util.List;

public class DroughtInformation {
    private String overallStatus;
    private double overallScore;
    private String targetTier;
    private boolean targetTierInDrought;
    private List<DroughtTierInfo> tierInformation;
    private List<String> recommendations;
    private boolean advancedGenerationReady;
    private String recommendedStrategy;
    private String analysisDate;
    private String strategicRecommendations;

    public String getOverallStatus() {
        return overallStatus;
    }

    public void setOverallStatus(String overallStatus) {
        this.overallStatus = overallStatus;
    }

    public double getOverallScore() {
        return overallScore;
    }

    public void setOverallScore(double overallScore) {
        this.overallScore = overallScore;
    }

    public String getTargetTier() {
        return targetTier;
    }

    public void setTargetTier(String targetTier) {
        this.targetTier = targetTier;
    }

    public boolean isTargetTierInDrought() {
        return targetTierInDrought;
    }

    public void setTargetTierInDrought(boolean targetTierInDrought) {
        this.targetTierInDrought = targetTierInDrought;
    }

    public List<DroughtTierInfo> getTierInformation() {
        return tierInformation;
    }

    public void setTierInformation(List<DroughtTierInfo> tierInformation) {
        this.tierInformation = tierInformation;
    }

    public List<String> getRecommendations() {
        return recommendations;
    }

    public void setRecommendations(List<String> recommendations) {
        this.recommendations = recommendations;
    }

    public boolean isAdvancedGenerationReady() {
        return advancedGenerationReady;
    }

    public void setAdvancedGenerationReady(boolean advancedGenerationReady) {
        this.advancedGenerationReady = advancedGenerationReady;
    }

    public String getRecommendedStrategy() {
        return recommendedStrategy;
    }

    public void setRecommendedStrategy(String recommendedStrategy) {
        this.recommendedStrategy = recommendedStrategy;
    }

    public String getAnalysisDate() {
        return analysisDate;
    }

    public void setAnalysisDate(String analysisDate) {
        this.analysisDate = analysisDate;
    }

    public String getStrategicRecommendations() {
        return strategicRecommendations;
    }

    public void setStrategicRecommendations(String strategicRecommendations) {
        this.strategicRecommendations = strategicRecommendations;
    }
}