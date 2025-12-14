package com.example.lottooptionspro.integration;

import com.example.lottooptionspro.model.deltapick.DeltaPickGenerationRequest;
import com.example.lottooptionspro.model.deltapick.DeltaPickGenerationResponse;
import com.example.lottooptionspro.model.deltapick.GameConfigResponse;
import com.example.lottooptionspro.service.DeltaPickGenerationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for the complete Delta Pick Generator workflow.
 * These tests require the API server to be running at localhost:8002.
 */
@Tag("integration")
class DeltaPickGeneratorIntegrationTest {

    private DeltaPickGenerationService service;

    @BeforeEach
    void setUp() {
        service = new DeltaPickGenerationService();
    }

    @Test
    void testFullWorkflow_RawMode() {
        // Arrange - Step 1: Fetch game configuration
        String state = "Texas";
        String game = "Cash Five";

        // Act & Assert - Step 1
        Mono<GameConfigResponse> configMono = service.fetchGameConfig(state, game);
        
        StepVerifier.create(configMono)
                .assertNext(config -> {
                    assertNotNull(config, "Config should not be null");
                    assertEquals(35, config.getMaxNumber(), "Max number should be 35");
                    assertEquals(5, config.getDrawPositionCount(), "Draw position count should be 5");
                })
                .verifyComplete();

        // Arrange - Step 2: Build request with RAW mode
        DeltaPickGenerationRequest request = new DeltaPickGenerationRequest();
        request.setLotteryState(state);
        request.setLotteryGame(game);
        request.setDeltaInputMode("RAW");
        request.setNumCombinations(10);
        
        Map<String, List<Integer>> rawDeltas = new HashMap<>();
        rawDeltas.put("D1", List.of(3));
        rawDeltas.put("D2", List.of(3));
        rawDeltas.put("D3", List.of(3));
        rawDeltas.put("D4", List.of(7));
        rawDeltas.put("D5", List.of(13));
        request.setRawDeltas(rawDeltas);

        // Act & Assert - Step 2: Generate picks
        Mono<DeltaPickGenerationResponse> responseMono = service.generateDeltaPicks(request);
        
        StepVerifier.create(responseMono)
                .assertNext(response -> {
                    // Validate response structure
                    assertNotNull(response, "Response should not be null");
                    assertNotNull(response.getConfiguration(), "Configuration should exist");
                    assertNotNull(response.getGeneratedPicks(), "Generated picks should exist");
                    assertFalse(response.getGeneratedPicks().isEmpty(), "Should have generated picks");
                    
                    // Validate configuration
                    assertEquals("RAW", response.getConfiguration().getDeltaInputMode());
                    assertEquals(state, response.getConfiguration().getLotteryState());
                    assertEquals(game, response.getConfiguration().getLotteryGame());
                    
                    // Validate picks
                    assertTrue(response.getGeneratedPicks().size() <= 10, "Should not exceed requested combinations");
                    
                    // Validate first pick structure
                    var firstPick = response.getGeneratedPicks().get(0);
                    assertNotNull(firstPick.getNumbers(), "Numbers should exist");
                    assertNotNull(firstPick.getRawDeltas(), "Raw deltas should exist");
                    assertNotNull(firstPick.getSortedDeltas(), "Sorted deltas should exist");
                    assertNotNull(firstPick.getProbabilityScore(), "Probability score should exist");
                    assertNotNull(firstPick.getRank(), "Rank should exist");
                    assertEquals(1, firstPick.getRank(), "First pick should have rank 1");
                    
                    // Validate metadata
                    assertNotNull(response.getMetadata(), "Metadata should exist");
                    assertNotNull(response.getExecutionTimeMs(), "Execution time should exist");
                    assertTrue(response.getExecutionTimeMs() >= 0, "Execution time should be non-negative");
                    
                    // Validate historical performance
                    assertNotNull(response.getHistoricalPerformance(), "Historical performance should exist");
                })
                .verifyComplete();
    }

    @Test
    void testFullWorkflow_SortedMode() {
        // Arrange - Build request with SORTED mode
        DeltaPickGenerationRequest request = new DeltaPickGenerationRequest();
        request.setLotteryState("Texas");
        request.setLotteryGame("Cash Five");
        request.setDeltaInputMode("SORTED");
        request.setNumCombinations(10);
        
        Map<String, List<Integer>> sortedDeltas = new HashMap<>();
        sortedDeltas.put("S1", List.of(3));
        sortedDeltas.put("S2", List.of(3));
        sortedDeltas.put("S3", List.of(3));
        sortedDeltas.put("S4", List.of(7));
        sortedDeltas.put("S5", List.of(13));
        request.setSortedDeltaMagnitudes(sortedDeltas);

        // Act
        Mono<DeltaPickGenerationResponse> responseMono = service.generateDeltaPicks(request);
        
        // Assert
        StepVerifier.create(responseMono)
                .assertNext(response -> {
                    assertNotNull(response, "Response should not be null");
                    assertEquals("SORTED", response.getConfiguration().getDeltaInputMode());
                    assertFalse(response.getGeneratedPicks().isEmpty(), "Should have generated picks");
                })
                .verifyComplete();
    }

    @Test
    void testFullWorkflow_MultipleDeltas() {
        // Arrange - Build request with multiple delta values per position
        DeltaPickGenerationRequest request = new DeltaPickGenerationRequest();
        request.setLotteryState("Texas");
        request.setLotteryGame("Cash Five");
        request.setDeltaInputMode("RAW");
        request.setNumCombinations(20);
        
        Map<String, List<Integer>> rawDeltas = new HashMap<>();
        rawDeltas.put("D1", List.of(3, 13, 14));
        rawDeltas.put("D2", List.of(3));
        rawDeltas.put("D3", List.of(3));
        rawDeltas.put("D4", List.of(3, 7, 9));
        rawDeltas.put("D5", List.of(7, 13, 14));
        request.setRawDeltas(rawDeltas);

        // Act
        Mono<DeltaPickGenerationResponse> responseMono = service.generateDeltaPicks(request);
        
        // Assert
        StepVerifier.create(responseMono)
                .assertNext(response -> {
                    assertNotNull(response, "Response should not be null");
                    assertTrue(response.getGeneratedPicks().size() <= 20, "Should not exceed requested combinations");
                    assertTrue(response.getTotalValidCombinations() > 0, "Should have valid combinations");
                    
                    // Verify picks are ranked correctly
                    for (int i = 0; i < response.getGeneratedPicks().size() - 1; i++) {
                        int currentRank = response.getGeneratedPicks().get(i).getRank();
                        int nextRank = response.getGeneratedPicks().get(i + 1).getRank();
                        assertTrue(currentRank <= nextRank, "Picks should be in rank order");
                    }
                })
                .verifyComplete();
    }

    @Test
    void testEdgeCase_SingleDeltaValue() {
        // Arrange - Request with only one delta position specified
        DeltaPickGenerationRequest request = new DeltaPickGenerationRequest();
        request.setLotteryState("Texas");
        request.setLotteryGame("Cash Five");
        request.setDeltaInputMode("RAW");
        request.setNumCombinations(5);
        
        Map<String, List<Integer>> rawDeltas = new HashMap<>();
        rawDeltas.put("D1", List.of(3));
        request.setRawDeltas(rawDeltas);

        // Act
        Mono<DeltaPickGenerationResponse> responseMono = service.generateDeltaPicks(request);
        
        // Assert
        StepVerifier.create(responseMono)
                .assertNext(response -> {
                    assertNotNull(response, "Response should not be null");
                    // Should still generate picks even with minimal input
                    assertFalse(response.getGeneratedPicks().isEmpty(), "Should generate picks");
                })
                .verifyComplete();
    }

    @Test
    void testEdgeCase_LargeNumberOfCombinations() {
        // Arrange - Request large number of combinations
        DeltaPickGenerationRequest request = new DeltaPickGenerationRequest();
        request.setLotteryState("Texas");
        request.setLotteryGame("Cash Five");
        request.setDeltaInputMode("RAW");
        request.setNumCombinations(100);
        
        Map<String, List<Integer>> rawDeltas = new HashMap<>();
        rawDeltas.put("D1", List.of(3, 5, 7));
        rawDeltas.put("D2", List.of(3, 5, 7));
        rawDeltas.put("D3", List.of(3, 5, 7));
        rawDeltas.put("D4", List.of(7, 9, 11));
        rawDeltas.put("D5", List.of(13, 15, 17));
        request.setRawDeltas(rawDeltas);

        // Act
        Mono<DeltaPickGenerationResponse> responseMono = service.generateDeltaPicks(request);
        
        // Assert
        StepVerifier.create(responseMono)
                .assertNext(response -> {
                    assertNotNull(response, "Response should not be null");
                    assertTrue(response.getGeneratedPicks().size() <= 100, "Should not exceed requested");
                    assertTrue(response.getExecutionTimeMs() > 0, "Should have execution time");
                })
                .verifyComplete();
    }

    @Test
    void testValidation_InvalidState() {
        // Arrange
        DeltaPickGenerationRequest request = new DeltaPickGenerationRequest();
        request.setLotteryState("InvalidState");
        request.setLotteryGame("InvalidGame");
        request.setDeltaInputMode("RAW");
        request.setNumCombinations(10);

        // Act
        Mono<DeltaPickGenerationResponse> responseMono = service.generateDeltaPicks(request);
        
        // Assert
        StepVerifier.create(responseMono)
                .expectError()
                .verify();
    }

    @Test
    void testValidation_EmptyDeltas() {
        // Arrange
        DeltaPickGenerationRequest request = new DeltaPickGenerationRequest();
        request.setLotteryState("Texas");
        request.setLotteryGame("Cash Five");
        request.setDeltaInputMode("RAW");
        request.setNumCombinations(10);
        request.setRawDeltas(new HashMap<>()); // Empty deltas

        // Act
        Mono<DeltaPickGenerationResponse> responseMono = service.generateDeltaPicks(request);
        
        // Assert
        StepVerifier.create(responseMono)
                .expectError()
                .verify();
    }
}
