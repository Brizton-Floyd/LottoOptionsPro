package com.example.lottooptionspro.service;

import com.floyd.model.generatednumbers.GeneratedNumberData;
import com.floyd.model.request.RandomNumberGeneratorRequest;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Service
public class RandomNumberGeneratorService {

    private final WebClient webClient ;

    public RandomNumberGeneratorService(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder.baseUrl("http://localhost:8002/api/v1/analysis").build();
    }

    public Mono<GeneratedNumberData> generateNumbers(RandomNumberGeneratorRequest request) {
        return webClient.post()
                .uri("/generate/numbers")
                .bodyValue(request)
                .retrieve()
                .bodyToMono(GeneratedNumberData.class);
    }
}
