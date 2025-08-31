package com.example.lottooptionspro.model.smart;

public class DroughtTierInfo {
    private String internalTier;
    private String friendlyTier;
    private boolean inDrought;
    private int daysSinceLastWin;
    private double severity;
    private String severityLevel;
    private String recommendation;
    private Double expectedDays;
    private Double actualVsExpected;

    public String getInternalTier() {
        return internalTier;
    }

    public void setInternalTier(String internalTier) {
        this.internalTier = internalTier;
    }

    public String getFriendlyTier() {
        return friendlyTier;
    }

    public void setFriendlyTier(String friendlyTier) {
        this.friendlyTier = friendlyTier;
    }

    public boolean isInDrought() {
        return inDrought;
    }

    public void setInDrought(boolean inDrought) {
        this.inDrought = inDrought;
    }

    public int getDaysSinceLastWin() {
        return daysSinceLastWin;
    }

    public void setDaysSinceLastWin(int daysSinceLastWin) {
        this.daysSinceLastWin = daysSinceLastWin;
    }

    public double getSeverity() {
        return severity;
    }

    public void setSeverity(double severity) {
        this.severity = severity;
    }

    public String getSeverityLevel() {
        return severityLevel;
    }

    public void setSeverityLevel(String severityLevel) {
        this.severityLevel = severityLevel;
    }

    public String getRecommendation() {
        return recommendation;
    }

    public void setRecommendation(String recommendation) {
        this.recommendation = recommendation;
    }

    public Double getExpectedDays() {
        return expectedDays;
    }

    public void setExpectedDays(Double expectedDays) {
        this.expectedDays = expectedDays;
    }

    public Double getActualVsExpected() {
        return actualVsExpected;
    }

    public void setActualVsExpected(Double actualVsExpected) {
        this.actualVsExpected = actualVsExpected;
    }
}