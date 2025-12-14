package com.example.lottooptionspro.model.range;

import java.util.List;

public class RangeAnalysisRequest {
    private String lotteryState;
    private String lotteryGame;
    private int rangeSize;
    private AnalysisType analysisType;
    private int maxDraws;
    private boolean includePerformanceMetrics;
    private List<Integer> drawPositions;

    public RangeAnalysisRequest() {
        this.rangeSize = 10;
        this.analysisType = AnalysisType.ACTUAL;
        this.maxDraws = 50;
        this.includePerformanceMetrics = true;
    }

    public RangeAnalysisRequest(String lotteryState, String lotteryGame, int rangeSize, 
                               AnalysisType analysisType, int maxDraws, 
                               boolean includePerformanceMetrics, List<Integer> drawPositions) {
        this.lotteryState = lotteryState;
        this.lotteryGame = lotteryGame;
        this.rangeSize = rangeSize;
        this.analysisType = analysisType;
        this.maxDraws = maxDraws;
        this.includePerformanceMetrics = includePerformanceMetrics;
        this.drawPositions = drawPositions;
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

    public int getRangeSize() {
        return rangeSize;
    }

    public void setRangeSize(int rangeSize) {
        this.rangeSize = rangeSize;
    }

    public AnalysisType getAnalysisType() {
        return analysisType;
    }

    public void setAnalysisType(AnalysisType analysisType) {
        this.analysisType = analysisType;
    }

    public int getMaxDraws() {
        return maxDraws;
    }

    public void setMaxDraws(int maxDraws) {
        this.maxDraws = maxDraws;
    }

    public boolean isIncludePerformanceMetrics() {
        return includePerformanceMetrics;
    }

    public void setIncludePerformanceMetrics(boolean includePerformanceMetrics) {
        this.includePerformanceMetrics = includePerformanceMetrics;
    }

    public List<Integer> getDrawPositions() {
        return drawPositions;
    }

    public void setDrawPositions(List<Integer> drawPositions) {
        this.drawPositions = drawPositions;
    }
}