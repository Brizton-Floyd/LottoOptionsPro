package com.example.lottooptionspro.service;

import com.example.lottooptionspro.model.dashboard.SegmentAnalysisResult;
import com.example.lottooptionspro.model.response.EnhancedDashboardResponse;
import com.floyd.model.response.DashboardResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Service
public class DashboardService {
    private final WebClient webClient;
    private final String baseUrl;
    private final SegmentAnalysisService segmentAnalysisService;

    @Autowired
    public DashboardService(@Value("${dashboard.base-url}") String baseUrl, 
                           SegmentAnalysisService segmentAnalysisService) {
        this.baseUrl = baseUrl;
        this.segmentAnalysisService = segmentAnalysisService;
        this.webClient = WebClient
                .builder()
                .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(256 * 1024 * 1024)) // 256MB
                .baseUrl(baseUrl).build();
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
            System.err.println("Error generating segment analysis: " + e.getMessage());
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
