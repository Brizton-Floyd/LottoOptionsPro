package com.example.lottooptionspro.models;

public class LotteryGame {
    private String fullName;

    // Getters and setters
    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    // Constructor
    public LotteryGame(String fullName, String description) {
        this.fullName = fullName;
    }
}
