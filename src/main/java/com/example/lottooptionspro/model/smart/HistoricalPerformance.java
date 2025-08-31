package com.example.lottooptionspro.model.smart;

import java.util.List;
import java.util.Map;

public class HistoricalPerformance {
    private String analysisType;
    private AnalysisScope analysisScope;
    private WinSummary winSummary;
    private Map<String, PrizeBreakdown.PrizeTier> prizeBreakdown;
    private Object bestPerformingTicket; // Can be null in the sample data
    private PerformanceComparison comparison;
    private List<String> insights;
    private boolean fullAnalysisAvailable;
    private String fullAnalysisEndpoint;

    public String getAnalysisType() {
        return analysisType;
    }

    public void setAnalysisType(String analysisType) {
        this.analysisType = analysisType;
    }

    public AnalysisScope getAnalysisScope() {
        return analysisScope;
    }

    public void setAnalysisScope(AnalysisScope analysisScope) {
        this.analysisScope = analysisScope;
    }

    public WinSummary getWinSummary() {
        return winSummary;
    }

    public void setWinSummary(WinSummary winSummary) {
        this.winSummary = winSummary;
    }

    public Map<String, PrizeBreakdown.PrizeTier> getPrizeBreakdown() {
        return prizeBreakdown;
    }

    public void setPrizeBreakdown(Map<String, PrizeBreakdown.PrizeTier> prizeBreakdown) {
        this.prizeBreakdown = prizeBreakdown;
    }

    public Object getBestPerformingTicket() {
        return bestPerformingTicket;
    }

    public void setBestPerformingTicket(Object bestPerformingTicket) {
        this.bestPerformingTicket = bestPerformingTicket;
    }

    public PerformanceComparison getComparison() {
        return comparison;
    }

    public void setComparison(PerformanceComparison comparison) {
        this.comparison = comparison;
    }

    public List<String> getInsights() {
        return insights;
    }

    public void setInsights(List<String> insights) {
        this.insights = insights;
    }

    public boolean isFullAnalysisAvailable() {
        return fullAnalysisAvailable;
    }

    public void setFullAnalysisAvailable(boolean fullAnalysisAvailable) {
        this.fullAnalysisAvailable = fullAnalysisAvailable;
    }

    public String getFullAnalysisEndpoint() {
        return fullAnalysisEndpoint;
    }

    public void setFullAnalysisEndpoint(String fullAnalysisEndpoint) {
        this.fullAnalysisEndpoint = fullAnalysisEndpoint;
    }
}