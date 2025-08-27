package com.example.lottooptionspro.models;

public enum GridMappingMode {
    NORMAL("Normal mode - click to place individual numbers"),
    DEFINING_FIRST_CORNER("Click the first corner of the number grid"),
    DEFINING_SECOND_CORNER("Click the opposite corner to complete the grid"),
    GRID_PREVIEW("Preview the grid layout - adjust if needed"),
    GRID_MAPPED("Grid mapping complete - numbers are placed");
    
    private final String statusMessage;
    
    GridMappingMode(String statusMessage) {
        this.statusMessage = statusMessage;
    }
    
    public String getStatusMessage() {
        return statusMessage;
    }
}