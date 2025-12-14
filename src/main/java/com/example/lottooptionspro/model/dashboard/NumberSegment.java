package com.example.lottooptionspro.model.dashboard;

import java.util.List;
import java.util.Map;

public class NumberSegment {
    private int startNumber;
    private int endNumber;
    private String segmentName;
    private Map<Integer, Integer> currentGamesOut;
    private List<Integer> recommendedGamesOut;
    private double averageGamesOut;
    private int totalHits;
    private String lastHitDate;
    private List<Integer> optimalHotNumbers;
    private List<Integer> optimalNormalNumbers;
    private int hotPredictionHits;
    private int hotPredictionAttempts;
    private int normalPredictionHits;
    private int normalPredictionAttempts;
    private double hotAccuracyPercentage;
    private double normalAccuracyPercentage;
    private int segmentHitPredictions;
    private int segmentHitAttempts;
    private double segmentHitAccuracy;
    private int expectedHitsFromSegment;
    private int actualHitsFromSegment;
    
    public NumberSegment() {}
    
    public NumberSegment(int startNumber, int endNumber) {
        this.startNumber = startNumber;
        this.endNumber = endNumber;
        this.segmentName = "Segment " + startNumber + "-" + endNumber;
    }
    
    public int getStartNumber() {
        return startNumber;
    }
    
    public void setStartNumber(int startNumber) {
        this.startNumber = startNumber;
    }
    
    public int getEndNumber() {
        return endNumber;
    }
    
    public void setEndNumber(int endNumber) {
        this.endNumber = endNumber;
    }
    
    public String getSegmentName() {
        return segmentName;
    }
    
    public void setSegmentName(String segmentName) {
        this.segmentName = segmentName;
    }
    
    public Map<Integer, Integer> getCurrentGamesOut() {
        return currentGamesOut;
    }
    
    public void setCurrentGamesOut(Map<Integer, Integer> currentGamesOut) {
        this.currentGamesOut = currentGamesOut;
    }
    
    public List<Integer> getRecommendedGamesOut() {
        return recommendedGamesOut;
    }
    
    public void setRecommendedGamesOut(List<Integer> recommendedGamesOut) {
        this.recommendedGamesOut = recommendedGamesOut;
    }
    
    public double getAverageGamesOut() {
        return averageGamesOut;
    }
    
    public void setAverageGamesOut(double averageGamesOut) {
        this.averageGamesOut = averageGamesOut;
    }
    
    public int getTotalHits() {
        return totalHits;
    }
    
    public void setTotalHits(int totalHits) {
        this.totalHits = totalHits;
    }
    
    public String getLastHitDate() {
        return lastHitDate;
    }
    
    public void setLastHitDate(String lastHitDate) {
        this.lastHitDate = lastHitDate;
    }
    
    public List<Integer> getOptimalHotNumbers() {
        return optimalHotNumbers;
    }
    
    public void setOptimalHotNumbers(List<Integer> optimalHotNumbers) {
        this.optimalHotNumbers = optimalHotNumbers;
    }
    
    public List<Integer> getOptimalNormalNumbers() {
        return optimalNormalNumbers;
    }
    
    public void setOptimalNormalNumbers(List<Integer> optimalNormalNumbers) {
        this.optimalNormalNumbers = optimalNormalNumbers;
    }
    
    public int getHotPredictionHits() {
        return hotPredictionHits;
    }
    
    public void setHotPredictionHits(int hotPredictionHits) {
        this.hotPredictionHits = hotPredictionHits;
    }
    
    public int getHotPredictionAttempts() {
        return hotPredictionAttempts;
    }
    
    public void setHotPredictionAttempts(int hotPredictionAttempts) {
        this.hotPredictionAttempts = hotPredictionAttempts;
    }
    
    public int getNormalPredictionHits() {
        return normalPredictionHits;
    }
    
    public void setNormalPredictionHits(int normalPredictionHits) {
        this.normalPredictionHits = normalPredictionHits;
    }
    
    public int getNormalPredictionAttempts() {
        return normalPredictionAttempts;
    }
    
    public void setNormalPredictionAttempts(int normalPredictionAttempts) {
        this.normalPredictionAttempts = normalPredictionAttempts;
    }
    
    public double getHotAccuracyPercentage() {
        return hotAccuracyPercentage;
    }
    
    public void setHotAccuracyPercentage(double hotAccuracyPercentage) {
        this.hotAccuracyPercentage = hotAccuracyPercentage;
    }
    
    public double getNormalAccuracyPercentage() {
        return normalAccuracyPercentage;
    }
    
    public void setNormalAccuracyPercentage(double normalAccuracyPercentage) {
        this.normalAccuracyPercentage = normalAccuracyPercentage;
    }
    
    public int getSegmentHitPredictions() {
        return segmentHitPredictions;
    }
    
    public void setSegmentHitPredictions(int segmentHitPredictions) {
        this.segmentHitPredictions = segmentHitPredictions;
    }
    
    public int getSegmentHitAttempts() {
        return segmentHitAttempts;
    }
    
    public void setSegmentHitAttempts(int segmentHitAttempts) {
        this.segmentHitAttempts = segmentHitAttempts;
    }
    
    public double getSegmentHitAccuracy() {
        return segmentHitAccuracy;
    }
    
    public void setSegmentHitAccuracy(double segmentHitAccuracy) {
        this.segmentHitAccuracy = segmentHitAccuracy;
    }
    
    public int getExpectedHitsFromSegment() {
        return expectedHitsFromSegment;
    }
    
    public void setExpectedHitsFromSegment(int expectedHitsFromSegment) {
        this.expectedHitsFromSegment = expectedHitsFromSegment;
    }
    
    public int getActualHitsFromSegment() {
        return actualHitsFromSegment;
    }
    
    public void setActualHitsFromSegment(int actualHitsFromSegment) {
        this.actualHitsFromSegment = actualHitsFromSegment;
    }
    
    public boolean containsNumber(int number) {
        return number >= startNumber && number <= endNumber;
    }
    
    public int getSegmentSize() {
        return endNumber - startNumber + 1;
    }
    
    @Override
    public String toString() {
        return "NumberSegment{" +
                "segmentName='" + segmentName + '\'' +
                ", startNumber=" + startNumber +
                ", endNumber=" + endNumber +
                ", averageGamesOut=" + averageGamesOut +
                ", totalHits=" + totalHits +
                '}';
    }
}