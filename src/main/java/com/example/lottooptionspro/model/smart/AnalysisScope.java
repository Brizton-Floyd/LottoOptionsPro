package com.example.lottooptionspro.model.smart;

public class AnalysisScope {
    private int ticketsAnalyzed;
    private int totalTickets;
    private int historicalDraws;
    private DateRange dateRange;
    private double yearsSpanned;
    private String samplingMethod;

    public int getTicketsAnalyzed() {
        return ticketsAnalyzed;
    }

    public void setTicketsAnalyzed(int ticketsAnalyzed) {
        this.ticketsAnalyzed = ticketsAnalyzed;
    }

    public int getTotalTickets() {
        return totalTickets;
    }

    public void setTotalTickets(int totalTickets) {
        this.totalTickets = totalTickets;
    }

    public int getHistoricalDraws() {
        return historicalDraws;
    }

    public void setHistoricalDraws(int historicalDraws) {
        this.historicalDraws = historicalDraws;
    }

    public DateRange getDateRange() {
        return dateRange;
    }

    public void setDateRange(DateRange dateRange) {
        this.dateRange = dateRange;
    }

    public double getYearsSpanned() {
        return yearsSpanned;
    }

    public void setYearsSpanned(double yearsSpanned) {
        this.yearsSpanned = yearsSpanned;
    }

    public String getSamplingMethod() {
        return samplingMethod;
    }

    public void setSamplingMethod(String samplingMethod) {
        this.samplingMethod = samplingMethod;
    }
}