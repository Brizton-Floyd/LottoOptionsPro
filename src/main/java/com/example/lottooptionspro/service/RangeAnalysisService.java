package com.example.lottooptionspro.service;

import com.example.lottooptionspro.model.range.*;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import reactor.core.publisher.Mono;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@Service
public class RangeAnalysisService {
    
    private static final Logger logger = LoggerFactory.getLogger(RangeAnalysisService.class);
    private static final String API_BASE_URL = "http://localhost:8002/api/v1/analysis";
    
    private final RestTemplate restTemplate;
    private final Gson gson;
    
    public RangeAnalysisService() {
        this.restTemplate = new RestTemplate();
        this.gson = new GsonBuilder()
                .setPrettyPrinting()
                .create();
    }
    
    public Mono<RangeAnalysisResponse> performRangeAnalysis(RangeAnalysisRequest request) {
        return Mono.fromCallable(() -> {
            try {
                logger.info("Performing range analysis for {}:{} with type {}", 
                           request.getLotteryState(), request.getLotteryGame(), request.getAnalysisType());
                
                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_JSON);
                
                String requestJson = gson.toJson(request);
                logger.debug("Request payload: {}", requestJson);
                
                HttpEntity<String> entity = new HttpEntity<>(requestJson, headers);
                
                String response = restTemplate.postForObject(
                    API_BASE_URL + "/range-analysis", 
                    entity, 
                    String.class
                );
                
                if (response != null) {
                    logger.debug("API Response: {}", response);
                    RangeAnalysisResponse analysisResponse = gson.fromJson(response, RangeAnalysisResponse.class);
                    logger.info("Range analysis completed successfully. Draws analyzed: {}", analysisResponse.getTotalDrawsAnalyzed());
                    return analysisResponse;
                } else {
                    throw new RuntimeException("Empty response from range analysis API");
                }
                
            } catch (Exception e) {
                logger.error("Error performing range analysis", e);
                throw new RuntimeException("Failed to perform range analysis: " + e.getMessage(), e);
            }
        });
    }
    
    public Mono<RangeAnalysisResponse> performRangeAnalysis(String state, String game, AnalysisType analysisType, 
                                                           int rangeSize, int maxDraws, List<Integer> positions) {
        RangeAnalysisRequest request = new RangeAnalysisRequest();
        request.setLotteryState(state);
        request.setLotteryGame(game);
        request.setAnalysisType(analysisType);
        request.setRangeSize(rangeSize);
        request.setMaxDraws(maxDraws);
        request.setIncludePerformanceMetrics(true);
        request.setDrawPositions(positions);
        
        return performRangeAnalysis(request);
    }
    
    public Mono<RangeAnalysisResponse> performDefaultRangeAnalysis(String state, String game) {
        GameConfiguration config = new GameConfiguration(state, game);
        return performRangeAnalysis(state, game, AnalysisType.ACTUAL, 
                                   config.getDefaultRangeSize(), 
                                   config.getOptimalMaxDraws(), 
                                   config.getDrawPositions());
    }
    
    public Mono<RangeAnalysisResponse> performQuickAnalysis(String state, String game, AnalysisType analysisType) {
        GameConfiguration config = new GameConfiguration(state, game);
        return performRangeAnalysis(state, game, analysisType, 
                                   config.getDefaultRangeSize(), 
                                   Math.min(30, config.getOptimalMaxDraws()), 
                                   config.getDrawPositions());
    }
    
    public CompletableFuture<RangeAnalysisResponse> performRangeAnalysisAsync(RangeAnalysisRequest request) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return performRangeAnalysis(request).block();
            } catch (Exception e) {
                logger.error("Async range analysis failed", e);
                throw new RuntimeException("Async range analysis failed", e);
            }
        });
    }
    
    public boolean isServiceAvailable() {
        try {
            String healthUrl = API_BASE_URL.replace("/analysis", "/health");
            String response = restTemplate.getForObject(healthUrl, String.class);
            return response != null;
        } catch (Exception e) {
            logger.warn("Range analysis service health check failed", e);
            return false;
        }
    }
    
    public List<AnalysisType> getSupportedAnalysisTypes() {
        return Arrays.asList(AnalysisType.values());
    }
    
    public List<Integer> getSupportedRangeSizes() {
        return Arrays.asList(5, 10, 15, 20);
    }
    
    public List<Integer> getSupportedRangeSizesForGame(String state, String game) {
        GameConfiguration config = new GameConfiguration(state, game);
        return config.getValidRangeSizes(); // Use valid range sizes instead of all supported
    }
    
    public List<String> generateRangeHeadersForGame(String state, String game, int rangeSize) {
        GameConfiguration config = new GameConfiguration(state, game);
        return config.generateValidRangeHeaders(rangeSize);
    }
    
    public boolean isValidRangeSizeForGame(String state, String game, int rangeSize) {
        GameConfiguration config = new GameConfiguration(state, game);
        return config.isValidRangeSize(rangeSize);
    }
    
    public String getRangePreviewForGame(String state, String game, int rangeSize) {
        GameConfiguration config = new GameConfiguration(state, game);
        return config.getRangePreviewText(rangeSize);
    }
    
    public List<Integer> getDefaultPositionsForGame(String game) {
        // This method is kept for backward compatibility but now uses GameConfiguration
        GameConfiguration config = new GameConfiguration("", game);
        return config.getDrawPositions();
    }
    
    public GameConfiguration getGameConfiguration(String state, String game) {
        return new GameConfiguration(state, game);
    }
    
    public RangeAnalysisRequest createDefaultRequest(String state, String game) {
        GameConfiguration config = new GameConfiguration(state, game);
        RangeAnalysisRequest request = new RangeAnalysisRequest();
        request.setLotteryState(state.toUpperCase());
        request.setLotteryGame(game);
        request.setAnalysisType(AnalysisType.ACTUAL);
        request.setRangeSize(config.getDefaultRangeSize());
        request.setMaxDraws(config.getOptimalMaxDraws());
        request.setIncludePerformanceMetrics(true);
        request.setDrawPositions(config.getDrawPositions());
        return request;
    }
}