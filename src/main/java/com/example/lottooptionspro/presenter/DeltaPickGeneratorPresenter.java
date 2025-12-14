package com.example.lottooptionspro.presenter;

import com.example.lottooptionspro.model.deltapick.DeltaPickGenerationRequest;
import com.example.lottooptionspro.model.deltapick.DeltaPickGenerationResponse;
import com.example.lottooptionspro.model.deltapick.GameConfigResponse;
import com.example.lottooptionspro.service.DeltaPickGenerationService;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

/**
 * Presenter for the Delta Pick Generator feature.
 * Mediates between the view and the service layer.
 */
@Component
public class DeltaPickGeneratorPresenter {
    
    private final DeltaPickGenerationService service;
    private DeltaPickGeneratorView view;
    
    public DeltaPickGeneratorPresenter(DeltaPickGenerationService service) {
        this.service = service;
    }
    
    /**
     * Sets the view for this presenter.
     *
     * @param view the view implementation
     */
    public void setView(DeltaPickGeneratorView view) {
        this.view = view;
    }
    
    /**
     * Loads the game configuration for the specified state and game.
     *
     * @param state the lottery state
     * @param game the lottery game name
     * @return Mono containing the game configuration
     */
    public Mono<GameConfigResponse> loadGameConfiguration(String state, String game) {
        return service.fetchGameConfig(state, game);
    }
    
    /**
     * Generates delta-based lottery picks using the provided request.
     *
     * @param request the delta pick generation request
     * @return Mono containing the generation response
     */
    public Mono<DeltaPickGenerationResponse> generatePicks(DeltaPickGenerationRequest request) {
        return service.generateDeltaPicks(request);
    }
}
