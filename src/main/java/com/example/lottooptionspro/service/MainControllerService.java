package com.example.lottooptionspro.service;

import com.example.lottooptionspro.models.LotteryGame;
import com.example.lottooptionspro.models.LotteryState;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Flux;
import reactor.util.retry.Retry;

import javax.naming.ServiceUnavailableException;
import java.net.ConnectException;
import java.time.Duration;
import java.util.List;

@Service
public class MainControllerService {
    private final WebClient webClient;

    public MainControllerService(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder.baseUrl("http://localhost:8001/api/v1").build();
    }

    public Flux<LotteryState> fetchStateGames() {
        return webClient.get()
                .uri("/states")
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .bodyToFlux(StateResponse.class)
                .flatMap(stateResponse -> Flux.fromIterable(stateResponse.getData()))
                .flatMap(stateData -> fetchGamesForState(stateData)
                        .collectList()
                        .map(games -> new LotteryState(stateData.getStateRegion(), games)))
                .retryWhen(Retry.backoff(3, Duration.ofSeconds(2))
                        .doBeforeRetry(retrySignal -> System.out.println("Retry attempt #" + (retrySignal.totalRetries() + 1)))
                        .filter(throwable ->
                                throwable instanceof ConnectException)
                        .onRetryExhaustedThrow((retryBackoffSpec, retrySignal) ->
                                new RuntimeException("Failed after max retries", retrySignal.failure())));
    }

    private Flux<LotteryGame> fetchGamesForState(StateResponse.StateData stateData) {
        return webClient.get()
                .uri("/states/{stateName}/games", stateData.getStateRegion())
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .bodyToFlux(LotteryGame.class);
    }

    private static class StateResponse {
        private List<StateData> data;

        // Getters and setters
        public List<StateData> getData() {
            return data;
        }

        public void setData(List<StateData> data) {
            this.data = data;
        }

        public static class StateData {
            private String stateRegion;

            // Getters and setters
            public String getStateRegion() {
                return stateRegion;
            }

            public void setStateRegion(String name) {
                this.stateRegion = name;
            }
        }
    }
}