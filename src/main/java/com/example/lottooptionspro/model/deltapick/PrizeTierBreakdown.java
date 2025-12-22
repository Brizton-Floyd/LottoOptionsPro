package com.example.lottooptionspro.model.deltapick;

/**
 * Breakdown of wins and estimated value for a specific prize tier.
 * Matches the Phase 4 API structure.
 */
public class PrizeTierBreakdown {
    private Integer wins;
    private Double frequency;
    private Double estimatedValue;

    public PrizeTierBreakdown() {
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

    public Double getEstimatedValue() {
        return estimatedValue;
    }

    public void setEstimatedValue(Double estimatedValue) {
        this.estimatedValue = estimatedValue;
    }
}
