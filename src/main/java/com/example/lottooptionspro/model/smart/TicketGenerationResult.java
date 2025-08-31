package com.example.lottooptionspro.model.smart;

import java.util.List;

public class TicketGenerationResult {
    private String sessionId;
    private List<List<Integer>> tickets;
    private QualityMetrics qualityMetrics;
    private GenerationSummary generationSummary;
    private String lotteryConfigId;
    private String generatedAt;
    private String finalStatus;
    private boolean successful;
    private int ticketCount;
    private String displaySummary;
    private HistoricalPerformance historicalPerformance;
    private DroughtInformation droughtInformation;
    private boolean meetsQualityCriteria;
    private String fullAnalysisEndpoint;

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public List<List<Integer>> getTickets() {
        return tickets;
    }

    public void setTickets(List<List<Integer>> tickets) {
        this.tickets = tickets;
    }

    public QualityMetrics getQualityMetrics() {
        return qualityMetrics;
    }

    public void setQualityMetrics(QualityMetrics qualityMetrics) {
        this.qualityMetrics = qualityMetrics;
    }

    public GenerationSummary getGenerationSummary() {
        return generationSummary;
    }

    public void setGenerationSummary(GenerationSummary generationSummary) {
        this.generationSummary = generationSummary;
    }

    public String getLotteryConfigId() {
        return lotteryConfigId;
    }

    public void setLotteryConfigId(String lotteryConfigId) {
        this.lotteryConfigId = lotteryConfigId;
    }

    public String getGeneratedAt() {
        return generatedAt;
    }

    public void setGeneratedAt(String generatedAt) {
        this.generatedAt = generatedAt;
    }

    public String getFinalStatus() {
        return finalStatus;
    }

    public void setFinalStatus(String finalStatus) {
        this.finalStatus = finalStatus;
    }

    public boolean isSuccessful() {
        return successful;
    }

    public void setSuccessful(boolean successful) {
        this.successful = successful;
    }

    public int getTicketCount() {
        return ticketCount;
    }

    public void setTicketCount(int ticketCount) {
        this.ticketCount = ticketCount;
    }

    public String getDisplaySummary() {
        return displaySummary;
    }

    public void setDisplaySummary(String displaySummary) {
        this.displaySummary = displaySummary;
    }

    public int[][] getTicketsAsIntArrays() {
        if (tickets == null) return new int[0][];
        
        return tickets.stream()
                .map(ticket -> ticket.stream().mapToInt(Integer::intValue).toArray())
                .toArray(int[][]::new);
    }

    public HistoricalPerformance getHistoricalPerformance() {
        return historicalPerformance;
    }

    public void setHistoricalPerformance(HistoricalPerformance historicalPerformance) {
        this.historicalPerformance = historicalPerformance;
    }

    public DroughtInformation getDroughtInformation() {
        return droughtInformation;
    }

    public void setDroughtInformation(DroughtInformation droughtInformation) {
        this.droughtInformation = droughtInformation;
    }

    public boolean isMeetsQualityCriteria() {
        return meetsQualityCriteria;
    }

    public void setMeetsQualityCriteria(boolean meetsQualityCriteria) {
        this.meetsQualityCriteria = meetsQualityCriteria;
    }

    public String getFullAnalysisEndpoint() {
        return fullAnalysisEndpoint;
    }

    public void setFullAnalysisEndpoint(String fullAnalysisEndpoint) {
        this.fullAnalysisEndpoint = fullAnalysisEndpoint;
    }
}