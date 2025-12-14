package com.example.lottooptionspro.model.deltapick;

/**
 * Scope of the historical performance analysis.
 */
public class AnalysisScope {
    private Integer ticketsAnalyzed;
    private Integer totalTickets;
    private Integer historicalDraws;
    private DateRange dateRange;
    private Double yearsSpanned;
    private String samplingMethod;

    public AnalysisScope() {
    }

    public Integer getTicketsAnalyzed() {
        return ticketsAnalyzed;
    }

    public void setTicketsAnalyzed(Integer ticketsAnalyzed) {
        this.ticketsAnalyzed = ticketsAnalyzed;
    }

    public Integer getTotalTickets() {
        return totalTickets;
    }

    public void setTotalTickets(Integer totalTickets) {
        this.totalTickets = totalTickets;
    }

    public Integer getHistoricalDraws() {
        return historicalDraws;
    }

    public void setHistoricalDraws(Integer historicalDraws) {
        this.historicalDraws = historicalDraws;
    }

    public DateRange getDateRange() {
        return dateRange;
    }

    public void setDateRange(DateRange dateRange) {
        this.dateRange = dateRange;
    }

    public Double getYearsSpanned() {
        return yearsSpanned;
    }

    public void setYearsSpanned(Double yearsSpanned) {
        this.yearsSpanned = yearsSpanned;
    }

    public String getSamplingMethod() {
        return samplingMethod;
    }

    public void setSamplingMethod(String samplingMethod) {
        this.samplingMethod = samplingMethod;
    }
}
