package com.example.lottooptionspro.service;

import com.example.lottooptionspro.model.cache.DeltaSelectionCache;
import com.example.lottooptionspro.model.deltapick.DeltaInputMode;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.MapChangeListener;
import javafx.collections.ObservableList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Service for managing delta selections across views using JavaFX observable properties.
 * Thread-safe and provides automatic UI updates through property bindings.
 */
@Service
public class DeltaSelectionCacheService {
    
    private static final Logger logger = LoggerFactory.getLogger(DeltaSelectionCacheService.class);
    private static final int CACHE_FRESHNESS_MINUTES = 30;
    
    private final DeltaSelectionCache cache = new DeltaSelectionCache();
    private final List<CacheUpdateListener> listeners = new ArrayList<>();
    
    public DeltaSelectionCacheService() {
        // Listen to map changes
        cache.getPositionSelections().addListener((MapChangeListener<String, ObservableList<Integer>>) change -> {
            Platform.runLater(() -> {
                cache.updateTimestamp();
                cache.updateFilledPositionsCount();
                notifyListeners(new CacheUpdateEvent(change.getKey(), 
                    change.wasAdded() ? change.getValueAdded() : null));
            });
        });
        
        logger.info("DeltaSelectionCacheService initialized");
    }
    
    /**
     * Get the observable cache instance for direct binding.
     */
    public DeltaSelectionCache getCache() {
        return cache;
    }
    
    /**
     * Initialize cache for a specific game.
     */
    public void initializeForGame(String state, String game, int numPositions) {
        Platform.runLater(() -> {
            cache.setState(state);
            cache.setGame(game);
            cache.updateTimestamp();
            cache.checkCompleteness(numPositions);
            logger.info("Cache initialized for {}:{} with {} positions", state, game, numPositions);
        });
    }
    
    /**
     * Update or add selection for a position.
     */
    public void updatePosition(String position, List<Integer> values) {
        Platform.runLater(() -> {
            ObservableList<Integer> observableValues = FXCollections.observableArrayList(values);
            
            // Add listener to the list for granular updates
            observableValues.addListener((ListChangeListener<Integer>) change -> {
                cache.updateTimestamp();
                logger.debug("Position {} values changed", position);
            });
            
            cache.getPositionSelections().put(position, observableValues);
            cache.updateFilledPositionsCount();
            
            logger.info("Updated position {} with {} values", position, values.size());
        });
    }
    
    /**
     * Append values to existing position.
     */
    public void appendToPosition(String position, List<Integer> additionalValues) {
        Platform.runLater(() -> {
            ObservableList<Integer> existing = cache.getPositionSelections().get(position);
            if (existing != null) {
                existing.addAll(additionalValues);
            } else {
                updatePosition(position, additionalValues);
            }
            logger.info("Appended {} values to position {}", additionalValues.size(), position);
        });
    }
    
    /**
     * Remove specific values from a position.
     */
    public void removeFromPosition(String position, List<Integer> valuesToRemove) {
        Platform.runLater(() -> {
            ObservableList<Integer> existing = cache.getPositionSelections().get(position);
            if (existing != null) {
                existing.removeAll(valuesToRemove);
                cache.updateTimestamp();
                logger.info("Removed {} values from position {}", valuesToRemove.size(), position);
            }
        });
    }
    
    /**
     * Clear a specific position.
     */
    public void clearPosition(String position) {
        Platform.runLater(() -> {
            cache.getPositionSelections().remove(position);
            cache.updateFilledPositionsCount();
            logger.info("Cleared position {}", position);
        });
    }
    
    /**
     * Clear all positions.
     */
    public void clearAll() {
        Platform.runLater(() -> {
            cache.getPositionSelections().clear();
            cache.updateFilledPositionsCount();
            cache.checkCompleteness(0);
            logger.info("Cleared all cache data");
        });
    }
    
    /**
     * Set delta input mode.
     */
    public void setMode(DeltaInputMode mode) {
        Platform.runLater(() -> {
            DeltaInputMode oldMode = cache.getMode();
            cache.setMode(mode);
            
            // Convert position keys if mode changed
            if (oldMode != mode) {
                convertPositionKeys(oldMode, mode);
            }
            
            logger.info("Mode changed from {} to {}", oldMode, mode);
        });
    }
    
    /**
     * Convert position keys when mode changes (D1 ↔ S1).
     */
    private void convertPositionKeys(DeltaInputMode from, DeltaInputMode to) {
        Map<String, ObservableList<Integer>> converted = new HashMap<>();
        String fromPrefix = from == DeltaInputMode.RAW ? "D" : "S";
        String toPrefix = to == DeltaInputMode.RAW ? "D" : "S";
        
        cache.getPositionSelections().forEach((key, value) -> {
            if (key.startsWith(fromPrefix)) {
                String newKey = key.replace(fromPrefix, toPrefix);
                converted.put(newKey, value);
            } else {
                converted.put(key, value);
            }
        });
        
        cache.getPositionSelections().clear();
        cache.getPositionSelections().putAll(converted);
    }
    
    /**
     * Check if cache exists for the given game.
     */
    public boolean hasCacheFor(String state, String game) {
        return state.equals(cache.getState()) && game.equals(cache.getGame()) 
            && !cache.getPositionSelections().isEmpty();
    }
    
    /**
     * Check if cache is fresh (within freshness window).
     */
    public boolean isCacheFresh() {
        if (cache.getTimestamp() == null) return false;
        Duration age = Duration.between(cache.getTimestamp(), LocalDateTime.now());
        return age.toMinutes() < CACHE_FRESHNESS_MINUTES;
    }
    
    /**
     * Get cache age in minutes.
     */
    public long getCacheAgeMinutes() {
        if (cache.getTimestamp() == null) return Long.MAX_VALUE;
        return Duration.between(cache.getTimestamp(), LocalDateTime.now()).toMinutes();
    }
    
    /**
     * Add a listener for cache updates.
     */
    public void addListener(CacheUpdateListener listener) {
        listeners.add(listener);
    }
    
    /**
     * Remove a listener.
     */
    public void removeListener(CacheUpdateListener listener) {
        listeners.remove(listener);
    }
    
    private void notifyListeners(CacheUpdateEvent event) {
        listeners.forEach(listener -> listener.onCacheUpdated(event));
    }
    
    /**
     * Listener interface for cache updates.
     */
    public interface CacheUpdateListener {
        void onCacheUpdated(CacheUpdateEvent event);
    }
    
    /**
     * Event object for cache updates.
     */
    public static class CacheUpdateEvent {
        private final String position;
        private final ObservableList<Integer> newValues;
        private final LocalDateTime timestamp;
        
        public CacheUpdateEvent(String position, ObservableList<Integer> newValues) {
            this.position = position;
            this.newValues = newValues;
            this.timestamp = LocalDateTime.now();
        }
        
        public String getPosition() { return position; }
        public ObservableList<Integer> getNewValues() { return newValues; }
        public LocalDateTime getTimestamp() { return timestamp; }
    }
}
