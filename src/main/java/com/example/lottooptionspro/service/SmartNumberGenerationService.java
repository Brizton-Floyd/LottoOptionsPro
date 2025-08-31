package com.example.lottooptionspro.service;

import com.example.lottooptionspro.model.smart.*;
import org.springframework.beans.factory.annotation.Value;
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

    private final WebClient smartGeneratorWebClient;
    private final WebClient lotteryConfigWebClient;

    public SmartNumberGenerationService(@Value("${dashboard.base-url}") String dashboardBaseUrl, WebClient.Builder webClientBuilder) {
        // Smart generation endpoints (v2) - port 8002
        this.smartGeneratorWebClient = webClientBuilder
                .baseUrl(dashboardBaseUrl)
                .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(2 * 1024 * 1024 * 1024))
                .build();
        
        // Lottery configuration endpoints (v1) - port 8001
        String configBaseUrl = dashboardBaseUrl.replace("8002", "8001");
        this.lotteryConfigWebClient = webClientBuilder
                .baseUrl(configBaseUrl)
                .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(2 * 1024 * 1024 * 1024))
                .build();
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
                    System.err.println("SSE connection failed: " + error.getMessage());
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
                        System.err.println("Server error (5xx) when fetching results for session: " + sessionId);
                        return response.bodyToMono(String.class)
                                .flatMap(body -> {
                                    System.err.println("Error response body: " + body);
                                    return Mono.error(new RuntimeException("Server error while fetching results. Session: " + sessionId + ". Response: " + body));
                                });
                    }
                )
                .onStatus(
                    status -> status.is4xxClientError(),
                    response -> {
                        System.err.println("Client error (4xx) when fetching results for session: " + sessionId);
                        return response.bodyToMono(String.class)
                                .flatMap(body -> {
                                    System.err.println("Error response body: " + body);
                                    if (response.statusCode().value() == 404) {
                                        return Mono.error(new RuntimeException("Session not found or expired: " + sessionId));
                                    }
                                    return Mono.error(new RuntimeException("Invalid request for session: " + sessionId + ". Response: " + body));
                                });
                    }
                )
                .bodyToMono(TicketGenerationResult.class);
    }

    public Mono<TicketGenerationResult> getFullAnalysisData(String fullAnalysisEndpoint) {
        return smartGeneratorWebClient.get()
                .uri(fullAnalysisEndpoint)
                .retrieve()
                .onStatus(
                    status -> status.is5xxServerError(),
                    response -> {
                        System.err.println("Server error (5xx) when fetching full analysis from: " + fullAnalysisEndpoint);
                        return response.bodyToMono(String.class)
                                .flatMap(body -> {
                                    System.err.println("Error response body: " + body);
                                    return Mono.error(new RuntimeException("Server error while fetching full analysis. Endpoint: " + fullAnalysisEndpoint + ". Response: " + body));
                                });
                    }
                )
                .onStatus(
                    status -> status.is4xxClientError(),
                    response -> {
                        System.err.println("Client error (4xx) when fetching full analysis from: " + fullAnalysisEndpoint);
                        return response.bodyToMono(String.class)
                                .flatMap(body -> {
                                    System.err.println("Error response body: " + body);
                                    return Mono.error(new RuntimeException("Invalid request for full analysis. Endpoint: " + fullAnalysisEndpoint + ". Response: " + body));
                                });
                    }
                )
                .bodyToMono(TicketGenerationResult.class);
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
        System.out.println("Testing API connectivity...");
        System.out.println("Smart Generator API (port 8002): " + smartGeneratorWebClient.mutate().build().toString());
        System.out.println("Lottery Config API (port 8001): " + lotteryConfigWebClient.mutate().build().toString());
        
        return getGenerationStatistics()
                .doOnSuccess(stats -> System.out.println("✅ Smart Generator API reachable"))
                .doOnError(error -> System.err.println("❌ Smart Generator API unreachable: " + error.getMessage()))
                .onErrorReturn("API Unreachable")
                .then(getAllLotteryConfigurations()
                        .doOnSuccess(configs -> System.out.println("✅ Lottery Config API reachable, " + configs.size() + " configs found"))
                        .doOnError(error -> System.err.println("❌ Lottery Config API unreachable: " + error.getMessage()))
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