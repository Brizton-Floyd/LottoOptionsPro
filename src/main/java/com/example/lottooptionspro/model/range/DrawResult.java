package com.example.lottooptionspro.model.range;

import java.util.List;
import java.util.Map;

public class DrawResult {
    private String drawDate;
    private Map<String, Integer> rangeHits;
    private int totalHits;
    private List<Integer> analyzedNumbers;

    public DrawResult() {
    }

    public DrawResult(String drawDate, Map<String, Integer> rangeHits, int totalHits, List<Integer> analyzedNumbers) {
        this.drawDate = drawDate;
        this.rangeHits = rangeHits;
        this.totalHits = totalHits;
        this.analyzedNumbers = analyzedNumbers;
    }

    public String getDrawDate() {
        return drawDate;
    }

    public void setDrawDate(String drawDate) {
        this.drawDate = drawDate;
    }

    public Map<String, Integer> getRangeHits() {
        return rangeHits;
    }

    public void setRangeHits(Map<String, Integer> rangeHits) {
        this.rangeHits = rangeHits;
    }

    public int getTotalHits() {
        return totalHits;
    }

    public void setTotalHits(int totalHits) {
        this.totalHits = totalHits;
    }

    public List<Integer> getAnalyzedNumbers() {
        return analyzedNumbers;
    }

    public void setAnalyzedNumbers(List<Integer> analyzedNumbers) {
        this.analyzedNumbers = analyzedNumbers;
    }
}