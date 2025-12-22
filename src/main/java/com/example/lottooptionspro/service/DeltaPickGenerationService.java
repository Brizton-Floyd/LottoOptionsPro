package com.example.lottooptionspro.service;

import com.example.lottooptionspro.model.deltapick.DeltaPickGenerationRequest;
import com.example.lottooptionspro.model.deltapick.DeltaPickGenerationResponse;
import com.example.lottooptionspro.model.deltapick.GameConfigResponse;
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

/**
 * Service for generating delta-based lottery picks and fetching game configurations.
 */
@Service
public class DeltaPickGenerationService {
    
    private static final Logger logger = LoggerFactory.getLogger(DeltaPickGenerationService.class);
    private static final String API_BASE_URL = "http://localhost:8002/api/v1";
    
    private final RestTemplate restTemplate;
    private final Gson gson;
    
    public DeltaPickGenerationService() {
        this.restTemplate = new RestTemplate();
        this.gson = new GsonBuilder()
                .setPrettyPrinting()
                .create();
    }
    
    /**
     * Fetches game configuration including maxNumber and drawPositionCount.
     *
     * @param state the lottery state
     * @param game the lottery game name
     * @return Mono containing the game configuration
     */
    public Mono<GameConfigResponse> fetchGameConfig(String state, String game) {
        return Mono.fromCallable(() -> {
            try {
                String url = String.format("%s/lotto-options-core/games/%s/%s", 
                    API_BASE_URL, state, game);
                
                logger.info("Fetching game config for {}:{} from {}", state, game, url);
                
                String response = restTemplate.getForObject(url, String.class);
                
                if (response == null || response.trim().isEmpty()) {
                    throw new RuntimeException("Empty response from game config API");
                }
                
                logger.debug("API Response: {}", response);
                
                GameConfigResponse config = gson.fromJson(response, GameConfigResponse.class);
                
                if (config.getMaxNumber() == null || config.getDrawPositionCount() == null) {
                    throw new RuntimeException(String.format(
                        "Invalid game config: maxNumber=%s, drawPositionCount=%s", 
                        config.getMaxNumber(), config.getDrawPositionCount()));
                }
                
                logger.info("Game config loaded: maxNumber={}, drawPositionCount={}, game={}", 
                    config.getMaxNumber(), 
                    config.getDrawPositionCount(),
                    config.getLotteryGame() != null ? config.getLotteryGame().getFullName() : "unknown");
                
                return config;
                
            } catch (Exception e) {
                logger.error("Error fetching game config for {}:{}", state, game, e);
                throw new RuntimeException("Failed to fetch game configuration: " + e.getMessage(), e);
            }
        });
    }
    
    /**
     * Generates delta-based lottery picks using the specified constraints.
     *
     * @param request the delta pick generation request
     * @return Mono containing the generation response with picks and analysis
     */
    public Mono<DeltaPickGenerationResponse> generateDeltaPicks(DeltaPickGenerationRequest request) {
        return Mono.fromCallable(() -> {
            try {
                logger.info("Generating delta picks for {}:{} in {} mode with {} combinations", 
                    request.getLotteryState(), 
                    request.getLotteryGame(),
                    request.getDeltaInputMode(),
                    request.getNumCombinations());
                
                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_JSON);
                
                String requestJson = gson.toJson(request);
                logger.info("Request payload: {}", requestJson);
                logger.info("Include recent performance in request: {}", request.getIncludeRecentPerformance());
                logger.info("Lookback days in request: {}", request.getLookbackDays());
                logger.info("Posting to: {}", API_BASE_URL + "/analysis/generate/delta-picks");
                
                // Log delta details
                if ("RAW".equals(request.getDeltaInputMode())) {
                    logger.info("Raw deltas: {}", request.getRawDeltas());
                } else {
                    logger.info("Sorted delta magnitudes: {}", request.getSortedDeltaMagnitudes());
                }
                
                HttpEntity<String> entity = new HttpEntity<>(requestJson, headers);
                
                String response = restTemplate.postForObject(
                    API_BASE_URL + "/analysis/generate/delta-picks",
                    entity,
                    String.class
                );
                
                if (response == null || response.trim().isEmpty()) {
                    throw new RuntimeException("Empty response from delta picks generation API");
                }
                
                logger.debug("API Response: {}", response);
                
                DeltaPickGenerationResponse result = gson.fromJson(response, DeltaPickGenerationResponse.class);
                
                logger.info("Delta picks generated successfully. Total picks: {}, Execution time: {}ms", 
                    result.getGeneratedPicks().size(), 
                    result.getExecutionTimeMs());
                
                return result;
                
            } catch (Exception e) {
                logger.error("Error generating delta picks. Request was: {}", gson.toJson(request), e);
                throw new RuntimeException("Failed to generate delta picks: " + e.getMessage(), e);
            }
        });
    }
}
