package com.example.lottooptionspro.model.response;

import com.example.lottooptionspro.model.dashboard.SegmentAnalysisResult;
import com.floyd.model.dashboard.DrawResultPattern;
import com.floyd.model.dashboard.PatternAnalysisResult;
import com.floyd.model.response.DashboardResponse;

import java.util.List;
import java.util.Map;

public class EnhancedDashboardResponse {
    private DashboardResponse originalResponse;
    private SegmentAnalysisResult segmentAnalysis;
    private boolean segmentAnalysisEnabled;
    
    public EnhancedDashboardResponse() {
        this.segmentAnalysisEnabled = false;
    }
    
    public EnhancedDashboardResponse(DashboardResponse originalResponse) {
        this.originalResponse = originalResponse;
        this.segmentAnalysisEnabled = false;
    }
    
    // Delegate methods to original response
    public List<DrawResultPattern> getDrawResultPatterns() {
        return originalResponse != null ? originalResponse.getDrawResultPatterns() : null;
    }
    
    public int getTotalDraws() {
        return originalResponse != null ? originalResponse.getTotalDraws() : 0;
    }
    
    public String[] getHistoryBeginAndEndDates() {
        return originalResponse != null ? originalResponse.getHistoryBeginAndEndDates() : new String[0];
    }
    
    public Map<String, PatternAnalysisResult> getOddEvenPatterns() {
        return originalResponse != null ? originalResponse.getOddEvenPatterns() : null;
    }
    
    public Map<String, PatternAnalysisResult> getHighLowPatterns() {
        return originalResponse != null ? originalResponse.getHighLowPatterns() : null;
    }
    
    public DashboardResponse getOriginalResponse() {
        return originalResponse;
    }
    
    public void setOriginalResponse(DashboardResponse originalResponse) {
        this.originalResponse = originalResponse;
    }
    
    public SegmentAnalysisResult getSegmentAnalysis() {
        return segmentAnalysis;
    }
    
    public void setSegmentAnalysis(SegmentAnalysisResult segmentAnalysis) {
        this.segmentAnalysis = segmentAnalysis;
        this.segmentAnalysisEnabled = segmentAnalysis != null;
    }
    
    public boolean isSegmentAnalysisEnabled() {
        return segmentAnalysisEnabled;
    }
    
    public void setSegmentAnalysisEnabled(boolean segmentAnalysisEnabled) {
        this.segmentAnalysisEnabled = segmentAnalysisEnabled;
    }
    
    public boolean hasSegmentData() {
        return segmentAnalysis != null && 
               segmentAnalysis.getSegments() != null && 
               !segmentAnalysis.getSegments().isEmpty();
    }
    
    @Override
    public String toString() {
        return "EnhancedDashboardResponse{" +
                "segmentAnalysisEnabled=" + segmentAnalysisEnabled +
                ", hasSegmentData=" + hasSegmentData() +
                ", totalDraws=" + getTotalDraws() +
                ", drawResultPatterns=" + (getDrawResultPatterns() != null ? getDrawResultPatterns().size() : 0) + " patterns" +
                '}';
    }
}