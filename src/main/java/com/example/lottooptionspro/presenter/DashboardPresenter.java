package com.example.lottooptionspro.presenter;

import com.example.lottooptionspro.model.response.EnhancedDashboardResponse;
import com.example.lottooptionspro.service.DashboardService;
import com.example.lottooptionspro.service.SegmentAnalysisService;
import com.example.lottooptionspro.model.dashboard.SegmentAnalysisResult;
import com.floyd.model.response.DashboardResponse;
import javafx.application.Platform;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.List;

@Component
public class DashboardPresenter {

    private DashboardView view;
    private final DashboardService service;
    private final SegmentAnalysisService segmentAnalysisService;
    private boolean segmentViewEnabled = false;

    @Autowired
    public DashboardPresenter(DashboardService service, SegmentAnalysisService segmentAnalysisService) {
        this.service = service;
        this.segmentAnalysisService = segmentAnalysisService;
    }

    public void setView(DashboardView view) {
        this.view = view;
    }

    public Mono<Void> loadDashboardData(String stateName, String gameName) {
        return service.getEnhancedDashboardData(stateName.toUpperCase(), gameName)
                .flatMap(this::updateUiWithEnhancedDashboardData)
                .doOnError(view::showDataError)
                .then();
    }

    public Mono<Void> loadBasicDashboardData(String stateName, String gameName) {
        return service.getDashboardData(stateName.toUpperCase(), gameName)
                .flatMap(this::updateUiWithDashboardData)
                .doOnError(view::showDataError)
                .then();
    }
    
    public void toggleSegmentView() {
        segmentViewEnabled = !segmentViewEnabled;
        if (view != null) {
            view.toggleSegmentView(segmentViewEnabled);
        }
    }
    
    public boolean isSegmentViewEnabled() {
        return segmentViewEnabled;
    }
    
    public void enableSegmentView(boolean enabled) {
        this.segmentViewEnabled = enabled;
        if (view != null) {
            view.toggleSegmentView(segmentViewEnabled);
        }
    }
    
    public void refreshSegmentAnalysis(List<com.floyd.model.dashboard.DrawResultPattern> drawResultPatterns) {
        if (view == null || !segmentViewEnabled) {
            return;
        }
        
        // Use the existing service to generate new segment analysis
        try {
            System.out.println("Refreshing segment analysis with " + drawResultPatterns.size() + " draw patterns");
            
            // Generate new segment analysis using the injected service
            SegmentAnalysisResult newResult = segmentAnalysisService.analyzeSegments(drawResultPatterns);
            
            // Update the UI with the fresh analysis
            Platform.runLater(() -> {
                if (view != null) {
                    view.setUpSegmentAnalysisPane(newResult);
                }
            });
            
        } catch (Exception e) {
            System.err.println("Error refreshing segment analysis: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private Mono<Void> updateUiWithEnhancedDashboardData(EnhancedDashboardResponse enhancedResponse) {
        return Mono.fromRunnable(() -> Platform.runLater(() -> {
            if (view == null) return;
            
            view.setUpDrawPatternTable(enhancedResponse.getDrawResultPatterns());
            
            // Use the original response for probability panes to avoid reflection issues
            if (enhancedResponse.getOriginalResponse() != null) {
                view.setUpProbabilityPanes(enhancedResponse.getOriginalResponse());
            }
            
            view.setUpLegend();
            
            if (enhancedResponse.hasSegmentData()) {
                view.setUpSegmentAnalysisPane(enhancedResponse.getSegmentAnalysis());
            }
            
            view.setUpEnhancedDashboard(enhancedResponse);
        }));
    }

    private Mono<Void> updateUiWithDashboardData(DashboardResponse dashboardResponse) {
        return Mono.fromRunnable(() -> Platform.runLater(() -> {
            if (view == null) return;
            view.setUpDrawPatternTable(dashboardResponse.getDrawResultPatterns());
            view.setUpProbabilityPanes(dashboardResponse);
            view.setUpLegend();
        }));
    }
}
