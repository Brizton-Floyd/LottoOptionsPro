package com.example.lottooptionspro.model.deltapick;

import java.util.List;

/**
 * Represents a single recent draw result with match statistics.
 */
public class RecentDrawResult {
    private String drawDate;
    private List<Integer> winningNumbers;
    private Integer bestPickRank;
    private Integer bestPickMatches;
    private Integer totalMatches;

    public RecentDrawResult() {
    }

    public String getDrawDate() {
        return drawDate;
    }

    public void setDrawDate(String drawDate) {
        this.drawDate = drawDate;
    }

    public List<Integer> getWinningNumbers() {
        return winningNumbers;
    }

    public void setWinningNumbers(List<Integer> winningNumbers) {
        this.winningNumbers = winningNumbers;
    }

    public Integer getBestPickRank() {
        return bestPickRank;
    }

    public void setBestPickRank(Integer bestPickRank) {
        this.bestPickRank = bestPickRank;
    }

    public Integer getBestPickMatches() {
        return bestPickMatches;
    }

    public void setBestPickMatches(Integer bestPickMatches) {
        this.bestPickMatches = bestPickMatches;
    }

    public Integer getTotalMatches() {
        return totalMatches;
    }

    public void setTotalMatches(Integer totalMatches) {
        this.totalMatches = totalMatches;
    }
}
