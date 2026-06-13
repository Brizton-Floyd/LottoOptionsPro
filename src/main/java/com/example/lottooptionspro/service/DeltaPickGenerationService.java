package com.example.lottooptionspro.service;

import com.example.lottooptionspro.model.deltapick.DeltaPickGenerationRequest;
import com.example.lottooptionspro.model.deltapick.DeltaPickGenerationResponse;
import com.example.lottooptionspro.model.deltapick.GameConfigResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

/**
 * Service for generating delta-based lottery picks and fetching game configurations.
 */
@Service
public class DeltaPickGenerationService {

    private static final Logger logger = LoggerFactory.getLogger(DeltaPickGenerationService.class);

    private final WebClient webClient;

    public DeltaPickGenerationService(@Qualifier("analysisServiceWebClient") WebClient analysisServiceWebClient) {
        this.webClient = analysisServiceWebClient;
    }

    /**
     * Fetches game configuration including maxNumber and drawPositionCount.
     *
     * @param state the lottery state
     * @param game the lottery game name
     * @return Mono containing the game configuration
     */
    public Mono<GameConfigResponse> fetchGameConfig(String state, String game) {
        logger.info("Fetching game config for {}:{}", state, game);
        return webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/api/v1/lotto-options-core/games/{state}/{game}")
                        .build(state, game))
                .retrieve()
                .bodyToMono(GameConfigResponse.class)
                .doOnNext(config -> {
                    if (config.getMaxNumber() == null || config.getDrawPositionCount() == null) {
                        throw new RuntimeException(String.format(
                                "Invalid game config: maxNumber=%s, drawPositionCount=%s",
                                config.getMaxNumber(), config.getDrawPositionCount()));
                    }
                    logger.info("Game config loaded: maxNumber={}, drawPositionCount={}, game={}",
                            config.getMaxNumber(),
                            config.getDrawPositionCount(),
                            config.getLotteryGame() != null ? config.getLotteryGame().getFullName() : "unknown");
                })
                .onErrorMap(e -> new RuntimeException("Failed to fetch game configuration: " + e.getMessage(), e));
    }

    /**
     * Generates delta-based lottery picks using the specified constraints.
     *
     * @param request the delta pick generation request
     * @return Mono containing the generation response with picks and analysis
     */
    public Mono<DeltaPickGenerationResponse> generateDeltaPicks(DeltaPickGenerationRequest request) {
        logger.info("Generating delta picks for {}:{} in {} mode with {} combinations",
                request.getLotteryState(),
                request.getLotteryGame(),
                request.getDeltaInputMode(),
                request.getNumCombinations());
        logger.info("Include recent performance in request: {}", request.getIncludeRecentPerformance());
        logger.info("Lookback days in request: {}", request.getLookbackDays());
        if ("RAW".equals(request.getDeltaInputMode())) {
            logger.info("Raw deltas: {}", request.getRawDeltas());
        } else {
            logger.info("Sorted delta magnitudes: {}", request.getSortedDeltaMagnitudes());
        }

        return webClient.post()
                .uri("/api/v1/analysis/generate/delta-picks")
                .bodyValue(request)
                .retrieve()
                .bodyToMono(DeltaPickGenerationResponse.class)
                .doOnNext(result -> logger.info(
                        "Delta picks generated successfully. Total picks: {}, Execution time: {}ms",
                        result.getGeneratedPicks().size(),
                        result.getExecutionTimeMs()))
                .onErrorMap(e -> new RuntimeException("Failed to generate delta picks: " + e.getMessage(), e));
    }
}
