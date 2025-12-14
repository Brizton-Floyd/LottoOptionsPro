package com.example.lottooptionspro.presenter;

import com.example.lottooptionspro.model.range.*;

import java.util.List;
import java.util.Map;

public interface RangeAnalysisView {
    
    void setUpAnalysisControls(List<AnalysisType> analysisTypes, List<Integer> rangeSizes, 
                              String currentState, String currentGame);
    
    void setUpRangeGrid(List<String> rangeHeaders, Map<String, PerformanceMetrics> performanceMetrics);
    
    void setUpPerformanceMetricsTable(Map<String, PerformanceMetrics> performanceMetrics);
    
    void setUpPositionAnalysisCharts(Map<String, PositionAnalysis> positionAnalysis);
    
    void setUpDrawResultsTimeline(List<DrawResult> drawResults);
    
    void updateAnalysisConfiguration(RangeAnalysisResponse.GameConfiguration config);
    
    void showAnalysisResults(RangeAnalysisResponse response);
    
    void showLoadingIndicator(boolean loading);
    
    void showDataError(Throwable error);
    
    void showNoDataMessage(String message);
    
    void enableExportControls(boolean enabled);
    
    void highlightRange(String range);
    
    void clearRangeHighlights();
    
    void refreshUI();
    
    void setAnalysisInProgress(boolean inProgress);
    
    void showAnalysisTypeTooltip(AnalysisType analysisType, String description);
}