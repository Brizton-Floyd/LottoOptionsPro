package com.example.lottooptionspro.model.range;

import java.util.List;
import java.util.Map;

public class PositionAnalysis {
    private int position;
    private List<String> mostLikelyRanges;
    private List<String> leastLikelyRanges;
    private Map<String, Double> rangeFrequencies;
    private double averageValue;
    private double standardDeviation;
    private Integer lastDrawnNumber;
    private String currentPositionStreak;

    public PositionAnalysis() {
    }

    public PositionAnalysis(int position, List<String> mostLikelyRanges, List<String> leastLikelyRanges,
                           Map<String, Double> rangeFrequencies, double averageValue, double standardDeviation,
                           Integer lastDrawnNumber, String currentPositionStreak) {
        this.position = position;
        this.mostLikelyRanges = mostLikelyRanges;
        this.leastLikelyRanges = leastLikelyRanges;
        this.rangeFrequencies = rangeFrequencies;
        this.averageValue = averageValue;
        this.standardDeviation = standardDeviation;
        this.lastDrawnNumber = lastDrawnNumber;
        this.currentPositionStreak = currentPositionStreak;
    }

    public int getPosition() {
        return position;
    }

    public void setPosition(int position) {
        this.position = position;
    }

    public List<String> getMostLikelyRanges() {
        return mostLikelyRanges;
    }

    public void setMostLikelyRanges(List<String> mostLikelyRanges) {
        this.mostLikelyRanges = mostLikelyRanges;
    }

    public List<String> getLeastLikelyRanges() {
        return leastLikelyRanges;
    }

    public void setLeastLikelyRanges(List<String> leastLikelyRanges) {
        this.leastLikelyRanges = leastLikelyRanges;
    }

    public Map<String, Double> getRangeFrequencies() {
        return rangeFrequencies;
    }

    public void setRangeFrequencies(Map<String, Double> rangeFrequencies) {
        this.rangeFrequencies = rangeFrequencies;
    }

    public double getAverageValue() {
        return averageValue;
    }

    public void setAverageValue(double averageValue) {
        this.averageValue = averageValue;
    }

    public double getStandardDeviation() {
        return standardDeviation;
    }

    public void setStandardDeviation(double standardDeviation) {
        this.standardDeviation = standardDeviation;
    }

    public Integer getLastDrawnNumber() {
        return lastDrawnNumber;
    }

    public void setLastDrawnNumber(Integer lastDrawnNumber) {
        this.lastDrawnNumber = lastDrawnNumber;
    }

    public String getCurrentPositionStreak() {
        return currentPositionStreak;
    }

    public void setCurrentPositionStreak(String currentPositionStreak) {
        this.currentPositionStreak = currentPositionStreak;
    }

    public String getPositionDisplayName() {
        return "Position " + position;
    }

    public String getStreakDisplayName() {
        if (currentPositionStreak != null) {
            return currentPositionStreak.replace("_", " ");
        }
        return "No streak";
    }

    public double getFrequencyForRange(String range) {
        return rangeFrequencies != null ? rangeFrequencies.getOrDefault(range, 0.0) : 0.0;
    }
}