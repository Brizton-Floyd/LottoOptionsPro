package com.example.lottooptionspro.service;

import com.floyd.model.generatednumbers.GeneratedNumberData;
import com.floyd.model.request.RandomNumberGeneratorRequest;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Service
public class RandomNumberGeneratorService {

    private final WebClient webClient;

    public RandomNumberGeneratorService(@Qualifier("analysisServiceWebClient") WebClient analysisServiceWebClient) {
        this.webClient = analysisServiceWebClient;
    }

    public Mono<GeneratedNumberData> generateNumbers(RandomNumberGeneratorRequest request) {
        return webClient.post()
                .uri("/api/v1/analysis/generate/numbers")
                .bodyValue(request)
                .retrieve()
                .bodyToMono(GeneratedNumberData.class);
    }
}
