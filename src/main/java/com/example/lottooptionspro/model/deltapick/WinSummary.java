package com.example.lottooptionspro.model.deltapick;

/**
 * Summary of wins from historical analysis.
 */
public class WinSummary {
    private Integer jackpotWins;
    private Integer totalWins;
    private Integer totalWinsEstimate;

    public WinSummary() {
    }

    public Integer getJackpotWins() {
        return jackpotWins;
    }

    public void setJackpotWins(Integer jackpotWins) {
        this.jackpotWins = jackpotWins;
    }

    public Integer getTotalWins() {
        return totalWins;
    }

    public void setTotalWins(Integer totalWins) {
        this.totalWins = totalWins;
    }

    public Integer getTotalWinsEstimate() {
        return totalWinsEstimate;
    }

    public void setTotalWinsEstimate(Integer totalWinsEstimate) {
        this.totalWinsEstimate = totalWinsEstimate;
    }
}
