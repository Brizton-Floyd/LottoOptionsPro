package com.example.lottooptionspro.model.deltapick;

import java.util.List;
import java.util.Map;

/**
 * Performance analysis of generated picks against recent draw results.
 * Includes prize tier breakdown and estimated winnings (Phase 4).
 */
public class RecentPerformance {
    private Integer drawsAnalyzed;
    private String dateRangeStart;
    private String dateRangeEnd;
    private List<RecentDrawResult> recentDraws;
    private Map<Integer, PickMatchSummary> pickPerformance;
    private RecentPerformanceSummary summary;
    
    // Phase 4: Prize tier breakdown at top level
    private Map<String, PrizeTierBreakdown> prizeTierBreakdown;

    public RecentPerformance() {
    }

    public Integer getDrawsAnalyzed() {
        return drawsAnalyzed;
    }

    public void setDrawsAnalyzed(Integer drawsAnalyzed) {
        this.drawsAnalyzed = drawsAnalyzed;
    }

    public String getDateRangeStart() {
        return dateRangeStart;
    }

    public void setDateRangeStart(String dateRangeStart) {
        this.dateRangeStart = dateRangeStart;
    }

    public String getDateRangeEnd() {
        return dateRangeEnd;
    }

    public void setDateRangeEnd(String dateRangeEnd) {
        this.dateRangeEnd = dateRangeEnd;
    }

    public List<RecentDrawResult> getRecentDraws() {
        return recentDraws;
    }

    public void setRecentDraws(List<RecentDrawResult> recentDraws) {
        this.recentDraws = recentDraws;
    }

    public Map<Integer, PickMatchSummary> getPickPerformance() {
        return pickPerformance;
    }

    public void setPickPerformance(Map<Integer, PickMatchSummary> pickPerformance) {
        this.pickPerformance = pickPerformance;
    }

    public RecentPerformanceSummary getSummary() {
        return summary;
    }

    public void setSummary(RecentPerformanceSummary summary) {
        this.summary = summary;
    }

    public Map<String, PrizeTierBreakdown> getPrizeTierBreakdown() {
        return prizeTierBreakdown;
    }

    public void setPrizeTierBreakdown(Map<String, PrizeTierBreakdown> prizeTierBreakdown) {
        this.prizeTierBreakdown = prizeTierBreakdown;
    }
}
