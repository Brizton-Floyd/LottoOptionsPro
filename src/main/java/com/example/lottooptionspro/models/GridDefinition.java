package com.example.lottooptionspro.models;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class GridDefinition {
    
    // Convenience constructor without offset/spacing (defaults to 0)
    public GridDefinition(double topLeftX, double topLeftY, double bottomRightX, double bottomRightY,
                         int columns, int rows, int startNumber, int endNumber,
                         FillOrder fillOrder, String panelId) {
        this(topLeftX, topLeftY, bottomRightX, bottomRightY, columns, rows, startNumber, endNumber,
             fillOrder, panelId, 0.0, 0.0, 0.0, 0.0);
    }
    private double topLeftX;
    private double topLeftY;
    private double bottomRightX;
    private double bottomRightY;
    
    private int columns;
    private int rows;
    private int startNumber;
    private int endNumber;
    
    private FillOrder fillOrder;
    private String panelId;
    
    // Fine-tuning offset and spacing adjustments
    private double xOffset = 0.0;
    private double yOffset = 0.0;
    private double horizontalSpacing = 0.0; // Additional spacing between columns
    private double verticalSpacing = 0.0;   // Additional spacing between rows
    
    public double getCellWidth() {
        return (bottomRightX - topLeftX) / columns;
    }
    
    public double getCellHeight() {
        return (bottomRightY - topLeftY) / rows;
    }
    
    // Get adjusted cell width including horizontal spacing
    public double getAdjustedCellWidth() {
        return getCellWidth() + horizontalSpacing;
    }
    
    // Get adjusted cell height including vertical spacing  
    public double getAdjustedCellHeight() {
        return getCellHeight() + verticalSpacing;
    }
    
    // Get adjusted top-left X position including X offset
    public double getAdjustedTopLeftX() {
        return topLeftX + xOffset;
    }
    
    // Get adjusted top-left Y position including Y offset
    public double getAdjustedTopLeftY() {
        return topLeftY + yOffset;
    }
    
    public int getTotalNumbers() {
        return endNumber - startNumber + 1;
    }
    
    public boolean isValid() {
        return columns > 0 && rows > 0 && 
               startNumber > 0 && endNumber >= startNumber &&
               bottomRightX > topLeftX && bottomRightY > topLeftY;
    }
}