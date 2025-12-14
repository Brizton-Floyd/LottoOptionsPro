package com.example.lottooptionspro.controller;

import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;

import java.util.Map;

/**
 * Table row class for draw results in Range Analysis
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
    
    public String getDrawDate() {
        return drawDate.get();
    }
    
    public SimpleStringProperty drawDateProperty() {
        return drawDate;
    }
    
    public int getHitCountForRange(String range) {
        return rangeHits.getOrDefault(range, 0);
    }
    
    public int getTotalHits() {
        return totalHits.get();
    }
    
    public SimpleIntegerProperty totalHitsProperty() {
        return totalHits;
    }
}
