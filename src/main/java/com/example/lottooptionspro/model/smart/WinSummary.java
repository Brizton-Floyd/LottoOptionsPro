package com.example.lottooptionspro.model.smart;

public class WinSummary {
    private int jackpotWins;
    private int totalWins;
    private WinsEstimate totalWinsEstimate;

    public int getJackpotWins() {
        return jackpotWins;
    }

    public void setJackpotWins(int jackpotWins) {
        this.jackpotWins = jackpotWins;
    }

    public int getTotalWins() {
        return totalWins;
    }

    public void setTotalWins(int totalWins) {
        this.totalWins = totalWins;
    }

    public WinsEstimate getTotalWinsEstimate() {
        return totalWinsEstimate;
    }

    public void setTotalWinsEstimate(WinsEstimate totalWinsEstimate) {
        this.totalWinsEstimate = totalWinsEstimate;
    }

    public static class WinsEstimate {
        private int value;
        private int[] confidenceRange;
        private int confidence;

        public int getValue() {
            return value;
        }

        public void setValue(int value) {
            this.value = value;
        }

        public int[] getConfidenceRange() {
            return confidenceRange;
        }

        public void setConfidenceRange(int[] confidenceRange) {
            this.confidenceRange = confidenceRange;
        }

        public int getConfidence() {
            return confidence;
        }

        public void setConfidence(int confidence) {
            this.confidence = confidence;
        }
    }
}