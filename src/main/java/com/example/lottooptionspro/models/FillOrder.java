package com.example.lottooptionspro.models;

public enum FillOrder {
    COLUMN_BOTTOM_TO_TOP("Column-wise (bottom → top)", "Numbers fill vertically from bottom to top, then move to next column"),
    COLUMN_TOP_TO_BOTTOM("Column-wise (top → bottom)", "Numbers fill vertically from top to bottom, then move to next column"),
    ROW_LEFT_TO_RIGHT("Row-wise (left → right)", "Numbers fill horizontally from left to right, then move to next row"),
    ROW_RIGHT_TO_LEFT("Row-wise (right → left)", "Numbers fill horizontally from right to left, then move to next row");
    
    private final String displayName;
    private final String description;
    
    FillOrder(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    public String getDescription() {
        return description;
    }
    
    @Override
    public String toString() {
        return displayName;
    }
}