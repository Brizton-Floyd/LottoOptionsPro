package com.example.lottooptionspro.model.smart;

public class TemplateDetail {
    private int lowOdd;
    private int lowEven;
    private int highOdd;
    private int highEven;
    private double probability;
    private double expectedFrequency;
    private int midpoint;
    private String templateGroup;
    private int hitCount;
    private int gamesOut;
    private double actualFrequency;
    private double successToFailureRatio;
    private boolean overdue;
    private String timingIndicator;
    private double performanceScore;
    private boolean recommendedForGeneration;
    private String probabilityPercentage;
    private double performanceDeviation;
    private String patternString;
    private int totalNumbers;
    private int usageCount;
    private double qualityContribution;

    // Default constructor
    public TemplateDetail() {
    }

    // Getters and Setters
    public int getLowOdd() {
        return lowOdd;
    }

    public void setLowOdd(int lowOdd) {
        this.lowOdd = lowOdd;
    }

    public int getLowEven() {
        return lowEven;
    }

    public void setLowEven(int lowEven) {
        this.lowEven = lowEven;
    }

    public int getHighOdd() {
        return highOdd;
    }

    public void setHighOdd(int highOdd) {
        this.highOdd = highOdd;
    }

    public int getHighEven() {
        return highEven;
    }

    public void setHighEven(int highEven) {
        this.highEven = highEven;
    }

    public double getProbability() {
        return probability;
    }

    public void setProbability(double probability) {
        this.probability = probability;
    }

    public double getExpectedFrequency() {
        return expectedFrequency;
    }

    public void setExpectedFrequency(double expectedFrequency) {
        this.expectedFrequency = expectedFrequency;
    }

    public int getMidpoint() {
        return midpoint;
    }

    public void setMidpoint(int midpoint) {
        this.midpoint = midpoint;
    }

    public String getTemplateGroup() {
        return templateGroup;
    }

    public void setTemplateGroup(String templateGroup) {
        this.templateGroup = templateGroup;
    }

    public int getHitCount() {
        return hitCount;
    }

    public void setHitCount(int hitCount) {
        this.hitCount = hitCount;
    }

    public int getGamesOut() {
        return gamesOut;
    }

    public void setGamesOut(int gamesOut) {
        this.gamesOut = gamesOut;
    }

    public double getActualFrequency() {
        return actualFrequency;
    }

    public void setActualFrequency(double actualFrequency) {
        this.actualFrequency = actualFrequency;
    }

    public double getSuccessToFailureRatio() {
        return successToFailureRatio;
    }

    public void setSuccessToFailureRatio(double successToFailureRatio) {
        this.successToFailureRatio = successToFailureRatio;
    }

    public boolean isOverdue() {
        return overdue;
    }

    public void setOverdue(boolean overdue) {
        this.overdue = overdue;
    }

    public String getTimingIndicator() {
        return timingIndicator;
    }

    public void setTimingIndicator(String timingIndicator) {
        this.timingIndicator = timingIndicator;
    }

    public double getPerformanceScore() {
        return performanceScore;
    }

    public void setPerformanceScore(double performanceScore) {
        this.performanceScore = performanceScore;
    }

    public boolean isRecommendedForGeneration() {
        return recommendedForGeneration;
    }

    public void setRecommendedForGeneration(boolean recommendedForGeneration) {
        this.recommendedForGeneration = recommendedForGeneration;
    }

    public String getProbabilityPercentage() {
        return probabilityPercentage;
    }

    public void setProbabilityPercentage(String probabilityPercentage) {
        this.probabilityPercentage = probabilityPercentage;
    }

    public double getPerformanceDeviation() {
        return performanceDeviation;
    }

    public void setPerformanceDeviation(double performanceDeviation) {
        this.performanceDeviation = performanceDeviation;
    }

    public String getPatternString() {
        return patternString;
    }

    public void setPatternString(String patternString) {
        this.patternString = patternString;
    }

    public int getTotalNumbers() {
        return totalNumbers;
    }

    public void setTotalNumbers(int totalNumbers) {
        this.totalNumbers = totalNumbers;
    }

    public int getUsageCount() {
        return usageCount;
    }

    public void setUsageCount(int usageCount) {
        this.usageCount = usageCount;
    }

    public double getQualityContribution() {
        return qualityContribution;
    }

    public void setQualityContribution(double qualityContribution) {
        this.qualityContribution = qualityContribution;
    }

    // Utility methods
    public boolean isTimingIndicator(String indicator) {
        return indicator != null && indicator.equals(this.timingIndicator);
    }

    public boolean isBestOrGoodGroup() {
        return "BEST".equals(templateGroup) || "GOOD".equals(templateGroup);
    }

    public String getFormattedProbability() {
        return String.format("%.2f%%", probability * 100);
    }

    public String getFormattedPerformanceScore() {
        return String.format("%.1f", performanceScore);
    }

    @Override
    public String toString() {
        return "TemplateDetail{" +
                "patternString='" + patternString + '\'' +
                ", templateGroup='" + templateGroup + '\'' +
                ", probability=" + probability +
                ", performanceScore=" + performanceScore +
                ", overdue=" + overdue +
                ", timingIndicator='" + timingIndicator + '\'' +
                '}';
    }
}