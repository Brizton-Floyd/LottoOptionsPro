package com.example.lottooptionspro.service;

import com.example.lottooptionspro.model.strategyengine.StrategyEngineRequest;
import com.example.lottooptionspro.model.strategyengine.StrategyEngineResponse;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Service
public class StrategyEngineService {

    private final WebClient webClient;
    private static final String GENERATE_POOLS_ENDPOINT = "/api/v1/analysis/dsh/generate-pools";

    public StrategyEngineService(@Qualifier("analysisServiceWebClient") WebClient analysisServiceWebClient) {
        this.webClient = analysisServiceWebClient;
    }

    public Mono<StrategyEngineResponse> generatePools(StrategyEngineRequest request) {
        return webClient.post()
                .uri(GENERATE_POOLS_ENDPOINT)
                .bodyValue(request)
                .retrieve()
                .bodyToMono(StrategyEngineResponse.class);
    }

    public Mono<StrategyEngineResponse> generatePools(String gameId, String lotteryState, 
                                                      Integer poolSize, Integer setCount, 
                                                      String strategyBias) {
        StrategyEngineRequest request = new StrategyEngineRequest();
        request.setGameProfile(new com.example.lottooptionspro.model.strategyengine.GameProfile(gameId, lotteryState));
        request.setPreferences(new com.example.lottooptionspro.model.strategyengine.Preferences(poolSize, setCount, strategyBias));
        
        return generatePools(request);
    }
}
