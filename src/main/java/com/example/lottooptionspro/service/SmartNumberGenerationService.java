package com.example.lottooptionspro.service;

import com.example.lottooptionspro.model.smart.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

@Service
public class SmartNumberGenerationService {

    private static final Logger logger = LoggerFactory.getLogger(SmartNumberGenerationService.class);

    private final WebClient smartGeneratorWebClient;
    private final WebClient lotteryConfigWebClient;

    public SmartNumberGenerationService(
            @Qualifier("analysisServiceWebClient") WebClient analysisServiceWebClient,
            @Qualifier("statesServiceWebClient") WebClient statesServiceWebClient) {
        this.smartGeneratorWebClient = analysisServiceWebClient;
        this.lotteryConfigWebClient = statesServiceWebClient;
    }

    public Mono<SmartGenerationResponse> startGeneration(SmartGenerationRequest request) {
        return smartGeneratorWebClient.post()
                .uri("/api/v2/generate-tickets")
                .bodyValue(request)
                .retrieve()
                .bodyToMono(SmartGenerationResponse.class);
    }

    public Flux<ServerSentEvent<String>> getGenerationProgress(String sessionId) {
        return smartGeneratorWebClient.get()
                .uri("/api/v2/generation-status/{sessionId}", sessionId)
                .accept(MediaType.TEXT_EVENT_STREAM)
                .retrieve()
                .bodyToFlux(new ParameterizedTypeReference<ServerSentEvent<String>>() {})
                .onErrorResume(error -> {
                    logger.error("SSE connection failed: {}", error.getMessage());
                    return Flux.empty();
                });
    }

    public Mono<TicketGenerationResult> getGenerationResult(String sessionId) {
        return smartGeneratorWebClient.get()
                .uri("/api/v2/generation-result/{sessionId}", sessionId)
                .retrieve()
                .onStatus(
                    status -> status.is5xxServerError(),
                    response -> {
                        logger.error("Server error (5xx) when fetching results for session: {}", sessionId);
                        return response.bodyToMono(String.class)
                                .flatMap(body -> {
                                    logger.error("Error response body: {}", body);
                                    return Mono.error(new RuntimeException("Server error while fetching results. Session: " + sessionId + ". Response: " + body));
                                });
                    }
                )
                .onStatus(
                    status -> status.is4xxClientError(),
                    response -> {
                        logger.error("Client error (4xx) when fetching results for session: {}", sessionId);
                        return response.bodyToMono(String.class)
                                .flatMap(body -> {
                                    logger.error("Error response body: {}", body);
                                    if (response.statusCode().value() == 404) {
                                        return Mono.error(new RuntimeException("Session not found or expired: " + sessionId));
                                    }
                                    return Mono.error(new RuntimeException("Invalid request for session: " + sessionId + ". Response: " + body));
                                });
                    }
                )
                .bodyToMono(TicketGenerationResult.class);
    }

    public Mono<HistoricalPerformance> getFullAnalysisData(String fullAnalysisEndpoint) {
        return smartGeneratorWebClient.get()
                .uri(fullAnalysisEndpoint)
                .retrieve()
                .onStatus(
                    status -> status.is5xxServerError(),
                    response -> {
                        logger.error("Server error (5xx) when fetching full analysis from: {}", fullAnalysisEndpoint);
                        return response.bodyToMono(String.class)
                                .flatMap(body -> {
                                    logger.error("Error response body: {}", body);
                                    return Mono.error(new RuntimeException("Server error while fetching full analysis. Endpoint: " + fullAnalysisEndpoint + ". Response: " + body));
                                });
                    }
                )
                .onStatus(
                    status -> status.is4xxClientError(),
                    response -> {
                        logger.error("Client error (4xx) when fetching full analysis from: {}", fullAnalysisEndpoint);
                        return response.bodyToMono(String.class)
                                .flatMap(body -> {
                                    logger.error("Error response body: {}", body);
                                    return Mono.error(new RuntimeException("Invalid request for full analysis. Endpoint: " + fullAnalysisEndpoint + ". Response: " + body));
                                });
                    }
                )
                .bodyToMono(HistoricalPerformance.class);
    }

    public Mono<Void> cancelGeneration(String sessionId) {
        return smartGeneratorWebClient.delete()
                .uri("/api/v2/generation-session/{sessionId}", sessionId)
                .retrieve()
                .bodyToMono(Void.class);
    }

    public Mono<List<LotteryConfiguration>> getAllLotteryConfigurations() {
        return lotteryConfigWebClient.get()
                .uri("/api/v1/lottery-targeting/configurations")
                .retrieve()
                .bodyToFlux(LotteryConfiguration.class)
                .collectList();
    }

    public Mono<LotteryConfiguration> getLotteryConfiguration(String configId) {
        return lotteryConfigWebClient.get()
                .uri("/api/v1/lottery-targeting/configurations/{configId}", configId)
                .retrieve()
                .bodyToMono(LotteryConfiguration.class);
    }

    public Mono<String> getGenerationStatistics() {
        return smartGeneratorWebClient.get()
                .uri("/api/v2/generation-statistics")
                .retrieve()
                .bodyToMono(String.class);
    }

    public Mono<String> testApiConnectivity() {
        logger.info("Testing API connectivity...");
        logger.debug("Smart Generator API: {}", smartGeneratorWebClient.mutate().build());
        logger.debug("Lottery Config API: {}", lotteryConfigWebClient.mutate().build());

        return getGenerationStatistics()
                .doOnSuccess(stats -> logger.info("Smart Generator API reachable"))
                .doOnError(error -> logger.error("Smart Generator API unreachable: {}", error.getMessage()))
                .onErrorReturn("API Unreachable")
                .then(getAllLotteryConfigurations()
                        .doOnSuccess(configs -> logger.info("Lottery Config API reachable, {} configs found", configs.size()))
                        .doOnError(error -> logger.error("Lottery Config API unreachable: {}", error.getMessage()))
                        .onErrorReturn(java.util.Collections.emptyList())
                        .map(configs -> "APIs tested")
                );
    }

    public String deriveConfigId(String stateName, String gameName) {
        return gameName
                .toLowerCase()
                .replaceAll("\\s+", "_")
                .replaceAll("[^a-z0-9_]", "");
    }
}
