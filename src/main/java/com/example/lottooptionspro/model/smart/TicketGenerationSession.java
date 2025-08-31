package com.example.lottooptionspro.model.smart;

public class TicketGenerationSession {
    private String sessionId;
    private String status;
    private String message;
    private int progress;
    private int requestedTickets;
    private String lotteryConfigId;
    private String startTime;
    private String currentPhase;
    private QualityMetrics currentQualityMetrics;
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

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public int getProgress() {
        return progress;
    }

    public void setProgress(int progress) {
        this.progress = progress;
    }

    public int getRequestedTickets() {
        return requestedTickets;
    }

    public void setRequestedTickets(int requestedTickets) {
        this.requestedTickets = requestedTickets;
    }

    public String getLotteryConfigId() {
        return lotteryConfigId;
    }

    public void setLotteryConfigId(String lotteryConfigId) {
        this.lotteryConfigId = lotteryConfigId;
    }

    public String getStartTime() {
        return startTime;
    }

    public void setStartTime(String startTime) {
        this.startTime = startTime;
    }

    public String getCurrentPhase() {
        return currentPhase;
    }

    public void setCurrentPhase(String currentPhase) {
        this.currentPhase = currentPhase;
    }

    public QualityMetrics getCurrentQualityMetrics() {
        return currentQualityMetrics;
    }

    public void setCurrentQualityMetrics(QualityMetrics currentQualityMetrics) {
        this.currentQualityMetrics = currentQualityMetrics;
    }

    public boolean isCanCancel() {
        return canCancel;
    }

    public void setCanCancel(boolean canCancel) {
        this.canCancel = canCancel;
    }
}