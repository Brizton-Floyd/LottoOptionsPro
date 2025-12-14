package com.example.lottooptionspro.presenter;

import com.example.lottooptionspro.model.dashboard.SegmentAnalysisResult;
import com.example.lottooptionspro.model.response.EnhancedDashboardResponse;
import com.floyd.model.dashboard.DrawResultPattern;
import com.floyd.model.response.DashboardResponse;

import java.util.List;

public interface DashboardView {
    void setUpDrawPatternTable(List<DrawResultPattern> drawResultPatterns);

    void setUpProbabilityPanes(DashboardResponse dashboardResponse);

    void setUpLegend();

    void showDataError(Throwable error);
    
    void setUpSegmentAnalysisPane(SegmentAnalysisResult segmentAnalysis);
    
    void setUpEnhancedDashboard(EnhancedDashboardResponse enhancedResponse);
    
    void toggleSegmentView(boolean showSegments);
}
