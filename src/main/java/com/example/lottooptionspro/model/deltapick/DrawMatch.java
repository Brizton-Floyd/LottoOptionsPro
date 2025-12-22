package com.example.lottooptionspro.model.deltapick;

import java.util.List;

/**
 * Details of matches for a specific draw.
 */
public class DrawMatch {
    private String drawDate;
    private Integer matchCount;
    private List<Integer> matchedNumbers;

    public DrawMatch() {
    }

    public String getDrawDate() {
        return drawDate;
    }

    public void setDrawDate(String drawDate) {
        this.drawDate = drawDate;
    }

    public Integer getMatchCount() {
        return matchCount;
    }

    public void setMatchCount(Integer matchCount) {
        this.matchCount = matchCount;
    }

    public List<Integer> getMatchedNumbers() {
        return matchedNumbers;
    }

    public void setMatchedNumbers(List<Integer> matchedNumbers) {
        this.matchedNumbers = matchedNumbers;
    }
}
