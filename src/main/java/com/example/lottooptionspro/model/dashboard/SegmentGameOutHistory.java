package com.example.lottooptionspro.model.dashboard;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SegmentGameOutHistory {
    private int segmentStart;
    private int segmentEnd;
    private Map<Integer, List<GameOutEntry>> numberHistories;
    private LocalDate firstDrawDate;
    private LocalDate lastDrawDate;
    private int totalDraws;
    
    public SegmentGameOutHistory() {
        this.numberHistories = new HashMap<>();
    }
    
    public SegmentGameOutHistory(int segmentStart, int segmentEnd) {
        this.segmentStart = segmentStart;
        this.segmentEnd = segmentEnd;
        this.numberHistories = new HashMap<>();
    }
    
    public static class GameOutEntry {
        private String drawDate;
        private int gamesOutValue;
        private boolean wasHit;
        
        public GameOutEntry() {}
        
        public GameOutEntry(String drawDate, int gamesOutValue, boolean wasHit) {
            this.drawDate = drawDate;
            this.gamesOutValue = gamesOutValue;
            this.wasHit = wasHit;
        }
        
        public String getDrawDate() {
            return drawDate;
        }
        
        public void setDrawDate(String drawDate) {
            this.drawDate = drawDate;
        }
        
        public int getGamesOutValue() {
            return gamesOutValue;
        }
        
        public void setGamesOutValue(int gamesOutValue) {
            this.gamesOutValue = gamesOutValue;
        }
        
        public boolean isWasHit() {
            return wasHit;
        }
        
        public void setWasHit(boolean wasHit) {
            this.wasHit = wasHit;
        }
        
        @Override
        public String toString() {
            return "GameOutEntry{" +
                    "drawDate='" + drawDate + '\'' +
                    ", gamesOutValue=" + gamesOutValue +
                    ", wasHit=" + wasHit +
                    '}';
        }
    }
    
    public int getSegmentStart() {
        return segmentStart;
    }
    
    public void setSegmentStart(int segmentStart) {
        this.segmentStart = segmentStart;
    }
    
    public int getSegmentEnd() {
        return segmentEnd;
    }
    
    public void setSegmentEnd(int segmentEnd) {
        this.segmentEnd = segmentEnd;
    }
    
    public Map<Integer, List<GameOutEntry>> getNumberHistories() {
        return numberHistories;
    }
    
    public void setNumberHistories(Map<Integer, List<GameOutEntry>> numberHistories) {
        this.numberHistories = numberHistories;
    }
    
    public LocalDate getFirstDrawDate() {
        return firstDrawDate;
    }
    
    public void setFirstDrawDate(LocalDate firstDrawDate) {
        this.firstDrawDate = firstDrawDate;
    }
    
    public LocalDate getLastDrawDate() {
        return lastDrawDate;
    }
    
    public void setLastDrawDate(LocalDate lastDrawDate) {
        this.lastDrawDate = lastDrawDate;
    }
    
    public int getTotalDraws() {
        return totalDraws;
    }
    
    public void setTotalDraws(int totalDraws) {
        this.totalDraws = totalDraws;
    }
    
    public void addGameOutEntry(int number, String drawDate, int gamesOutValue, boolean wasHit) {
        if (number >= segmentStart && number <= segmentEnd) {
            numberHistories.computeIfAbsent(number, k -> new ArrayList<>())
                    .add(new GameOutEntry(drawDate, gamesOutValue, wasHit));
        }
    }
    
    public List<GameOutEntry> getHistoryForNumber(int number) {
        return numberHistories.getOrDefault(number, new ArrayList<>());
    }
    
    public List<GameOutEntry> getCurrentGamesOutEntries() {
        List<GameOutEntry> currentEntries = new ArrayList<>();
        for (Map.Entry<Integer, List<GameOutEntry>> entry : numberHistories.entrySet()) {
            List<GameOutEntry> history = entry.getValue();
            if (!history.isEmpty()) {
                currentEntries.add(history.get(history.size() - 1));
            }
        }
        return currentEntries;
    }
    
    public Map<Integer, Integer> getCurrentGamesOutMap() {
        Map<Integer, Integer> currentMap = new HashMap<>();
        for (Map.Entry<Integer, List<GameOutEntry>> entry : numberHistories.entrySet()) {
            List<GameOutEntry> history = entry.getValue();
            if (!history.isEmpty()) {
                GameOutEntry latestEntry = history.get(history.size() - 1);
                currentMap.put(entry.getKey(), latestEntry.getGamesOutValue());
            }
        }
        return currentMap;
    }
    
    @Override
    public String toString() {
        return "SegmentGameOutHistory{" +
                "segmentStart=" + segmentStart +
                ", segmentEnd=" + segmentEnd +
                ", totalDraws=" + totalDraws +
                ", numberHistories=" + numberHistories.size() + " numbers" +
                '}';
    }
}