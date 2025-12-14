package com.example.lottooptionspro.model.deltapick;

import java.util.List;

/**
 * Historical performance analysis of the generated picks.
 */
public class HistoricalPerformance {
    private String analysisType;
    private AnalysisScope analysisScope;
    private WinSummary winSummary;
    private PrizeBreakdown prizeBreakdown;
    private Comparison comparison;
    private List<String> insights;
    private Boolean fullAnalysisAvailable;
    private String fullAnalysisEndpoint;

    public HistoricalPerformance() {
    }

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

    public PrizeBreakdown getPrizeBreakdown() {
        return prizeBreakdown;
    }

    public void setPrizeBreakdown(PrizeBreakdown prizeBreakdown) {
        this.prizeBreakdown = prizeBreakdown;
    }

    public Comparison getComparison() {
        return comparison;
    }

    public void setComparison(Comparison comparison) {
        this.comparison = comparison;
    }

    public List<String> getInsights() {
        return insights;
    }

    public void setInsights(List<String> insights) {
        this.insights = insights;
    }

    public Boolean getFullAnalysisAvailable() {
        return fullAnalysisAvailable;
    }

    public void setFullAnalysisAvailable(Boolean fullAnalysisAvailable) {
        this.fullAnalysisAvailable = fullAnalysisAvailable;
    }

    public String getFullAnalysisEndpoint() {
        return fullAnalysisEndpoint;
    }

    public void setFullAnalysisEndpoint(String fullAnalysisEndpoint) {
        this.fullAnalysisEndpoint = fullAnalysisEndpoint;
    }
}
