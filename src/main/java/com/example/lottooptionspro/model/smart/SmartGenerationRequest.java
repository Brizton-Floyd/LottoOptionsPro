package com.example.lottooptionspro.model.smart;

public class SmartGenerationRequest {
    private String userId;
    private String targetTier;
    private int numberOfTickets;
    private String generationStrategy;
    private double budget;
    private String lotteryConfigId;
    private String lotteryState;
    private String lotteryGame;
    private UserPreferences preferences;

    public SmartGenerationRequest() {
        this.generationStrategy = "PATTERN_BASED";
        this.preferences = new UserPreferences();
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getTargetTier() {
        return targetTier;
    }

    public void setTargetTier(String targetTier) {
        this.targetTier = targetTier;
    }

    public int getNumberOfTickets() {
        return numberOfTickets;
    }

    public void setNumberOfTickets(int numberOfTickets) {
        this.numberOfTickets = numberOfTickets;
    }

    public String getGenerationStrategy() {
        return generationStrategy;
    }

    public void setGenerationStrategy(String generationStrategy) {
        this.generationStrategy = generationStrategy;
    }

    public double getBudget() {
        return budget;
    }

    public void setBudget(double budget) {
        this.budget = budget;
    }

    public String getLotteryConfigId() {
        return lotteryConfigId;
    }

    public void setLotteryConfigId(String lotteryConfigId) {
        this.lotteryConfigId = lotteryConfigId;
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

    public UserPreferences getPreferences() {
        return preferences;
    }

    public void setPreferences(UserPreferences preferences) {
        this.preferences = preferences;
    }
}