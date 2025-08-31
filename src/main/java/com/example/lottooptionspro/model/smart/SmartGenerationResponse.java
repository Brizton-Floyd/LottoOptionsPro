package com.example.lottooptionspro.model.smart;

public class SmartGenerationResponse {
    private String sessionId;
    private String status;
    private String message;
    private int requestedTickets;
    private String lotteryConfigId;

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
}