package com.example.lottooptionspro.controller;

/**
 * Interface for controllers that can receive state and game context
 * from the MainController when they are loaded.
 */
public interface ContextAware {
    
    /**
     * Initialize the controller with state and game context.
     * This method is called by ScreenManager after the controller is loaded.
     *
     * @param stateName The state/jurisdiction name (e.g., "TEXAS", "Texas")
     * @param gameName The game name (e.g., "Cash Five", "Lotto Texas")
     */
    void initializeWithContext(String stateName, String gameName);
}
