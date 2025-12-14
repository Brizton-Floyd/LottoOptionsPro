package com.example.lottooptionspro.model.deltapick;

/**
 * Configuration details for the delta pick generation request.
 */
public class Configuration {
    private String deltaInputMode;
    private Integer maxNumber;
    private Integer numPicks;
    private Integer requestedCombinations;
    private String lotteryState;
    private String lotteryGame;

    public Configuration() {
    }

    public String getDeltaInputMode() {
        return deltaInputMode;
    }

    public void setDeltaInputMode(String deltaInputMode) {
        this.deltaInputMode = deltaInputMode;
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

    public Integer getRequestedCombinations() {
        return requestedCombinations;
    }

    public void setRequestedCombinations(Integer requestedCombinations) {
        this.requestedCombinations = requestedCombinations;
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
}
