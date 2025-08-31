package com.example.lottooptionspro.model.smart;

public class GenerationProgress {
    private String sessionId;
    private String status;
    private int progress;
    private String message;
    private String currentPhase;
    private int ticketsGenerated;
    private int totalTickets;
    private QualityMetrics currentQuality;
    private double elapsedTimeSeconds;
    private String estimatedTimeRemaining;
    private boolean canCancel;

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public int getProgress() {
        return progress;
    }

    public void setProgress(int progress) {
        this.progress = progress;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getCurrentPhase() {
        return currentPhase;
    }

    public void setCurrentPhase(String currentPhase) {
        this.currentPhase = currentPhase;
    }

    public int getTicketsGenerated() {
        return ticketsGenerated;
    }

    public void setTicketsGenerated(int ticketsGenerated) {
        this.ticketsGenerated = ticketsGenerated;
    }

    public int getTotalTickets() {
        return totalTickets;
    }

    public void setTotalTickets(int totalTickets) {
        this.totalTickets = totalTickets;
    }

    public QualityMetrics getCurrentQuality() {
        return currentQuality;
    }

    public void setCurrentQuality(QualityMetrics currentQuality) {
        this.currentQuality = currentQuality;
    }

    public double getElapsedTimeSeconds() {
        return elapsedTimeSeconds;
    }

    public void setElapsedTimeSeconds(double elapsedTimeSeconds) {
        this.elapsedTimeSeconds = elapsedTimeSeconds;
    }

    public String getEstimatedTimeRemaining() {
        return estimatedTimeRemaining;
    }

    public void setEstimatedTimeRemaining(String estimatedTimeRemaining) {
        this.estimatedTimeRemaining = estimatedTimeRemaining;
    }

    public boolean isCanCancel() {
        return canCancel;
    }

    public void setCanCancel(boolean canCancel) {
        this.canCancel = canCancel;
    }
}