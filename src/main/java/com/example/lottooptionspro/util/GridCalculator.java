package com.example.lottooptionspro.util;

import com.example.lottooptionspro.models.Coordinate;
import com.example.lottooptionspro.models.FillOrder;
import com.example.lottooptionspro.models.GridDefinition;
import com.example.lottooptionspro.models.Mark;

import java.util.*;
import java.util.stream.Collectors;

public class GridCalculator {
    
    /**
     * Get the column index for a given number based on grid definition
     */
    public static int getColumnForNumber(int number, GridDefinition grid) {
        if (!grid.isValid() || number < grid.getStartNumber() || number > grid.getEndNumber()) {
            return -1;
        }
        
        int index = number - grid.getStartNumber();
        
        switch (grid.getFillOrder()) {
            case COLUMN_BOTTOM_TO_TOP:
            case COLUMN_TOP_TO_BOTTOM:
                return index / grid.getRows();
                
            case ROW_LEFT_TO_RIGHT:
            case ROW_RIGHT_TO_LEFT:
                return index % grid.getColumns();
                
            default:
                return -1;
        }
    }
    
    /**
     * Get the row index for a given number based on grid definition
     */
    public static int getRowForNumber(int number, GridDefinition grid) {
        if (!grid.isValid() || number < grid.getStartNumber() || number > grid.getEndNumber()) {
            return -1;
        }
        
        int index = number - grid.getStartNumber();
        
        switch (grid.getFillOrder()) {
            case COLUMN_BOTTOM_TO_TOP:
                return grid.getRows() - 1 - (index % grid.getRows());
                
            case COLUMN_TOP_TO_BOTTOM:
                return index % grid.getRows();
                
            case ROW_LEFT_TO_RIGHT:
            case ROW_RIGHT_TO_LEFT:
                return index / grid.getColumns();
                
            default:
                return -1;
        }
    }
    
    /**
     * Get all numbers in a specific column
     */
    public static List<Integer> getNumbersInColumn(int columnIndex, GridDefinition grid) {
        List<Integer> numbers = new ArrayList<>();
        
        if (!grid.isValid() || columnIndex < 0 || columnIndex >= grid.getColumns()) {
            return numbers;
        }
        
        for (int num = grid.getStartNumber(); num <= grid.getEndNumber(); num++) {
            if (getColumnForNumber(num, grid) == columnIndex) {
                numbers.add(num);
            }
        }
        
        return numbers;
    }
    
    /**
     * Get all numbers in a specific row
     */
    public static List<Integer> getNumbersInRow(int rowIndex, GridDefinition grid) {
        List<Integer> numbers = new ArrayList<>();
        
        if (!grid.isValid() || rowIndex < 0 || rowIndex >= grid.getRows()) {
            return numbers;
        }
        
        for (int num = grid.getStartNumber(); num <= grid.getEndNumber(); num++) {
            if (getRowForNumber(num, grid) == rowIndex) {
                numbers.add(num);
            }
        }
        
        return numbers;
    }
    
    public static Map<String, Coordinate> calculateNumberPositions(GridDefinition grid) {
        Map<String, Coordinate> positions = new HashMap<>();
        
        if (!grid.isValid()) {
            return positions;
        }
        
        // Use adjusted cell dimensions and positions that include offsets and spacing
        double cellWidth = grid.getCellWidth();
        double cellHeight = grid.getCellHeight();
        double adjustedCellWidth = grid.getAdjustedCellWidth();
        double adjustedCellHeight = grid.getAdjustedCellHeight();
        double adjustedTopLeftX = grid.getAdjustedTopLeftX();
        double adjustedTopLeftY = grid.getAdjustedTopLeftY();
        
        for (int num = grid.getStartNumber(); num <= grid.getEndNumber(); num++) {
            int index = num - grid.getStartNumber();
            int col, row;
            
            switch (grid.getFillOrder()) {
                case COLUMN_BOTTOM_TO_TOP:
                    // Cash Five style: numbers go up in columns
                    col = index / grid.getRows();
                    row = grid.getRows() - 1 - (index % grid.getRows());
                    break;
                    
                case COLUMN_TOP_TO_BOTTOM:
                    col = index / grid.getRows();
                    row = index % grid.getRows();
                    break;
                    
                case ROW_LEFT_TO_RIGHT:
                    row = index / grid.getColumns();
                    col = index % grid.getColumns();
                    break;
                    
                case ROW_RIGHT_TO_LEFT:
                    row = index / grid.getColumns();
                    col = grid.getColumns() - 1 - (index % grid.getColumns());
                    break;
                    
                default:
                    continue;
            }
            
            // Calculate center position using adjusted spacing and offsets
            double x = adjustedTopLeftX + (col * adjustedCellWidth) + (cellWidth / 2);
            double y = adjustedTopLeftY + (row * adjustedCellHeight) + (cellHeight / 2);
            
            positions.put(String.valueOf(num), new Coordinate((int) x, (int) y));
        }
        
        return positions;
    }
    
    public static boolean markFitsInCell(GridDefinition grid, Mark mark) {
        if (grid == null || mark == null) {
            return false;
        }
        
        double cellWidth = grid.getCellWidth();
        double cellHeight = grid.getCellHeight();
        
        return mark.getWidth() <= cellWidth && mark.getHeight() <= cellHeight;
    }
    
    public static Mark suggestOptimalMarkSize(GridDefinition grid) {
        if (grid == null || !grid.isValid()) {
            return new Mark(20, 20); // Default size
        }
        
        double cellWidth = grid.getCellWidth();
        double cellHeight = grid.getCellHeight();
        
        // Use 80% of cell size to leave some padding
        int optimalWidth = (int) (cellWidth * 0.8);
        int optimalHeight = (int) (cellHeight * 0.8);
        
        // Ensure minimum size
        optimalWidth = Math.max(optimalWidth, 10);
        optimalHeight = Math.max(optimalHeight, 10);
        
        return new Mark(optimalWidth, optimalHeight);
    }
    
    public static boolean willMarksOverlap(GridDefinition grid, Mark mark) {
        if (!markFitsInCell(grid, mark)) {
            return true;
        }
        
        // Additional check: if mark size is more than 90% of cell size, 
        // marks might visually overlap due to rounding
        double cellWidth = grid.getCellWidth();
        double cellHeight = grid.getCellHeight();
        
        return mark.getWidth() > cellWidth * 0.9 || mark.getHeight() > cellHeight * 0.9;
    }
    
    public static GridDefinition createFromCorners(double x1, double y1, double x2, double y2,
                                                   int columns, int rows, 
                                                   int startNumber, int endNumber,
                                                   FillOrder fillOrder, String panelId) {
        // Ensure top-left and bottom-right are correctly oriented
        double topLeftX = Math.min(x1, x2);
        double topLeftY = Math.min(y1, y2);
        double bottomRightX = Math.max(x1, x2);
        double bottomRightY = Math.max(y1, y2);
        
        return new GridDefinition(
            topLeftX, topLeftY, bottomRightX, bottomRightY,
            columns, rows, startNumber, endNumber, fillOrder, panelId
        );
    }
    
    public static int[] parseNumberRange(String range) {
        if (range == null || range.trim().isEmpty()) {
            return new int[]{1, 35}; // Default for Cash Five
        }
        
        range = range.trim();
        
        // Handle "1-35" format
        if (range.contains("-")) {
            String[] parts = range.split("-");
            if (parts.length == 2) {
                try {
                    int start = Integer.parseInt(parts[0].trim());
                    int end = Integer.parseInt(parts[1].trim());
                    return new int[]{start, end};
                } catch (NumberFormatException e) {
                    // Fall through to default
                }
            }
        }
        
        // Handle single number
        try {
            int num = Integer.parseInt(range);
            return new int[]{1, num};
        } catch (NumberFormatException e) {
            // Return default
        }
        
        return new int[]{1, 35};
    }
    
    /**
     * Creates an optimized grid definition by learning from existing coordinate data
     */
    public static GridDefinition createLearnedGrid(Map<String, Coordinate> existingCoordinates,
                                                 double cornerX1, double cornerY1,
                                                 double cornerX2, double cornerY2,
                                                 int columns, int rows,
                                                 int startNumber, int endNumber,
                                                 FillOrder fillOrder, String panelId) {
        
        if (existingCoordinates == null || existingCoordinates.isEmpty()) {
            // Fall back to standard grid creation
            return createFromCorners(cornerX1, cornerY1, cornerX2, cornerY2, columns, rows, startNumber, endNumber, fillOrder, panelId);
        }
        
        // Extract coordinates that fall within our range
        Map<String, Coordinate> relevantCoords = existingCoordinates.entrySet().stream()
            .filter(entry -> {
                try {
                    int num = Integer.parseInt(entry.getKey());
                    return num >= startNumber && num <= endNumber;
                } catch (NumberFormatException e) {
                    return false;
                }
            })
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        
        if (relevantCoords.size() >= Math.min(10, (endNumber - startNumber + 1) / 2)) {
            // We have sufficient data to learn from actual coordinates
            return createGridFromActualCoordinates(relevantCoords, cornerX1, cornerY1, cornerX2, cornerY2, 
                                                 columns, rows, startNumber, endNumber, fillOrder, panelId);
        } else {
            // Not enough data, use geometric approach
            return createFromCorners(cornerX1, cornerY1, cornerX2, cornerY2, columns, rows, startNumber, endNumber, fillOrder, panelId);
        }
    }
    
    private static GridDefinition createGridFromActualCoordinates(Map<String, Coordinate> coords,
                                                               double cornerX1, double cornerY1,
                                                               double cornerX2, double cornerY2,
                                                               int columns, int rows,
                                                               int startNumber, int endNumber,
                                                               FillOrder fillOrder, String panelId) {
        
        // Find the actual bounds of the existing coordinates
        List<Coordinate> coordList = new ArrayList<>(coords.values());
        
        int minX = coordList.stream().mapToInt(Coordinate::getX).min().orElse((int)Math.min(cornerX1, cornerX2));
        int maxX = coordList.stream().mapToInt(Coordinate::getX).max().orElse((int)Math.max(cornerX1, cornerX2));
        int minY = coordList.stream().mapToInt(Coordinate::getY).min().orElse((int)Math.min(cornerY1, cornerY2));
        int maxY = coordList.stream().mapToInt(Coordinate::getY).max().orElse((int)Math.max(cornerY1, cornerY2));
        
        // Add some padding to ensure we capture the full grid area
        double paddingX = (maxX - minX) * 0.1; // 10% padding
        double paddingY = (maxY - minY) * 0.1; // 10% padding
        
        double topLeftX = minX - paddingX;
        double topLeftY = minY - paddingY;
        double bottomRightX = maxX + paddingX;
        double bottomRightY = maxY + paddingY;
        
        // Ensure we don't go outside the user-defined corners too much
        topLeftX = Math.max(topLeftX, Math.min(cornerX1, cornerX2) - 50);
        topLeftY = Math.max(topLeftY, Math.min(cornerY1, cornerY2) - 50);
        bottomRightX = Math.min(bottomRightX, Math.max(cornerX1, cornerX2) + 50);
        bottomRightY = Math.min(bottomRightY, Math.max(cornerY1, cornerY2) + 50);
        
        return new GridDefinition(
            topLeftX, topLeftY, bottomRightX, bottomRightY,
            columns, rows, startNumber, endNumber, fillOrder, panelId
        );
    }
    
    /**
     * Calculates optimized positions using existing coordinate data as reference
     */
    public static Map<String, Coordinate> calculateOptimizedPositions(GridDefinition grid, 
                                                                     Map<String, Coordinate> existingCoordinates) {
        Map<String, Coordinate> positions = new HashMap<>();
        
        if (!grid.isValid()) {
            return positions;
        }
        
        // First try to use existing coordinates as anchors
        Map<String, Coordinate> anchors = new HashMap<>();
        if (existingCoordinates != null) {
            for (int num = grid.getStartNumber(); num <= grid.getEndNumber(); num++) {
                String numStr = String.valueOf(num);
                if (existingCoordinates.containsKey(numStr)) {
                    anchors.put(numStr, existingCoordinates.get(numStr));
                }
            }
        }
        
        if (anchors.size() >= 3) {
            // Use hybrid approach: interpolate between known positions
            return calculateHybridPositions(grid, anchors);
        } else {
            // Fall back to standard calculation
            return calculateNumberPositions(grid);
        }
    }
    
    private static Map<String, Coordinate> calculateHybridPositions(GridDefinition grid, Map<String, Coordinate> anchors) {
        Map<String, Coordinate> positions = new HashMap<>();
        
        // Add all anchor points first
        positions.putAll(anchors);
        
        // Calculate positions for numbers we don't have anchors for
        for (int num = grid.getStartNumber(); num <= grid.getEndNumber(); num++) {
            String numStr = String.valueOf(num);
            
            if (!positions.containsKey(numStr)) {
                // Find nearest anchor points to interpolate from
                Coordinate interpolated = interpolatePosition(num, grid, anchors);
                if (interpolated != null) {
                    positions.put(numStr, interpolated);
                }
            }
        }
        
        return positions;
    }
    
    private static Coordinate interpolatePosition(int targetNum, GridDefinition grid, Map<String, Coordinate> anchors) {
        // Find the target position in the grid
        int index = targetNum - grid.getStartNumber();
        int targetCol, targetRow;
        
        switch (grid.getFillOrder()) {
            case COLUMN_BOTTOM_TO_TOP:
                targetCol = index / grid.getRows();
                targetRow = grid.getRows() - 1 - (index % grid.getRows());
                break;
            case COLUMN_TOP_TO_BOTTOM:
                targetCol = index / grid.getRows();
                targetRow = index % grid.getRows();
                break;
            case ROW_LEFT_TO_RIGHT:
                targetRow = index / grid.getColumns();
                targetCol = index % grid.getColumns();
                break;
            case ROW_RIGHT_TO_LEFT:
                targetRow = index / grid.getColumns();
                targetCol = grid.getColumns() - 1 - (index % grid.getColumns());
                break;
            default:
                return null;
        }
        
        // Find anchor points in the same row or column for interpolation
        List<Map.Entry<String, Coordinate>> sameRowAnchors = anchors.entrySet().stream()
            .filter(entry -> {
                int num = Integer.parseInt(entry.getKey());
                int anchorIndex = num - grid.getStartNumber();
                int anchorRow = getRowForIndex(anchorIndex, grid);
                return anchorRow == targetRow;
            })
            .collect(Collectors.toList());
        
        List<Map.Entry<String, Coordinate>> sameColAnchors = anchors.entrySet().stream()
            .filter(entry -> {
                int num = Integer.parseInt(entry.getKey());
                int anchorIndex = num - grid.getStartNumber();
                int anchorCol = getColForIndex(anchorIndex, grid);
                return anchorCol == targetCol;
            })
            .collect(Collectors.toList());
        
        // Try to interpolate using same row first
        if (sameRowAnchors.size() >= 2) {
            return interpolateAlongRow(targetCol, sameRowAnchors);
        }
        
        // Try to interpolate using same column
        if (sameColAnchors.size() >= 2) {
            return interpolateAlongColumn(targetRow, sameColAnchors, grid);
        }
        
        // Fall back to basic geometric calculation
        double cellWidth = grid.getCellWidth();
        double cellHeight = grid.getCellHeight();
        
        int x = (int) (grid.getTopLeftX() + (targetCol + 0.5) * cellWidth);
        int y = (int) (grid.getTopLeftY() + (targetRow + 0.5) * cellHeight);
        
        return new Coordinate(x, y);
    }
    
    private static int getRowForIndex(int index, GridDefinition grid) {
        switch (grid.getFillOrder()) {
            case COLUMN_BOTTOM_TO_TOP:
                return grid.getRows() - 1 - (index % grid.getRows());
            case COLUMN_TOP_TO_BOTTOM:
                return index % grid.getRows();
            case ROW_LEFT_TO_RIGHT:
            case ROW_RIGHT_TO_LEFT:
                return index / grid.getColumns();
            default:
                return 0;
        }
    }
    
    private static int getColForIndex(int index, GridDefinition grid) {
        switch (grid.getFillOrder()) {
            case COLUMN_BOTTOM_TO_TOP:
            case COLUMN_TOP_TO_BOTTOM:
                return index / grid.getRows();
            case ROW_LEFT_TO_RIGHT:
                return index % grid.getColumns();
            case ROW_RIGHT_TO_LEFT:
                return grid.getColumns() - 1 - (index % grid.getColumns());
            default:
                return 0;
        }
    }
    
    private static Coordinate interpolateAlongRow(int targetCol, List<Map.Entry<String, Coordinate>> rowAnchors) {
        // Sort anchors by their column position
        rowAnchors.sort((a, b) -> Integer.compare(a.getValue().getX(), b.getValue().getX()));
        
        // Find the two anchors to interpolate between
        for (int i = 0; i < rowAnchors.size() - 1; i++) {
            Coordinate left = rowAnchors.get(i).getValue();
            Coordinate right = rowAnchors.get(i + 1).getValue();
            
            // Simple linear interpolation
            double ratio = (double) targetCol / (rowAnchors.size() - 1);
            int x = (int) (left.getX() + ratio * (right.getX() - left.getX()));
            int y = (int) (left.getY() + ratio * (right.getY() - left.getY()));
            
            return new Coordinate(x, y);
        }
        
        return null;
    }
    
    private static Coordinate interpolateAlongColumn(int targetRow, List<Map.Entry<String, Coordinate>> colAnchors, GridDefinition grid) {
        // Sort anchors by their row position
        colAnchors.sort((a, b) -> Integer.compare(a.getValue().getY(), b.getValue().getY()));
        
        // Find the two anchors to interpolate between
        for (int i = 0; i < colAnchors.size() - 1; i++) {
            Coordinate top = colAnchors.get(i).getValue();
            Coordinate bottom = colAnchors.get(i + 1).getValue();
            
            // Simple linear interpolation
            double ratio = (double) targetRow / (grid.getRows() - 1);
            int x = (int) (top.getX() + ratio * (bottom.getX() - top.getX()));
            int y = (int) (top.getY() + ratio * (bottom.getY() - top.getY()));
            
            return new Coordinate(x, y);
        }
        
        return null;
    }
}