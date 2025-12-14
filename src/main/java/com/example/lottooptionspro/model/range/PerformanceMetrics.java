package com.example.lottooptionspro.model.range;

public class PerformanceMetrics {
    private String range;
    private int totalHits;
    private double hitFrequency;
    private double avgGapBetweenHits;
    private String currentStreak;
    private String lastHitDraw;
    private String status;
    private int daysSinceLastHit;
    private double expectedProbability;
    private double gapVariance;

    public PerformanceMetrics() {
    }

    public PerformanceMetrics(String range, int totalHits, double hitFrequency, double avgGapBetweenHits,
                             String currentStreak, String lastHitDraw, String status, int daysSinceLastHit,
                             double expectedProbability, double gapVariance) {
        this.range = range;
        this.totalHits = totalHits;
        this.hitFrequency = hitFrequency;
        this.avgGapBetweenHits = avgGapBetweenHits;
        this.currentStreak = currentStreak;
        this.lastHitDraw = lastHitDraw;
        this.status = status;
        this.daysSinceLastHit = daysSinceLastHit;
        this.expectedProbability = expectedProbability;
        this.gapVariance = gapVariance;
    }

    public String getRange() {
        return range;
    }

    public void setRange(String range) {
        this.range = range;
    }

    public int getTotalHits() {
        return totalHits;
    }

    public void setTotalHits(int totalHits) {
        this.totalHits = totalHits;
    }

    public double getHitFrequency() {
        return hitFrequency;
    }

    public void setHitFrequency(double hitFrequency) {
        this.hitFrequency = hitFrequency;
    }

    public double getAvgGapBetweenHits() {
        return avgGapBetweenHits;
    }

    public void setAvgGapBetweenHits(double avgGapBetweenHits) {
        this.avgGapBetweenHits = avgGapBetweenHits;
    }

    public String getCurrentStreak() {
        return currentStreak;
    }

    public void setCurrentStreak(String currentStreak) {
        this.currentStreak = currentStreak;
    }

    public String getLastHitDraw() {
        return lastHitDraw;
    }

    public void setLastHitDraw(String lastHitDraw) {
        this.lastHitDraw = lastHitDraw;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public int getDaysSinceLastHit() {
        return daysSinceLastHit;
    }

    public void setDaysSinceLastHit(int daysSinceLastHit) {
        this.daysSinceLastHit = daysSinceLastHit;
    }

    public double getExpectedProbability() {
        return expectedProbability;
    }

    public void setExpectedProbability(double expectedProbability) {
        this.expectedProbability = expectedProbability;
    }

    public double getGapVariance() {
        return gapVariance;
    }

    public void setGapVariance(double gapVariance) {
        this.gapVariance = gapVariance;
    }

    public boolean isHot() {
        return "HOT".equals(status);
    }

    public boolean isCold() {
        return "COLD".equals(status);
    }

    public boolean isNormal() {
        return "NORMAL".equals(status) || (!isHot() && !isCold());
    }

    public String getStatusDisplayName() {
        if (currentStreak != null) {
            return status + " (" + currentStreak.replace("_", " ") + ")";
        }
        return status;
    }
}