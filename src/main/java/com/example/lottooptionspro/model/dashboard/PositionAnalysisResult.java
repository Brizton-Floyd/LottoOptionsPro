package com.example.lottooptionspro.model.dashboard;

import java.util.List;
import java.util.Map;

public class PositionAnalysisResult {
    private final int position;
    private final int totalDrawsAnalyzed;
    private final double averageGamesOut;
    private final double volatility;
    
    // Trend analysis
    private final String primaryTrend;      // BULLISH, BEARISH, SIDEWAYS
    private final double trendStrength;     // 0.0 to 1.0
    private final String trendDuration;     // SHORT, MEDIUM, LONG
    
    // Statistical analysis
    private final Map<Integer, Integer> numberFrequency; // Number -> Count
    private final List<Integer> hotNumbers;              // Most frequent in position
    private final List<Integer> coldNumbers;             // Least frequent in position
    private final List<Integer> overdueNumbers;          // Numbers not seen recently
    
    // Support and resistance levels
    private final List<Double> supportLevels;
    private final List<Double> resistanceLevels;
    
    // Moving average analysis
    private final Map<String, Double> currentMovingAverages; // MA5, MA10, MA20
    private final String movingAverageTrend; // GOLDEN_CROSS, DEATH_CROSS, NEUTRAL
    
    // Predictions and recommendations
    private final List<String> insights;
    private final List<Integer> recommendedNumbers;
    private final double confidenceScore;
    
    public PositionAnalysisResult(int position, int totalDrawsAnalyzed, double averageGamesOut,
                                 double volatility, String primaryTrend, double trendStrength,
                                 String trendDuration, Map<Integer, Integer> numberFrequency,
                                 List<Integer> hotNumbers, List<Integer> coldNumbers,
                                 List<Integer> overdueNumbers, List<Double> supportLevels,
                                 List<Double> resistanceLevels, Map<String, Double> currentMovingAverages,
                                 String movingAverageTrend, List<String> insights,
                                 List<Integer> recommendedNumbers, double confidenceScore) {
        this.position = position;
        this.totalDrawsAnalyzed = totalDrawsAnalyzed;
        this.averageGamesOut = averageGamesOut;
        this.volatility = volatility;
        this.primaryTrend = primaryTrend;
        this.trendStrength = trendStrength;
        this.trendDuration = trendDuration;
        this.numberFrequency = numberFrequency;
        this.hotNumbers = hotNumbers;
        this.coldNumbers = coldNumbers;
        this.overdueNumbers = overdueNumbers;
        this.supportLevels = supportLevels;
        this.resistanceLevels = resistanceLevels;
        this.currentMovingAverages = currentMovingAverages;
        this.movingAverageTrend = movingAverageTrend;
        this.insights = insights;
        this.recommendedNumbers = recommendedNumbers;
        this.confidenceScore = confidenceScore;
    }
    
    // Getters
    public int getPosition() {
        return position;
    }
    
    public int getTotalDrawsAnalyzed() {
        return totalDrawsAnalyzed;
    }
    
    public double getAverageGamesOut() {
        return averageGamesOut;
    }
    
    public double getVolatility() {
        return volatility;
    }
    
    public String getPrimaryTrend() {
        return primaryTrend;
    }
    
    public double getTrendStrength() {
        return trendStrength;
    }
    
    public String getTrendDuration() {
        return trendDuration;
    }
    
    public Map<Integer, Integer> getNumberFrequency() {
        return numberFrequency;
    }
    
    public List<Integer> getHotNumbers() {
        return hotNumbers;
    }
    
    public List<Integer> getColdNumbers() {
        return coldNumbers;
    }
    
    public List<Integer> getOverdueNumbers() {
        return overdueNumbers;
    }
    
    public List<Double> getSupportLevels() {
        return supportLevels;
    }
    
    public List<Double> getResistanceLevels() {
        return resistanceLevels;
    }
    
    public Map<String, Double> getCurrentMovingAverages() {
        return currentMovingAverages;
    }
    
    public String getMovingAverageTrend() {
        return movingAverageTrend;
    }
    
    public List<String> getInsights() {
        return insights;
    }
    
    public List<Integer> getRecommendedNumbers() {
        return recommendedNumbers;
    }
    
    public double getConfidenceScore() {
        return confidenceScore;
    }
    
    // Utility methods
    public boolean isTrendStrong() {
        return trendStrength > 0.7;
    }
    
    public boolean isVolatile() {
        return volatility > 0.5;
    }
    
    public boolean hasReliableData() {
        return totalDrawsAnalyzed >= 20 && confidenceScore > 0.5;
    }
    
    public String getTrendDescription() {
        return primaryTrend + " (" + String.format("%.1f", trendStrength * 100) + "% strength, " + trendDuration.toLowerCase() + " term)";
    }
    
    @Override
    public String toString() {
        return "PositionAnalysisResult{" +
                "position=" + position +
                ", totalDraws=" + totalDrawsAnalyzed +
                ", avgGamesOut=" + String.format("%.2f", averageGamesOut) +
                ", trend=" + getTrendDescription() +
                ", volatility=" + String.format("%.2f", volatility) +
                ", confidence=" + String.format("%.1f%%", confidenceScore * 100) +
                '}';
    }
}