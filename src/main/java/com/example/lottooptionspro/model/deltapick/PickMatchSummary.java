package com.example.lottooptionspro.model.deltapick;

import java.util.List;
import java.util.Map;

/**
 * Summary of how a specific pick performed across recent draws.
 * Enhanced with prize tier wins and estimated winnings (Phase 4).
 */
public class PickMatchSummary {
    private Integer pickRank;
    private List<Integer> numbers;
    private Integer totalMatches;
    private Integer bestMatchCount;
    private String bestMatchDate;
    private Double averageMatchesPerDraw;
    private List<DrawMatch> drawMatches;
    
    // Phase 4: Prize tier analysis
    private Map<String, Integer> prizeTierWins;
    private Double estimatedWinnings;

    public PickMatchSummary() {
    }

    public Integer getPickRank() {
        return pickRank;
    }

    public void setPickRank(Integer pickRank) {
        this.pickRank = pickRank;
    }

    public List<Integer> getNumbers() {
        return numbers;
    }

    public void setNumbers(List<Integer> numbers) {
        this.numbers = numbers;
    }

    public Integer getTotalMatches() {
        return totalMatches;
    }

    public void setTotalMatches(Integer totalMatches) {
        this.totalMatches = totalMatches;
    }

    public Integer getBestMatchCount() {
        return bestMatchCount;
    }

    public void setBestMatchCount(Integer bestMatchCount) {
        this.bestMatchCount = bestMatchCount;
    }

    public String getBestMatchDate() {
        return bestMatchDate;
    }

    public void setBestMatchDate(String bestMatchDate) {
        this.bestMatchDate = bestMatchDate;
    }

    public Double getAverageMatchesPerDraw() {
        return averageMatchesPerDraw;
    }

    public void setAverageMatchesPerDraw(Double averageMatchesPerDraw) {
        this.averageMatchesPerDraw = averageMatchesPerDraw;
    }

    public List<DrawMatch> getDrawMatches() {
        return drawMatches;
    }

    public void setDrawMatches(List<DrawMatch> drawMatches) {
        this.drawMatches = drawMatches;
    }

    public Map<String, Integer> getPrizeTierWins() {
        return prizeTierWins;
    }

    public void setPrizeTierWins(Map<String, Integer> prizeTierWins) {
        this.prizeTierWins = prizeTierWins;
    }

    public Double getEstimatedWinnings() {
        return estimatedWinnings;
    }

    public void setEstimatedWinnings(Double estimatedWinnings) {
        this.estimatedWinnings = estimatedWinnings;
    }
}
