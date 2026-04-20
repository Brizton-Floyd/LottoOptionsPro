package com.example.lottooptionspro.service;

import com.example.lottooptionspro.model.dashboard.SegmentAnalysisResult;
import com.example.lottooptionspro.model.response.EnhancedDashboardResponse;
import com.floyd.model.response.DashboardResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Service
public class DashboardService {
    private static final Logger logger = LoggerFactory.getLogger(DashboardService.class);

    private final WebClient webClient;
    private final SegmentAnalysisService segmentAnalysisService;

    public DashboardService(@Qualifier("analysisServiceWebClient") WebClient analysisServiceWebClient,
                            SegmentAnalysisService segmentAnalysisService) {
        this.webClient = analysisServiceWebClient;
        this.segmentAnalysisService = segmentAnalysisService;
    }

    public Mono<DashboardResponse> getDashboardData(String state, String game) {
        return webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/api/v1/analysis/{state}/{game}/dashboard")
                        .build(state, game))
                .retrieve()
                .bodyToMono(DashboardResponse.class);
    }
    
    public Mono<EnhancedDashboardResponse> getEnhancedDashboardData(String state, String game) {
        return getDashboardData(state, game)
                .map(this::enhanceWithSegmentAnalysis)
                .onErrorReturn(createEmptyEnhancedResponse());
    }
    
    private EnhancedDashboardResponse enhanceWithSegmentAnalysis(DashboardResponse originalResponse) {
        EnhancedDashboardResponse enhanced = new EnhancedDashboardResponse(originalResponse);
        
        try {
            if (originalResponse != null && originalResponse.getDrawResultPatterns() != null && 
                !originalResponse.getDrawResultPatterns().isEmpty()) {
                
                SegmentAnalysisResult segmentAnalysis = segmentAnalysisService
                        .analyzeSegments(originalResponse.getDrawResultPatterns());
                enhanced.setSegmentAnalysis(segmentAnalysis);
            }
        } catch (Exception e) {
            logger.error("Error generating segment analysis: {}", e.getMessage(), e);
            enhanced.setSegmentAnalysisEnabled(false);
        }
        
        return enhanced;
    }
    
    private EnhancedDashboardResponse createEmptyEnhancedResponse() {
        EnhancedDashboardResponse empty = new EnhancedDashboardResponse();
        empty.setSegmentAnalysisEnabled(false);
        return empty;
    }
}
