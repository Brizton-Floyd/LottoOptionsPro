package com.example.lottooptionspro.models;

public class LotteryGame {
    private String name;
    private String description;

    // Getters and setters
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    // Constructor
    public LotteryGame(String name, String description) {
        this.name = name;
        this.description = description;
    }
}
