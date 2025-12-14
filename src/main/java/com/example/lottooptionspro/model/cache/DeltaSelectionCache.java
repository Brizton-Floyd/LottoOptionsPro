package com.example.lottooptionspro.model.cache;

import com.example.lottooptionspro.model.deltapick.DeltaInputMode;
import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.ObservableMap;

import java.time.LocalDateTime;

/**
 * Observable cache for delta selections across views.
 * Uses JavaFX properties for automatic UI synchronization.
 */
public class DeltaSelectionCache {
    
    // Game context
    private final StringProperty state = new SimpleStringProperty();
    private final StringProperty game = new SimpleStringProperty();
    
    // Delta selections (position -> values)
    // e.g., "D1" -> [3, 7, 13]
    private final ObservableMap<String, ObservableList<Integer>> positionSelections = 
        FXCollections.observableHashMap();
    
    // Delta input mode (RAW or SORTED)
    private final ObjectProperty<DeltaInputMode> mode = 
        new SimpleObjectProperty<>(DeltaInputMode.RAW);
    
    // Metadata
    private final ObjectProperty<LocalDateTime> timestamp = 
        new SimpleObjectProperty<>(LocalDateTime.now());
    
    private final BooleanProperty complete = new SimpleBooleanProperty(false);
    
    private final IntegerProperty filledPositions = new SimpleIntegerProperty(0);
    
    // Getters for properties (for binding)
    public StringProperty stateProperty() { return state; }
    public StringProperty gameProperty() { return game; }
    public ObjectProperty<DeltaInputMode> modeProperty() { return mode; }
    public ObjectProperty<LocalDateTime> timestampProperty() { return timestamp; }
    public BooleanProperty completeProperty() { return complete; }
    public IntegerProperty filledPositionsProperty() { return filledPositions; }
    
    // Getters/Setters
    public String getState() { return state.get(); }
    public void setState(String state) { this.state.set(state); }
    
    public String getGame() { return game.get(); }
    public void setGame(String game) { this.game.set(game); }
    
    public DeltaInputMode getMode() { return mode.get(); }
    public void setMode(DeltaInputMode mode) { 
        this.mode.set(mode);
        updateTimestamp();
    }
    
    public LocalDateTime getTimestamp() { return timestamp.get(); }
    
    public boolean isComplete() { return complete.get(); }
    
    public int getFilledPositions() { return filledPositions.get(); }
    
    public ObservableMap<String, ObservableList<Integer>> getPositionSelections() {
        return positionSelections;
    }
    
    // Helper methods
    public void updateTimestamp() {
        timestamp.set(LocalDateTime.now());
    }
    
    public void updateFilledPositionsCount() {
        long count = positionSelections.values().stream()
            .filter(list -> list != null && !list.isEmpty())
            .count();
        filledPositions.set((int) count);
    }
    
    public void checkCompleteness(int expectedPositions) {
        complete.set(filledPositions.get() >= expectedPositions);
    }
}
