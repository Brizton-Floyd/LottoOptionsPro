package com.example.lottooptionspro.model.deltapick;

import java.util.Map;

/**
 * Overall summary of recent performance analysis.
 * Enhanced with prize tier breakdown and estimated winnings (Phase 4).
 */
public class RecentPerformanceSummary {
    private Integer bestPickRank;
    private Integer bestPickTotalMatches;
    private Integer worstPickRank;
    private Integer worstPickTotalMatches;
    private Double averageMatchesPerPick;
    private Double averageMatchesPerDraw;
    private Integer picksWithMatches;
    private Integer picksWithoutMatches;
    private String recommendation;
    
    // Phase 4: Prize tier analysis
    private Map<String, PrizeTierBreakdown> prizeTierBreakdown;
    private Integer totalWins;
    private Double totalEstimatedWinnings;
    private String bestPrizeTier;

    public RecentPerformanceSummary() {
    }

    public Integer getBestPickRank() {
        return bestPickRank;
    }

    public void setBestPickRank(Integer bestPickRank) {
        this.bestPickRank = bestPickRank;
    }

    public Integer getBestPickTotalMatches() {
        return bestPickTotalMatches;
    }

    public void setBestPickTotalMatches(Integer bestPickTotalMatches) {
        this.bestPickTotalMatches = bestPickTotalMatches;
    }

    public Integer getWorstPickRank() {
        return worstPickRank;
    }

    public void setWorstPickRank(Integer worstPickRank) {
        this.worstPickRank = worstPickRank;
    }

    public Integer getWorstPickTotalMatches() {
        return worstPickTotalMatches;
    }

    public void setWorstPickTotalMatches(Integer worstPickTotalMatches) {
        this.worstPickTotalMatches = worstPickTotalMatches;
    }

    public Double getAverageMatchesPerPick() {
        return averageMatchesPerPick;
    }

    public void setAverageMatchesPerPick(Double averageMatchesPerPick) {
        this.averageMatchesPerPick = averageMatchesPerPick;
    }

    public Double getAverageMatchesPerDraw() {
        return averageMatchesPerDraw;
    }

    public void setAverageMatchesPerDraw(Double averageMatchesPerDraw) {
        this.averageMatchesPerDraw = averageMatchesPerDraw;
    }

    public Integer getPicksWithMatches() {
        return picksWithMatches;
    }

    public void setPicksWithMatches(Integer picksWithMatches) {
        this.picksWithMatches = picksWithMatches;
    }

    public Integer getPicksWithoutMatches() {
        return picksWithoutMatches;
    }

    public void setPicksWithoutMatches(Integer picksWithoutMatches) {
        this.picksWithoutMatches = picksWithoutMatches;
    }

    public String getRecommendation() {
        return recommendation;
    }

    public void setRecommendation(String recommendation) {
        this.recommendation = recommendation;
    }

    public Map<String, PrizeTierBreakdown> getPrizeTierBreakdown() {
        return prizeTierBreakdown;
    }

    public void setPrizeTierBreakdown(Map<String, PrizeTierBreakdown> prizeTierBreakdown) {
        this.prizeTierBreakdown = prizeTierBreakdown;
    }

    public Integer getTotalWins() {
        return totalWins;
    }

    public void setTotalWins(Integer totalWins) {
        this.totalWins = totalWins;
    }

    public Double getTotalEstimatedWinnings() {
        return totalEstimatedWinnings;
    }

    public void setTotalEstimatedWinnings(Double totalEstimatedWinnings) {
        this.totalEstimatedWinnings = totalEstimatedWinnings;
    }

    public String getBestPrizeTier() {
        return bestPrizeTier;
    }

    public void setBestPrizeTier(String bestPrizeTier) {
        this.bestPrizeTier = bestPrizeTier;
    }
}
