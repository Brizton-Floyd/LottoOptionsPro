package com.example.lottooptionspro.presenter;

import com.example.lottooptionspro.model.deltapick.DeltaPickGenerationResponse;
import com.example.lottooptionspro.model.deltapick.GameConfigResponse;

/**
 * View interface for the Delta Pick Generator.
 * Defines the contract between the presenter and the view (controller).
 */
public interface DeltaPickGeneratorView {
    
    /**
     * Displays the generated picks in the UI.
     *
     * @param response the complete response containing generated picks and analysis
     */
    void displayGeneratedPicks(DeltaPickGenerationResponse response);
    
    /**
     * Shows an error message to the user.
     *
     * @param message the error message to display
     */
    void showError(String message);
    
    /**
     * Shows or hides the loading indicator.
     *
     * @param show true to show loading, false to hide
     */
    void showLoading(boolean show);
    
    /**
     * Updates the configuration panel with game settings.
     *
     * @param config the game configuration containing maxNumber and drawPositionCount
     */
    void updateConfigurationPanel(GameConfigResponse config);
}
