package com.example.lottooptionspro.models;

/**
 * Grid configuration settings for a template
 * These settings are saved/loaded with the template JSON
 */
public class GridConfiguration {
    private String numberRange;    // e.g., "1-35" or "1-54"
    private int columns;           // Number of columns in the grid
    private int rows;              // Number of rows in the grid
    private String fillOrder;      // Fill pattern: "COLUMN_BOTTOM_TO_TOP", "ROW_LEFT_TO_RIGHT", etc.
    
    // Default constructor
    public GridConfiguration() {}
    
    // Full constructor
    public GridConfiguration(String numberRange, int columns, int rows, String fillOrder) {
        this.numberRange = numberRange;
        this.columns = columns;
        this.rows = rows;
        this.fillOrder = fillOrder;
    }
    
    // Constructor with default fill order
    public GridConfiguration(String numberRange, int columns, int rows) {
        this.numberRange = numberRange;
        this.columns = columns;
        this.rows = rows;
        this.fillOrder = "COLUMN_BOTTOM_TO_TOP"; // Default
    }
    
    // Getters and setters
    public String getNumberRange() { return numberRange; }
    public void setNumberRange(String numberRange) { this.numberRange = numberRange; }
    
    public int getColumns() { return columns; }
    public void setColumns(int columns) { this.columns = columns; }
    
    public int getRows() { return rows; }
    public void setRows(int rows) { this.rows = rows; }
    
    public String getFillOrder() { return fillOrder; }
    public void setFillOrder(String fillOrder) { this.fillOrder = fillOrder; }
    
    /**
     * Check if this grid configuration is valid
     */
    public boolean isValid() {
        return numberRange != null && !numberRange.trim().isEmpty() && 
               columns > 0 && rows > 0 && 
               fillOrder != null && !fillOrder.trim().isEmpty();
    }
    
    @Override
    public String toString() {
        return "GridConfiguration{" +
               "numberRange='" + numberRange + '\'' +
               ", columns=" + columns +
               ", rows=" + rows +
               ", fillOrder='" + fillOrder + '\'' +
               '}';
    }
}