package com.example.lottooptionspro.service;

import com.example.lottooptionspro.model.deltapick.DeltaPickGenerationRequest;
import com.example.lottooptionspro.model.deltapick.DeltaPickGenerationResponse;
import com.example.lottooptionspro.model.deltapick.GameConfigResponse;
import com.google.gson.Gson;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;
import org.springframework.web.client.RestClientException;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for DeltaPickGenerationService following AAA (Arrange-Act-Assert) pattern.
 * These tests require the API server to be running at localhost:8002.
 */
@Tag("integration")
class DeltaPickGenerationServiceTest {

    private DeltaPickGenerationService sut;
    private Gson gson;

    @BeforeEach
    void setUp() {
        sut = new DeltaPickGenerationService(WebClient.builder().baseUrl("http://localhost:8002").build());
        gson = new Gson();
    }

    @Test
    void testFetchGameConfig_Success() {
        // Arrange
        String state = "Texas";
        String game = "Cash Five";

        // Act
        Mono<GameConfigResponse> result = sut.fetchGameConfig(state, game);

        // Assert
        StepVerifier.create(result)
                .assertNext(config -> {
                    assertNotNull(config, "Config should not be null");
                    assertNotNull(config.getMaxNumber(), "MaxNumber should not be null");
                    assertNotNull(config.getDrawPositionCount(), "DrawPositionCount should not be null");
                    assertTrue(config.getMaxNumber() > 0, "MaxNumber should be positive");
                    assertTrue(config.getDrawPositionCount() > 0, "DrawPositionCount should be positive");
                })
                .verifyComplete();
    }

    @Test
    void testFetchGameConfig_InvalidGame() {
        // Arrange
        String state = "InvalidState";
        String game = "InvalidGame";

        // Act
        Mono<GameConfigResponse> result = sut.fetchGameConfig(state, game);

        // Assert
        StepVerifier.create(result)
                .expectError(RestClientException.class)
                .verify();
    }

    @Test
    void testGenerateDeltaPicks_RawMode_Success() {
        // Arrange
        DeltaPickGenerationRequest request = new DeltaPickGenerationRequest();
        request.setLotteryState("Texas");
        request.setLotteryGame("Cash Five");
        request.setDeltaInputMode("RAW");
        request.setNumCombinations(20);
        
        Map<String, List<Integer>> rawDeltas = new HashMap<>();
        rawDeltas.put("D1", List.of(3, 13, 14, 15, 16, 17));
        rawDeltas.put("D2", List.of(3));
        rawDeltas.put("D3", List.of(3));
        rawDeltas.put("D4", List.of(3, 7, 9, 10));
        rawDeltas.put("D5", List.of(7, 13, 14, 15, 16, 17));
        request.setRawDeltas(rawDeltas);

        // Act
        Mono<DeltaPickGenerationResponse> result = sut.generateDeltaPicks(request);

        // Assert
        StepVerifier.create(result)
                .assertNext(response -> {
                    assertNotNull(response, "Response should not be null");
                    assertNotNull(response.getGeneratedPicks(), "Generated picks should not be null");
                    assertFalse(response.getGeneratedPicks().isEmpty(), "Should have generated picks");
                    assertEquals(20, response.getGeneratedPicks().size(), "Should generate requested number of picks");
                    assertNotNull(response.getConfiguration(), "Configuration should not be null");
                    assertEquals("RAW", response.getConfiguration().getDeltaInputMode(), "Mode should be RAW");
                })
                .verifyComplete();
    }

    @Test
    void testGenerateDeltaPicks_SortedMode_Success() {
        // Arrange
        DeltaPickGenerationRequest request = new DeltaPickGenerationRequest();
        request.setLotteryState("Texas");
        request.setLotteryGame("Cash Five");
        request.setDeltaInputMode("SORTED");
        request.setNumCombinations(20);
        
        Map<String, List<Integer>> sortedDeltas = new HashMap<>();
        sortedDeltas.put("S1", List.of(3));
        sortedDeltas.put("S2", List.of(3));
        sortedDeltas.put("S3", List.of(3));
        sortedDeltas.put("S4", List.of(7, 9, 10));
        sortedDeltas.put("S5", List.of(13, 14, 15, 16, 17));
        request.setSortedDeltaMagnitudes(sortedDeltas);

        // Act
        Mono<DeltaPickGenerationResponse> result = sut.generateDeltaPicks(request);

        // Assert
        StepVerifier.create(result)
                .assertNext(response -> {
                    assertNotNull(response, "Response should not be null");
                    assertNotNull(response.getGeneratedPicks(), "Generated picks should not be null");
                    assertFalse(response.getGeneratedPicks().isEmpty(), "Should have generated picks");
                    assertEquals(20, response.getGeneratedPicks().size(), "Should generate requested number of picks");
                    assertNotNull(response.getConfiguration(), "Configuration should not be null");
                    assertEquals("SORTED", response.getConfiguration().getDeltaInputMode(), "Mode should be SORTED");
                })
                .verifyComplete();
    }

    @Test
    void testGenerateDeltaPicks_ValidatesResponseStructure() {
        // Arrange
        DeltaPickGenerationRequest request = new DeltaPickGenerationRequest();
        request.setLotteryState("Texas");
        request.setLotteryGame("Cash Five");
        request.setDeltaInputMode("RAW");
        request.setNumCombinations(5);
        
        Map<String, List<Integer>> rawDeltas = new HashMap<>();
        rawDeltas.put("D1", List.of(3));
        rawDeltas.put("D2", List.of(3));
        rawDeltas.put("D3", List.of(3));
        rawDeltas.put("D4", List.of(7));
        rawDeltas.put("D5", List.of(13));
        request.setRawDeltas(rawDeltas);

        // Act
        Mono<DeltaPickGenerationResponse> result = sut.generateDeltaPicks(request);

        // Assert
        StepVerifier.create(result)
                .assertNext(response -> {
                    // Validate complete response structure
                    assertNotNull(response.getConfiguration(), "Configuration should exist");
                    assertNotNull(response.getGeneratedPicks(), "Generated picks should exist");
                    assertNotNull(response.getTotalValidCombinations(), "Total combinations should exist");
                    assertNotNull(response.getExecutionTimeMs(), "Execution time should exist");
                    assertNotNull(response.getMetadata(), "Metadata should exist");
                    assertNotNull(response.getHistoricalPerformance(), "Historical performance should exist");
                    
                    // Validate first pick structure
                    if (!response.getGeneratedPicks().isEmpty()) {
                        var firstPick = response.getGeneratedPicks().get(0);
                        assertNotNull(firstPick.getNumbers(), "Numbers should exist");
                        assertNotNull(firstPick.getRawDeltas(), "Raw deltas should exist");
                        assertNotNull(firstPick.getSortedDeltas(), "Sorted deltas should exist");
                        assertNotNull(firstPick.getProbabilityScore(), "Probability score should exist");
                        assertNotNull(firstPick.getRank(), "Rank should exist");
                        assertEquals(1, firstPick.getRank(), "First pick should have rank 1");
                    }
                })
                .verifyComplete();
    }

    @Test
    void testGenerateDeltaPicks_ApiError() {
        // Arrange
        DeltaPickGenerationRequest request = new DeltaPickGenerationRequest();
        request.setLotteryState("InvalidState");
        request.setLotteryGame("InvalidGame");
        request.setDeltaInputMode("RAW");
        request.setNumCombinations(10);

        // Act
        Mono<DeltaPickGenerationResponse> result = sut.generateDeltaPicks(request);

        // Assert
        StepVerifier.create(result)
                .expectError()
                .verify();
    }

    @Test
    void testGenerateDeltaPicks_EmptyDeltaInputs() {
        // Arrange
        DeltaPickGenerationRequest request = new DeltaPickGenerationRequest();
        request.setLotteryState("Texas");
        request.setLotteryGame("Cash Five");
        request.setDeltaInputMode("RAW");
        request.setNumCombinations(10);
        request.setRawDeltas(new HashMap<>()); // Empty deltas

        // Act
        Mono<DeltaPickGenerationResponse> result = sut.generateDeltaPicks(request);

        // Assert
        StepVerifier.create(result)
                .expectError()
                .verify();
    }

    @Test
    void testServiceCreation() {
        // Arrange & Act
        DeltaPickGenerationService service = new DeltaPickGenerationService(
                WebClient.builder().baseUrl("http://localhost:8002").build());

        // Assert
        assertNotNull(service, "Service should be created successfully");
    }
}
