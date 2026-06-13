package com.example.lottooptionspro.service;

import com.example.lottooptionspro.model.range.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@Service
public class RangeAnalysisService {

    private static final Logger logger = LoggerFactory.getLogger(RangeAnalysisService.class);

    private final WebClient webClient;

    public RangeAnalysisService(@Qualifier("analysisServiceWebClient") WebClient analysisServiceWebClient) {
        this.webClient = analysisServiceWebClient;
    }

    public Mono<RangeAnalysisResponse> performRangeAnalysis(RangeAnalysisRequest request) {
        logger.info("Performing range analysis for {}:{} with type {}",
                request.getLotteryState(), request.getLotteryGame(), request.getAnalysisType());

        return webClient.post()
                .uri("/api/v1/analysis/range-analysis")
                .bodyValue(request)
                .retrieve()
                .bodyToMono(RangeAnalysisResponse.class)
                .doOnNext(analysisResponse -> logger.info(
                        "Range analysis completed successfully. Draws analyzed: {}",
                        analysisResponse.getTotalDrawsAnalyzed()))
                .onErrorMap(e -> new RuntimeException("Failed to perform range analysis: " + e.getMessage(), e));
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
            String response = webClient.get()
                    .uri("/api/v1/health")
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();
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
        return config.getValidRangeSizes();
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
