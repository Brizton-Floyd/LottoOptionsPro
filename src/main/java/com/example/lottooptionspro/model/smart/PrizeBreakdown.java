package com.example.lottooptionspro.model.smart;

import java.util.Map;

public class PrizeBreakdown {
    private Map<String, PrizeTier> prizeData;

    public Map<String, PrizeTier> getPrizeData() {
        return prizeData;
    }

    public void setPrizeData(Map<String, PrizeTier> prizeData) {
        this.prizeData = prizeData;
    }

    public static class PrizeTier {
        private int wins;
        private double frequency;

        public int getWins() {
            return wins;
        }

        public void setWins(int wins) {
            this.wins = wins;
        }

        public double getFrequency() {
            return frequency;
        }

        public void setFrequency(double frequency) {
            this.frequency = frequency;
        }
    }
}