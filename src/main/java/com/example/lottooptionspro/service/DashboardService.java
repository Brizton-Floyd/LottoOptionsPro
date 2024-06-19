package com.example.lottooptionspro.service;

import com.floyd.model.response.DashboardResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Service
public class DashboardService {
    private final WebClient webClient;
    private final String baseUrl;

    public DashboardService(@Value("${dashboard.base-url}") String baseUrl) {
        this.baseUrl = baseUrl;
        this.webClient = WebClient
                .builder()
                .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(2 * 1024 * 1024 * 1024)) // 2GB
                .baseUrl(baseUrl).build();
    }

    public Mono<DashboardResponse> getDashboardData(String state, String game) {
        return webClient.get()
                .uri("/api/v1/analysis/" + state + "/" + game + "/dashboard") // Inject arguments into the URL
                .retrieve()
                .bodyToMono(DashboardResponse.class);
    }
}
