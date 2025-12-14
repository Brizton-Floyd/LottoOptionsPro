package com.example.lottooptionspro.presenter;

import com.example.lottooptionspro.model.deltapick.DeltaPickGenerationRequest;
import com.example.lottooptionspro.model.deltapick.DeltaPickGenerationResponse;
import com.example.lottooptionspro.model.deltapick.GameConfigResponse;
import com.example.lottooptionspro.service.DeltaPickGenerationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Unit tests for DeltaPickGeneratorPresenter following AAA pattern.
 */
class DeltaPickGeneratorPresenterTest {

    private DeltaPickGeneratorPresenter sut;
    
    @Mock
    private DeltaPickGenerationService mockService;
    
    @Mock
    private DeltaPickGeneratorView mockView;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        sut = new DeltaPickGeneratorPresenter(mockService);
        sut.setView(mockView);
    }

    @Test
    void testLoadGameConfiguration_Success() {
        // Arrange
        String state = "Texas";
        String game = "Cash Five";
        
        GameConfigResponse expectedConfig = new GameConfigResponse();
        expectedConfig.setMaxNumber(35);
        expectedConfig.setDrawPositionCount(5);
        
        when(mockService.fetchGameConfig(state, game))
            .thenReturn(Mono.just(expectedConfig));

        // Act
        Mono<GameConfigResponse> result = sut.loadGameConfiguration(state, game);

        // Assert
        StepVerifier.create(result)
                .assertNext(config -> {
                    assertNotNull(config, "Config should not be null");
                    assertEquals(35, config.getMaxNumber(), "MaxNumber should be 35");
                    assertEquals(5, config.getDrawPositionCount(), "DrawPositionCount should be 5");
                })
                .verifyComplete();
        
        verify(mockService, times(1)).fetchGameConfig(state, game);
    }

    @Test
    void testLoadGameConfiguration_ServiceError() {
        // Arrange
        String state = "Texas";
        String game = "InvalidGame";
        
        when(mockService.fetchGameConfig(state, game))
            .thenReturn(Mono.error(new RuntimeException("Game not found")));

        // Act
        Mono<GameConfigResponse> result = sut.loadGameConfiguration(state, game);

        // Assert
        StepVerifier.create(result)
                .expectErrorMessage("Game not found")
                .verify();
        
        verify(mockService, times(1)).fetchGameConfig(state, game);
    }

    @Test
    void testGeneratePicks_Success() {
        // Arrange
        DeltaPickGenerationRequest request = new DeltaPickGenerationRequest();
        request.setLotteryState("Texas");
        request.setLotteryGame("Cash Five");
        request.setDeltaInputMode("RAW");
        request.setNumCombinations(10);
        
        DeltaPickGenerationResponse expectedResponse = new DeltaPickGenerationResponse();
        
        when(mockService.generateDeltaPicks(any(DeltaPickGenerationRequest.class)))
            .thenReturn(Mono.just(expectedResponse));

        // Act
        Mono<DeltaPickGenerationResponse> result = sut.generatePicks(request);

        // Assert
        StepVerifier.create(result)
                .assertNext(response -> {
                    assertNotNull(response, "Response should not be null");
                    assertSame(expectedResponse, response, "Should return the expected response");
                })
                .verifyComplete();
        
        verify(mockService, times(1)).generateDeltaPicks(request);
    }

    @Test
    void testGeneratePicks_ServiceError() {
        // Arrange
        DeltaPickGenerationRequest request = new DeltaPickGenerationRequest();
        request.setLotteryState("Texas");
        request.setLotteryGame("Cash Five");
        
        when(mockService.generateDeltaPicks(any(DeltaPickGenerationRequest.class)))
            .thenReturn(Mono.error(new RuntimeException("API error")));

        // Act
        Mono<DeltaPickGenerationResponse> result = sut.generatePicks(request);

        // Assert
        StepVerifier.create(result)
                .expectErrorMessage("API error")
                .verify();
        
        verify(mockService, times(1)).generateDeltaPicks(request);
    }

    @Test
    void testSetView() {
        // Arrange
        DeltaPickGeneratorView newView = mock(DeltaPickGeneratorView.class);

        // Act
        sut.setView(newView);

        // Assert
        assertNotNull(sut, "Presenter should not be null after setting view");
    }

    @Test
    void testPresenterCreation() {
        // Arrange & Act
        DeltaPickGeneratorPresenter presenter = new DeltaPickGeneratorPresenter(mockService);

        // Assert
        assertNotNull(presenter, "Presenter should be created successfully");
    }

    @Test
    void testLoadGameConfiguration_NullParameters() {
        // Arrange
        when(mockService.fetchGameConfig(null, null))
            .thenReturn(Mono.error(new IllegalArgumentException("State and game cannot be null")));

        // Act
        Mono<GameConfigResponse> result = sut.loadGameConfiguration(null, null);

        // Assert
        StepVerifier.create(result)
                .expectError(IllegalArgumentException.class)
                .verify();
    }

    @Test
    void testGeneratePicks_NullRequest() {
        // Arrange
        when(mockService.generateDeltaPicks(null))
            .thenReturn(Mono.error(new IllegalArgumentException("Request cannot be null")));

        // Act
        Mono<DeltaPickGenerationResponse> result = sut.generatePicks(null);

        // Assert
        StepVerifier.create(result)
                .expectError(IllegalArgumentException.class)
                .verify();
    }
}
