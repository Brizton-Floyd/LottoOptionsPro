package com.example.lottooptionspro.models;

import java.util.List;

public class LotteryState {
    private String stateRegion;
    private List<LotteryGame> stateLotteryGames;

    // Getters and setters
    public String getStateRegion() {
        return stateRegion;
    }

    public void setStateRegion(String stateRegion) {
        this.stateRegion = stateRegion;
    }

    public List<LotteryGame> getStateLotteryGames() {
        return stateLotteryGames;
    }

    public void setStateLotteryGames(List<LotteryGame> stateLotteryGames) {
        this.stateLotteryGames = stateLotteryGames;
    }

    // Constructor
    public LotteryState(String stateRegion, List<LotteryGame> stateLotteryGames) {
        this.stateRegion = stateRegion;
        this.stateLotteryGames = stateLotteryGames;
    }
}
