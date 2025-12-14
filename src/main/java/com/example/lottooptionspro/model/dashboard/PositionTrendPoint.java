package com.example.lottooptionspro.model.dashboard;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public class PositionTrendPoint {
    private final String drawDate;
    private final LocalDate date;
    private final int drawIndex;
    
    // OHLC-style data for games out values in this position
    private final double open;      // Games out at start of period
    private final double high;      // Highest games out in period
    private final double low;       // Lowest games out in period
    private final double close;     // Games out at end of period
    
    // Volume and frequency data
    private final int hitCount;     // Number of hits in this position during period
    private final int totalDraws;   // Total draws in period
    private final double hitFrequency; // Hit percentage
    
    // Numbers that appeared in this position
    private final List<Integer> numbersInPosition;
    
    // Technical analysis values
    private final Map<String, Double> movingAverages; // MA5, MA10, MA20
    private final double trendStrength;
    private final String trendDirection;
    
    public PositionTrendPoint(String drawDate, LocalDate date, int drawIndex,
                             double open, double high, double low, double close,
                             int hitCount, int totalDraws, double hitFrequency,
                             List<Integer> numbersInPosition,
                             Map<String, Double> movingAverages,
                             double trendStrength, String trendDirection) {
        this.drawDate = drawDate;
        this.date = date;
        this.drawIndex = drawIndex;
        this.open = open;
        this.high = high;
        this.low = low;
        this.close = close;
        this.hitCount = hitCount;
        this.totalDraws = totalDraws;
        this.hitFrequency = hitFrequency;
        this.numbersInPosition = numbersInPosition;
        this.movingAverages = movingAverages;
        this.trendStrength = trendStrength;
        this.trendDirection = trendDirection;
    }
    
    // Getters
    public String getDrawDate() {
        return drawDate;
    }
    
    public LocalDate getDate() {
        return date;
    }
    
    public int getDrawIndex() {
        return drawIndex;
    }
    
    public double getOpen() {
        return open;
    }
    
    public double getHigh() {
        return high;
    }
    
    public double getLow() {
        return low;
    }
    
    public double getClose() {
        return close;
    }
    
    public int getHitCount() {
        return hitCount;
    }
    
    public int getTotalDraws() {
        return totalDraws;
    }
    
    public double getHitFrequency() {
        return hitFrequency;
    }
    
    public List<Integer> getNumbersInPosition() {
        return numbersInPosition;
    }
    
    public Map<String, Double> getMovingAverages() {
        return movingAverages;
    }
    
    public double getTrendStrength() {
        return trendStrength;
    }
    
    public String getTrendDirection() {
        return trendDirection;
    }
    
    // Utility methods
    public boolean isHit() {
        return hitCount > 0;
    }
    
    public double getRange() {
        return high - low;
    }
    
    public boolean isBullish() {
        return close > open;
    }
    
    public boolean isBearish() {
        return close < open;
    }
    
    public double getBodySize() {
        return Math.abs(close - open);
    }
    
    public double getUpperShadow() {
        return high - Math.max(open, close);
    }
    
    public double getLowerShadow() {
        return Math.min(open, close) - low;
    }
    
    @Override
    public String toString() {
        return "PositionTrendPoint{" +
                "drawDate='" + drawDate + '\'' +
                ", OHLC=[" + open + ", " + high + ", " + low + ", " + close + "]" +
                ", hitCount=" + hitCount +
                ", hitFrequency=" + String.format("%.2f%%", hitFrequency * 100) +
                ", trend=" + trendDirection +
                '}';
    }
}