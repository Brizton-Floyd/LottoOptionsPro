package com.example.lottooptionspro.model.range;

import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;

import java.util.Map;

/**
 * Table row model for displaying draw results in the Range Analysis view.
 * Each row represents a single draw with date and hit counts for each range.
 */
public class DrawResultTableRow {
    
    private final SimpleStringProperty drawDate;
    private final Map<String, Integer> rangeHits;
    private final SimpleIntegerProperty totalHits;
    
    public DrawResultTableRow(String drawDate, Map<String, Integer> rangeHits, int totalHits) {
        this.drawDate = new SimpleStringProperty(drawDate);
        this.rangeHits = rangeHits;
        this.totalHits = new SimpleIntegerProperty(totalHits);
    }
    
    // Draw Date property
    public String getDrawDate() {
        return drawDate.get();
    }
    
    public SimpleStringProperty drawDateProperty() {
        return drawDate;
    }
    
    public void setDrawDate(String drawDate) {
        this.drawDate.set(drawDate);
    }
    
    // Range Hits
    public Map<String, Integer> getRangeHits() {
        return rangeHits;
    }
    
    /**
     * Get hit count for a specific range
     * @param range The range key (e.g., "1-5", "6-10")
     * @return Hit count for that range, or 0 if not found
     */
    public int getHitCountForRange(String range) {
        return rangeHits.getOrDefault(range, 0);
    }
    
    /**
     * Get hit count as property for JavaFX binding
     * @param range The range key
     * @return SimpleIntegerProperty for the hit count
     */
    public SimpleIntegerProperty getHitCountPropertyForRange(String range) {
        return new SimpleIntegerProperty(getHitCountForRange(range));
    }
    
    // Total Hits property
    public int getTotalHits() {
        return totalHits.get();
    }
    
    public SimpleIntegerProperty totalHitsProperty() {
        return totalHits;
    }
    
    public void setTotalHits(int totalHits) {
        this.totalHits.set(totalHits);
    }
    
    @Override
    public String toString() {
        return "DrawResultTableRow{" +
                "drawDate='" + drawDate.get() + '\'' +
                ", totalHits=" + totalHits.get() +
                ", rangeCount=" + rangeHits.size() +
                '}';
    }
}