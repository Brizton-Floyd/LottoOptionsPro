package com.example.lottooptionspro.model.deltapick;

import java.util.List;
import java.util.Map;

/**
 * Request payload for delta pick generation API.
 * Either rawDeltas OR sortedDeltaMagnitudes should be populated based on deltaInputMode.
 */
public class DeltaPickGenerationRequest {
    private String lotteryState;
    private String lotteryGame;
    private String deltaInputMode;
    private Integer numCombinations;
    private Integer maxNumber;
    private Integer numPicks;
    private Map<String, List<Integer>> rawDeltas;
    private Map<String, List<Integer>> sortedDeltaMagnitudes;
    
    // Recent performance analysis options
    private Boolean includeRecentPerformance = false;
    private Integer lookbackDays = 7;

    public DeltaPickGenerationRequest() {
    }

    public String getLotteryState() {
        return lotteryState;
    }

    public void setLotteryState(String lotteryState) {
        this.lotteryState = lotteryState;
    }

    public String getLotteryGame() {
        return lotteryGame;
    }

    public void setLotteryGame(String lotteryGame) {
        this.lotteryGame = lotteryGame;
    }

    public String getDeltaInputMode() {
        return deltaInputMode;
    }

    public void setDeltaInputMode(String deltaInputMode) {
        this.deltaInputMode = deltaInputMode;
    }

    public Integer getNumCombinations() {
        return numCombinations;
    }

    public void setNumCombinations(Integer numCombinations) {
        this.numCombinations = numCombinations;
    }

    public Integer getMaxNumber() {
        return maxNumber;
    }

    public void setMaxNumber(Integer maxNumber) {
        this.maxNumber = maxNumber;
    }

    public Integer getNumPicks() {
        return numPicks;
    }

    public void setNumPicks(Integer numPicks) {
        this.numPicks = numPicks;
    }

    public Map<String, List<Integer>> getRawDeltas() {
        return rawDeltas;
    }

    public void setRawDeltas(Map<String, List<Integer>> rawDeltas) {
        this.rawDeltas = rawDeltas;
    }

    public Map<String, List<Integer>> getSortedDeltaMagnitudes() {
        return sortedDeltaMagnitudes;
    }

    public void setSortedDeltaMagnitudes(Map<String, List<Integer>> sortedDeltaMagnitudes) {
        this.sortedDeltaMagnitudes = sortedDeltaMagnitudes;
    }

    public Boolean getIncludeRecentPerformance() {
        return includeRecentPerformance;
    }

    public void setIncludeRecentPerformance(Boolean includeRecentPerformance) {
        this.includeRecentPerformance = includeRecentPerformance;
    }

    public Integer getLookbackDays() {
        return lookbackDays;
    }

    public void setLookbackDays(Integer lookbackDays) {
        this.lookbackDays = lookbackDays;
    }
}
