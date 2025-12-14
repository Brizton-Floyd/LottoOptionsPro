package com.example.lottooptionspro.model.deltapick;

/**
 * Represents prize information for a specific match level.
 */
public class PrizeMatch {
    private Integer wins;
    private Double frequency;

    public PrizeMatch() {
    }

    public Integer getWins() {
        return wins;
    }

    public void setWins(Integer wins) {
        this.wins = wins;
    }

    public Double getFrequency() {
        return frequency;
    }

    public void setFrequency(Double frequency) {
        this.frequency = frequency;
    }
}
