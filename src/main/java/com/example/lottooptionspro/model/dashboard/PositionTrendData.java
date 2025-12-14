package com.example.lottooptionspro.model.dashboard;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public class PositionTrendData {
    private final int position;
    private final String timeRange;
    private final List<PositionTrendPoint> trendPoints;
    private final Map<String, Double> technicalIndicators;
    private final PositionAnalysisResult analysisResult;
    private final LocalDate startDate;
    private final LocalDate endDate;
    
    public PositionTrendData(int position, String timeRange, List<PositionTrendPoint> trendPoints,
                            Map<String, Double> technicalIndicators, PositionAnalysisResult analysisResult,
                            LocalDate startDate, LocalDate endDate) {
        this.position = position;
        this.timeRange = timeRange;
        this.trendPoints = trendPoints;
        this.technicalIndicators = technicalIndicators;
        this.analysisResult = analysisResult;
        this.startDate = startDate;
        this.endDate = endDate;
    }
    
    public int getPosition() {
        return position;
    }
    
    public String getTimeRange() {
        return timeRange;
    }
    
    public List<PositionTrendPoint> getTrendPoints() {
        return trendPoints;
    }
    
    public Map<String, Double> getTechnicalIndicators() {
        return technicalIndicators;
    }
    
    public PositionAnalysisResult getAnalysisResult() {
        return analysisResult;
    }
    
    public LocalDate getStartDate() {
        return startDate;
    }
    
    public LocalDate getEndDate() {
        return endDate;
    }
    
    public int getTotalDataPoints() {
        return trendPoints != null ? trendPoints.size() : 0;
    }
    
    public boolean hasValidData() {
        return trendPoints != null && !trendPoints.isEmpty();
    }
    
    @Override
    public String toString() {
        return "PositionTrendData{" +
                "position=" + position +
                ", timeRange='" + timeRange + '\'' +
                ", dataPoints=" + getTotalDataPoints() +
                ", startDate=" + startDate +
                ", endDate=" + endDate +
                '}';
    }
}