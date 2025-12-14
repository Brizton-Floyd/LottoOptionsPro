package com.example.lottooptionspro.model.dashboard;

import java.util.List;
import java.util.Map;

public class SegmentAnalysisResult {
    private List<NumberSegment> segments;
    private Map<String, SegmentPerformanceMetrics> segmentMetrics;
    private OverallAnalysis overallAnalysis;
    private List<SegmentRecommendation> recommendations;
    private Map<String, SegmentGameOutHistory> segmentHistories;
    
    public SegmentAnalysisResult() {}
    
    public static class SegmentPerformanceMetrics {
        private String segmentName;
        private double averageGamesOut;
        private double hitFrequency;
        private int totalHits;
        private int coldestNumber;
        private int hottestNumber;
        private double segmentEfficiency;
        private List<Integer> trendingGamesOutValues;
        
        public SegmentPerformanceMetrics() {}
        
        public String getSegmentName() {
            return segmentName;
        }
        
        public void setSegmentName(String segmentName) {
            this.segmentName = segmentName;
        }
        
        public double getAverageGamesOut() {
            return averageGamesOut;
        }
        
        public void setAverageGamesOut(double averageGamesOut) {
            this.averageGamesOut = averageGamesOut;
        }
        
        public double getHitFrequency() {
            return hitFrequency;
        }
        
        public void setHitFrequency(double hitFrequency) {
            this.hitFrequency = hitFrequency;
        }
        
        public int getTotalHits() {
            return totalHits;
        }
        
        public void setTotalHits(int totalHits) {
            this.totalHits = totalHits;
        }
        
        public int getColdestNumber() {
            return coldestNumber;
        }
        
        public void setColdestNumber(int coldestNumber) {
            this.coldestNumber = coldestNumber;
        }
        
        public int getHottestNumber() {
            return hottestNumber;
        }
        
        public void setHottestNumber(int hottestNumber) {
            this.hottestNumber = hottestNumber;
        }
        
        public double getSegmentEfficiency() {
            return segmentEfficiency;
        }
        
        public void setSegmentEfficiency(double segmentEfficiency) {
            this.segmentEfficiency = segmentEfficiency;
        }
        
        public List<Integer> getTrendingGamesOutValues() {
            return trendingGamesOutValues;
        }
        
        public void setTrendingGamesOutValues(List<Integer> trendingGamesOutValues) {
            this.trendingGamesOutValues = trendingGamesOutValues;
        }
    }
    
    public static class OverallAnalysis {
        private int totalSegments;
        private String mostProductiveSegment;
        private String leastProductiveSegment;
        private double overallAverageGamesOut;
        private String analysisDate;
        private int totalDrawsAnalyzed;
        
        public OverallAnalysis() {}
        
        public int getTotalSegments() {
            return totalSegments;
        }
        
        public void setTotalSegments(int totalSegments) {
            this.totalSegments = totalSegments;
        }
        
        public String getMostProductiveSegment() {
            return mostProductiveSegment;
        }
        
        public void setMostProductiveSegment(String mostProductiveSegment) {
            this.mostProductiveSegment = mostProductiveSegment;
        }
        
        public String getLeastProductiveSegment() {
            return leastProductiveSegment;
        }
        
        public void setLeastProductiveSegment(String leastProductiveSegment) {
            this.leastProductiveSegment = leastProductiveSegment;
        }
        
        public double getOverallAverageGamesOut() {
            return overallAverageGamesOut;
        }
        
        public void setOverallAverageGamesOut(double overallAverageGamesOut) {
            this.overallAverageGamesOut = overallAverageGamesOut;
        }
        
        public String getAnalysisDate() {
            return analysisDate;
        }
        
        public void setAnalysisDate(String analysisDate) {
            this.analysisDate = analysisDate;
        }
        
        public int getTotalDrawsAnalyzed() {
            return totalDrawsAnalyzed;
        }
        
        public void setTotalDrawsAnalyzed(int totalDrawsAnalyzed) {
            this.totalDrawsAnalyzed = totalDrawsAnalyzed;
        }
    }
    
    public static class SegmentRecommendation {
        private String segmentName;
        private List<Integer> recommendedGamesOutValues;
        private double confidenceScore;
        private String reasoning;
        private String priority;
        
        public SegmentRecommendation() {}
        
        public String getSegmentName() {
            return segmentName;
        }
        
        public void setSegmentName(String segmentName) {
            this.segmentName = segmentName;
        }
        
        public List<Integer> getRecommendedGamesOutValues() {
            return recommendedGamesOutValues;
        }
        
        public void setRecommendedGamesOutValues(List<Integer> recommendedGamesOutValues) {
            this.recommendedGamesOutValues = recommendedGamesOutValues;
        }
        
        public double getConfidenceScore() {
            return confidenceScore;
        }
        
        public void setConfidenceScore(double confidenceScore) {
            this.confidenceScore = confidenceScore;
        }
        
        public String getReasoning() {
            return reasoning;
        }
        
        public void setReasoning(String reasoning) {
            this.reasoning = reasoning;
        }
        
        public String getPriority() {
            return priority;
        }
        
        public void setPriority(String priority) {
            this.priority = priority;
        }
    }
    
    public List<NumberSegment> getSegments() {
        return segments;
    }
    
    public void setSegments(List<NumberSegment> segments) {
        this.segments = segments;
    }
    
    public Map<String, SegmentPerformanceMetrics> getSegmentMetrics() {
        return segmentMetrics;
    }
    
    public void setSegmentMetrics(Map<String, SegmentPerformanceMetrics> segmentMetrics) {
        this.segmentMetrics = segmentMetrics;
    }
    
    public OverallAnalysis getOverallAnalysis() {
        return overallAnalysis;
    }
    
    public void setOverallAnalysis(OverallAnalysis overallAnalysis) {
        this.overallAnalysis = overallAnalysis;
    }
    
    public List<SegmentRecommendation> getRecommendations() {
        return recommendations;
    }
    
    public void setRecommendations(List<SegmentRecommendation> recommendations) {
        this.recommendations = recommendations;
    }
    
    public Map<String, SegmentGameOutHistory> getSegmentHistories() {
        return segmentHistories;
    }
    
    public void setSegmentHistories(Map<String, SegmentGameOutHistory> segmentHistories) {
        this.segmentHistories = segmentHistories;
    }
    
    @Override
    public String toString() {
        return "SegmentAnalysisResult{" +
                "segments=" + (segments != null ? segments.size() : 0) + " segments" +
                ", overallAnalysis=" + overallAnalysis +
                ", recommendations=" + (recommendations != null ? recommendations.size() : 0) + " recommendations" +
                '}';
    }
}