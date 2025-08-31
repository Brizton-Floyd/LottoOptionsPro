package com.example.lottooptionspro.model.smart;

import java.util.Map;

public class LotteryConfiguration {
    private String id;
    private String name;
    private int drawSize;
    private int patternLength;
    private NumberRange numberRange;
    private Map<String, PrizeTier> prizeStructure;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getDrawSize() {
        return drawSize;
    }

    public void setDrawSize(int drawSize) {
        this.drawSize = drawSize;
    }

    public int getPatternLength() {
        return patternLength;
    }

    public void setPatternLength(int patternLength) {
        this.patternLength = patternLength;
    }

    public NumberRange getNumberRange() {
        return numberRange;
    }

    public void setNumberRange(NumberRange numberRange) {
        this.numberRange = numberRange;
    }

    public Map<String, PrizeTier> getPrizeStructure() {
        return prizeStructure;
    }

    public void setPrizeStructure(Map<String, PrizeTier> prizeStructure) {
        this.prizeStructure = prizeStructure;
    }

    public static class NumberRange {
        private int min;
        private int max;

        public int getMin() {
            return min;
        }

        public void setMin(int min) {
            this.min = min;
        }

        public int getMax() {
            return max;
        }

        public void setMax(int max) {
            this.max = max;
        }
    }

    public static class PrizeTier {
        private String tierName;
        private int matchCount;
        private String description;
        private boolean active;

        public String getTierName() {
            return tierName;
        }

        public void setTierName(String tierName) {
            this.tierName = tierName;
        }

        public int getMatchCount() {
            return matchCount;
        }

        public void setMatchCount(int matchCount) {
            this.matchCount = matchCount;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public boolean isActive() {
            return active;
        }

        public void setActive(boolean active) {
            this.active = active;
        }
    }
}