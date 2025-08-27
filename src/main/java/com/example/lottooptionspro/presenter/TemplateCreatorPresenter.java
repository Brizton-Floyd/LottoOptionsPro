package com.example.lottooptionspro.presenter;

import com.example.lottooptionspro.models.*;
import com.example.lottooptionspro.util.GridCalculator;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

public class TemplateCreatorPresenter {
    private BetslipTemplate model;
    private final TemplateCreatorView view;
    private final List<Runnable> undoStack = new ArrayList<>();

    private Coordinate originalCoordinate;
    private ScannerMark originalScannerMark;
    private File currentFile; // Remember the current file for saving
    
    // Grid mapping state
    private double firstCornerX, firstCornerY;
    private double secondCornerX, secondCornerY;
    private GridDefinition currentGrid;
    
    // Store original positions for reset functionality
    private Map<String, Coordinate> originalMainNumbers;
    private Map<String, Coordinate> originalBonusNumbers;

    public TemplateCreatorPresenter(BetslipTemplate model, TemplateCreatorView view) {
        this.model = model;
        this.view = view;
        if (this.model.getMark() == null) this.model.setMark(new Mark(20, 20));
        if (this.model.getPlayPanels() == null) this.model.setPlayPanels(new ArrayList<>());
        if (this.model.getGlobalOptions() == null) this.model.setGlobalOptions(new HashMap<>());
        if (this.model.getScannerMarks() == null) this.model.setScannerMarks(new ArrayList<>());
    }

    public void onPanelOrModeChanged() {
        String currentPanelId = view.getSelectedPanel();
        String currentMode = view.getSelectedMappingMode();
        if (currentPanelId == null || currentMode == null) return;

        // Panel/mode changed - no longer need to update Next Number field
    }

    public void saveTemplate() {
        if (currentFile != null) {
            writeTemplateToFile(currentFile);
        } else {
            saveTemplateAs();
        }
    }

    public void saveTemplateAs() {
        String gameName = view.getGameName();
        String jurisdiction = view.getJurisdiction();

        if (gameName == null || gameName.trim().isEmpty() || jurisdiction == null || jurisdiction.trim().isEmpty()) {
            view.showError("Please enter both a Game Name and Jurisdiction before saving.");
            return;
        }

        String initialFileName = String.format("%s-%s.json", jurisdiction.toLowerCase().replace(" ", ""), gameName.toLowerCase().replace(" ", ""));
        File file = view.showSaveDialog(initialFileName);
        if (file != null) {
            writeTemplateToFile(file);
        }
    }

    private void writeTemplateToFile(File file) {
        model.setGameName(view.getGameName());
        model.setJurisdiction(view.getJurisdiction());
        
        // Save current grid configuration if available
        captureGridConfiguration();

        try (FileWriter writer = new FileWriter(file)) {
            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            gson.toJson(model, writer);
            this.currentFile = file; // Remember the file path
            view.showSuccess("Template saved successfully to " + file.getName());
        } catch (IOException e) {
            view.showError("Failed to save template: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private void captureGridConfiguration() {
        // Only save grid config if the user has configured it
        if (view.isGridModeEnabled()) {
            String numberRange = view.getGridNumberRange();
            int columns = view.getGridColumns();
            int rows = view.getGridRows();
            String fillOrder = view.getGridFillOrder();
            
            // Only save if we have valid configuration
            if (numberRange != null && !numberRange.trim().isEmpty() && columns > 0 && rows > 0) {
                GridConfiguration gridConfig = new GridConfiguration(numberRange, columns, rows, fillOrder);
                model.setGridConfig(gridConfig);
            }
        } else {
            // Grid mode is disabled, don't save any grid config
            model.setGridConfig(null);
        }
    }

    public void loadTemplate() {
        File file = view.showOpenTemplateDialog();
        if (file != null) {
            try (FileReader reader = new FileReader(file)) {
                Gson gson = new Gson();
                this.model = gson.fromJson(reader, BetslipTemplate.class);
                if (model.getScannerMarks() == null) {
                    model.setScannerMarks(new ArrayList<>());
                }
                this.currentFile = file; // Remember the file path
                updateViewFromModel();
                view.showSuccess("Template loaded successfully from " + file.getName());
            } catch (IOException | com.google.gson.JsonSyntaxException e) {
                view.showError("Failed to load template: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    public void previewTemplate() {
        view.clearPreviewRectangles();
        String panelId = view.getSelectedPanel();
        if (panelId == null || panelId.isEmpty()) {
            view.showError("Please select a panel to preview.");
            return;
        }
        PlayPanel panel = getOrCreatePlayPanel(panelId);

        view.askForPreviewNumbers().ifPresent(numbersString -> {
            int marksDrawn = 0;
            String[] parts = numbersString.split(",");

            if (parts.length > 0 && !parts[0].trim().isEmpty()) {
                String[] mainNumbers = parts[0].trim().split("\\s+");
                for (String num : mainNumbers) {
                    Coordinate coord = panel.getMainNumbers().get(num);
                    if (coord != null) {
                        view.drawPreviewRectangle(coord, model.getMark().getWidth(), model.getMark().getHeight());
                        marksDrawn++;
                    }
                }
            }

            if (parts.length > 1 && !parts[1].trim().isEmpty()) {
                String[] bonusNumbers = parts[1].trim().split("\\s+");
                for (String num : bonusNumbers) {
                    Coordinate coord = panel.getBonusNumbers().get(num);
                    if (coord != null) {
                        view.drawPreviewRectangle(coord, model.getMark().getWidth(), model.getMark().getHeight());
                        marksDrawn++;
                    }
                }
            }

            if (marksDrawn == 0) {
                view.showSuccess("Preview Complete: No coordinates were found for the entered numbers on this panel.");
            }
        });
    }

    private void updateViewFromModel() {
        view.setGameName(model.getGameName());
        view.setJurisdiction(model.getJurisdiction());
        if (model.getImagePath() != null && !model.getImagePath().isEmpty()) {
            view.displayImage(new File(model.getImagePath()).toURI().toString());
        }
        
        // Auto-populate grid settings based on template data
        autoPopulateGridSettings();
        
        redrawAllMarkings();
        onPanelOrModeChanged();
    }
    
    private void autoPopulateGridSettings() {
        if (model == null) {
            return; // No model, let user configure manually
        }
        
        // First priority: Use saved grid configuration from the template
        if (model.getGridConfig() != null && model.getGridConfig().isValid()) {
            GridConfiguration savedConfig = model.getGridConfig();
            view.setGridNumberRange(savedConfig.getNumberRange());
            view.setGridColumns(savedConfig.getColumns());
            view.setGridRows(savedConfig.getRows());
            view.setGridFillOrder(savedConfig.getFillOrder());
            System.out.println("Loaded saved grid config: " + savedConfig.getNumberRange() + ", " + 
                             savedConfig.getColumns() + "x" + savedConfig.getRows());
            return;
        }
        
        // Second priority: Try to analyze existing coordinate data (fallback for old templates)
        if (model.getPlayPanels() != null && !model.getPlayPanels().isEmpty()) {
            PlayPanel firstPanel = model.getPlayPanels().get(0);
            if (firstPanel.getMainNumbers() != null && firstPanel.getMainNumbers().size() >= 10) {
                Map<String, Coordinate> mainNumbers = firstPanel.getMainNumbers();
                
                int minNumber = mainNumbers.keySet().stream().mapToInt(Integer::parseInt).min().orElse(1);
                int maxNumber = mainNumbers.keySet().stream().mapToInt(Integer::parseInt).max().orElse(35);
                
                // Set number range based on actual coordinate data
                view.setGridNumberRange(minNumber + "-" + maxNumber);
                
                // Try to learn grid configuration from coordinates
                GridConfig learnedConfig = analyzeExistingCoordinates(mainNumbers, minNumber, maxNumber);
                if (learnedConfig != null) {
                    view.setGridColumns(learnedConfig.columns);
                    view.setGridRows(learnedConfig.rows);
                    view.setGridFillOrder(learnedConfig.fillOrder);
                    System.out.println("Learned grid config from coordinates: " + 
                                     learnedConfig.columns + "x" + learnedConfig.rows);
                    return;
                }
            }
        }
        
        // No saved config and no analyzable coordinate data - let user configure manually
        System.out.println("No grid config found - user will configure manually");
    }
    
    private GridConfig analyzeExistingCoordinates(Map<String, Coordinate> coordinates, int minNumber, int maxNumber) {
        if (coordinates.size() < 10) {
            return null; // Need sufficient data points
        }
        
        try {
            // Group coordinates by X position (columns) and Y position (rows)
            Map<Integer, List<Coordinate>> coordinatesByX = coordinates.values().stream()
                .collect(Collectors.groupingBy(Coordinate::getX));
            
            Map<Integer, List<Coordinate>> coordinatesByY = coordinates.values().stream()
                .collect(Collectors.groupingBy(Coordinate::getY));
            
            // Analyze the grid structure
            int columns = coordinatesByX.size();
            int rows = coordinatesByY.size();
            
            // Validate the grid makes sense
            int totalNumbers = maxNumber - minNumber + 1;
            int expectedCells = columns * rows;
            
            // Allow for some flexibility - grid might be partially filled or have extras
            if (Math.abs(expectedCells - totalNumbers) > totalNumbers * 0.3) {
                System.out.println("Grid analysis: cell count (" + expectedCells + ") doesn't match number count (" + totalNumbers + ")");
                return null; // Grid structure doesn't match the data well
            }
            
            // Check if coordinates are reasonably distributed across grid positions
            boolean wellDistributed = coordinatesByX.values().stream()
                .allMatch(list -> list.size() >= Math.max(1, rows * 0.3)) && // Each column has at least 30% of expected numbers
                coordinatesByY.values().stream()
                .allMatch(list -> list.size() >= Math.max(1, columns * 0.3)); // Each row has at least 30% of expected numbers
            
            if (!wellDistributed) {
                System.out.println("Grid analysis: coordinates not well distributed across grid");
                return null; // Coordinates don't seem to follow a regular grid pattern
            }
            
            // Analyze the fill pattern by looking at the arrangement
            String fillOrder = determineFillOrderFromCoordinates(coordinates, columns, rows, minNumber);
            
            System.out.println("Grid analysis successful: " + columns + "x" + rows + " with " + fillOrder + " fill order");
            return new GridConfig(columns, rows, fillOrder);
            
        } catch (Exception e) {
            System.err.println("Error analyzing coordinates: " + e.getMessage());
            return null;
        }
    }
    
    private String determineFillOrderFromCoordinates(Map<String, Coordinate> coordinates, 
                                                   int columns, int rows, int minNumber) {
        // Get coordinates for first few numbers to determine pattern
        List<Integer> sortedNumbers = coordinates.keySet().stream()
            .mapToInt(Integer::parseInt)
            .sorted()
            .boxed()
            .limit(Math.min(10, columns * 2)) // Analyze first 10 numbers or 2 columns worth
            .collect(Collectors.toList());
        
        if (sortedNumbers.size() < 4) {
            return "COLUMN_BOTTOM_TO_TOP"; // Default
        }
        
        // Check if numbers are arranged in columns (Y changes before X)
        List<Coordinate> firstNumbers = sortedNumbers.stream()
            .map(n -> coordinates.get(String.valueOf(n)))
            .collect(Collectors.toList());
        
        // Compare first and second number positions
        Coordinate first = firstNumbers.get(0);
        Coordinate second = firstNumbers.get(1);
        Coordinate third = firstNumbers.size() > 2 ? firstNumbers.get(2) : null;
        
        if (first.getX() == second.getX()) {
            // Same column, check if going up or down
            if (third != null && first.getX() == third.getX()) {
                // First 3 numbers in same column
                return first.getY() > second.getY() ? "COLUMN_BOTTOM_TO_TOP" : "COLUMN_TOP_TO_BOTTOM";
            } else {
                // Column arrangement
                return first.getY() > second.getY() ? "COLUMN_BOTTOM_TO_TOP" : "COLUMN_TOP_TO_BOTTOM";
            }
        } else if (first.getY() == second.getY()) {
            // Same row, going left to right or right to left
            return first.getX() < second.getX() ? "ROW_LEFT_TO_RIGHT" : "ROW_RIGHT_TO_LEFT";
        }
        
        // Default fallback
        return "COLUMN_BOTTOM_TO_TOP";
    }
    
    private GridConfig getGridConfigForGame(String gameName, int totalNumbers) {
        // Cash Five: 35 numbers in 7×5 grid, column bottom-to-top
        if (gameName.contains("cash") || gameName.contains("five") || totalNumbers == 35) {
            return new GridConfig(7, 5, "COLUMN_BOTTOM_TO_TOP");
        }
        
        // Powerball main numbers: 69 numbers, could be various layouts
        if ((gameName.contains("powerball") || totalNumbers == 69)) {
            return new GridConfig(10, 7, "ROW_LEFT_TO_RIGHT"); // Common Powerball layout
        }
        
        // Mega Millions: 70 numbers
        if (gameName.contains("mega") || totalNumbers == 70) {
            return new GridConfig(10, 7, "ROW_LEFT_TO_RIGHT");
        }
        
        // For other number ranges, try to find reasonable grid dimensions
        if (totalNumbers <= 49) {
            return new GridConfig(7, 7, "COLUMN_BOTTOM_TO_TOP");
        } else if (totalNumbers <= 80) {
            return new GridConfig(10, 8, "ROW_LEFT_TO_RIGHT");
        }
        
        return null; // Let user configure manually
    }
    
    // Helper class for grid configuration
    private static class GridConfig {
        final int columns;
        final int rows;
        final String fillOrder;
        
        GridConfig(int columns, int rows, String fillOrder) {
            this.columns = columns;
            this.rows = rows;
            this.fillOrder = fillOrder;
        }
    }

    public void loadImage() {
        File file = view.showOpenImageDialog();
        if (file != null) {
            onImageSelected(file.toURI().toString(), file.getAbsolutePath());
        }
    }

    public void onImageSelected(String imageUri, String imagePath) {
        if (imagePath != null && !imagePath.isEmpty()) {
            model.setImagePath(imagePath);
            view.displayImage(imageUri);
            this.currentFile = null; // New image means it's a new template

            // A new image means a new template, so we clear all existing markings.
            if (model.getPlayPanels() != null) {
                model.getPlayPanels().clear();
            }
            if (model.getGlobalOptions() != null) {
                model.getGlobalOptions().clear();
            }
            if (model.getScannerMarks() != null) {
                model.getScannerMarks().clear();
            }

            undoStack.clear();
            redrawAllMarkings();
        }
    }

    public void onPaneClicked(double x, double y, int width, int height) {
        view.clearPreviewRectangles();
        String mappingMode = view.getSelectedMappingMode();
        if (mappingMode == null || mappingMode.isEmpty()) {
            view.showError("Please select a mapping mode first.");
            return;
        }

        switch (mappingMode) {
            case "Main Number":
            case "Bonus Number":
            case "Quick Pick":
                handlePanelMapping(new Coordinate((int) x, (int) y), mappingMode);
                break;
            case "Global Option":
                handleGlobalOptionMapping(new Coordinate((int) x, (int) y));
                break;
            case "Scanner Mark":
                handleScannerMarkMapping(x, y, width, height);
                break;
        }
    }

    private void handleScannerMarkMapping(double x, double y, int width, int height) {
        ScannerMark newMark = new ScannerMark(x, y, width, height);
        model.getScannerMarks().add(newMark);
        undoStack.add(() -> {
            model.getScannerMarks().remove(newMark);
            redrawAllMarkings();
        });
        redrawAllMarkings();
    }

    private void handlePanelMapping(Coordinate coordinate, String mappingMode) {
        String panelId = view.getSelectedPanel();
        if (panelId == null || panelId.isEmpty()) {
            view.showError("Please select a panel first.");
            return;
        }
        PlayPanel panel = getOrCreatePlayPanel(panelId);

        if ("Quick Pick".equals(mappingMode)) {
            Coordinate oldQp = panel.getQuickPick();
            panel.setQuickPick(coordinate);
            undoStack.add(() -> panel.setQuickPick(oldQp));
        } else { // Main or Bonus
            // Auto-calculate next number based on existing numbers in panel
            int nextNumber = 1;
            if ("Main Number".equals(mappingMode)) {
                nextNumber = panel.getMainNumbers().size() + 1;
            } else if ("Bonus Number".equals(mappingMode)) {
                nextNumber = panel.getBonusNumbers().size() + 1;
            }
            String numberKey = String.valueOf(nextNumber);
            if ("Main Number".equals(mappingMode)) {
                Coordinate oldMain = panel.getMainNumbers().put(numberKey, coordinate);
                undoStack.add(() -> {
                    if (oldMain != null) panel.getMainNumbers().put(numberKey, oldMain); else panel.getMainNumbers().remove(numberKey);
                });
            } else { // Bonus Number
                Coordinate oldBonus = panel.getBonusNumbers().put(numberKey, coordinate);
                undoStack.add(() -> {
                    if (oldBonus != null) panel.getBonusNumbers().put(numberKey, oldBonus); else panel.getBonusNumbers().remove(numberKey);
                });
            }
        }
        redrawAllMarkings();
    }

    private void handleGlobalOptionMapping(Coordinate coordinate) {
        String optionName = view.getGlobalOptionName();
        if (optionName == null || optionName.trim().isEmpty()) {
            view.showError("Please enter a name for the Global Option.");
            return;
        }
        Coordinate oldGlobal = model.getGlobalOptions().put(optionName, coordinate);
        undoStack.add(() -> model.getGlobalOptions().put(optionName, oldGlobal));
        redrawAllMarkings();
    }

    public void removeLastMarking() {
        view.clearPreviewRectangles();
        if (!undoStack.isEmpty()) {
            undoStack.remove(undoStack.size() - 1).run();
            redrawAllMarkings();
        }
    }

    public void updateMarkSize(int width, int height) {
        model.getMark().setWidth(width);
        model.getMark().setHeight(height);
        redrawAllMarkings();
    }

    public void updateScannerMarkSize(ScannerMark mark, int width, int height) {
        final double oldWidth = mark.getWidth();
        final double oldHeight = mark.getHeight();
        undoStack.add(() -> {
            mark.setWidth(oldWidth);
            mark.setHeight(oldHeight);
            view.updateScannerMarkRectangle(mark, oldWidth, oldHeight);
        });
        mark.setWidth(width);
        mark.setHeight(height);
        view.updateScannerMarkRectangle(mark, width, height);
    }

    public void startCoordinateMove(Coordinate coord) {
        this.originalCoordinate = new Coordinate(coord.getX(), coord.getY());
    }

    public void finishCoordinateMove(Coordinate coordToMove, int newX, int newY) {
        final int oldX = this.originalCoordinate.getX();
        final int oldY = this.originalCoordinate.getY();
        undoStack.add(() -> {
            coordToMove.setX(oldX);
            coordToMove.setY(oldY);
            redrawAllMarkings();
        });

        coordToMove.setX(newX);
        coordToMove.setY(newY);

        this.originalCoordinate = null;
        redrawAllMarkings();
    }

    public void startScannerMarkMove(ScannerMark mark) {
        this.originalScannerMark = new ScannerMark(mark.getX(), mark.getY(), mark.getWidth(), mark.getHeight());
    }

    public void finishScannerMarkMove(ScannerMark markToMove, double newX, double newY) {
        final double oldX = this.originalScannerMark.getX();
        final double oldY = this.originalScannerMark.getY();
        undoStack.add(() -> {
            markToMove.setX(oldX);
            markToMove.setY(oldY);
            redrawAllMarkings();
        });

        markToMove.setX(newX);
        markToMove.setY(newY);

        this.originalScannerMark = null;
        redrawAllMarkings();
    }

    public void redrawAllMarkings() {
        view.clearAllRectangles();
        int w = model.getMark().getWidth();
        int h = model.getMark().getHeight();
        for (PlayPanel panel : model.getPlayPanels()) {
            panel.getMainNumbers().values().forEach(c -> view.drawRectangle(c, w, h));
            panel.getBonusNumbers().values().forEach(c -> view.drawRectangle(c, w, h));
            if (panel.getQuickPick() != null) view.drawRectangle(panel.getQuickPick(), w, h);
        }
        model.getGlobalOptions().values().forEach(c -> view.drawRectangle(c, w, h));
        model.getScannerMarks().forEach(view::drawScannerMark);
        view.setScannerMarkCount(model.getScannerMarks().size());
    }

    private PlayPanel getOrCreatePlayPanel(String panelId) {
        return model.getPlayPanels().stream()
                .filter(p -> panelId.equals(p.getPanelId()))
                .findFirst()
                .orElseGet(() -> {
                    PlayPanel newPanel = new PlayPanel(panelId, new HashMap<>(), new HashMap<>(), null);
                    model.getPlayPanels().add(newPanel);
                    return newPanel;
                });
    }
    
    // Grid mapping methods
    public void setFirstGridCorner(double x, double y) {
        firstCornerX = x;
        firstCornerY = y;
    }
    
    public void setSecondGridCorner(double x, double y) {
        secondCornerX = x;
        secondCornerY = y;
    }
    
    public void previewGrid() {
        if (!view.isGridModeEnabled()) {
            return;
        }
        
        // Parse number range
        String rangeStr = view.getGridNumberRange();
        int[] range = GridCalculator.parseNumberRange(rangeStr);
        
        // Get grid dimensions
        int columns = view.getGridColumns();
        int rows = view.getGridRows();
        
        // Get fill order
        FillOrder fillOrder = parseFillOrder(view.getGridFillOrder());
        
        // Get current panel
        String panelId = view.getSelectedPanel();
        
        // Create grid definition
        currentGrid = GridCalculator.createFromCorners(
            firstCornerX, firstCornerY,
            secondCornerX, secondCornerY,
            columns, rows,
            range[0], range[1],
            fillOrder, panelId
        );
        
        // Check if mark fits
        if (!GridCalculator.markFitsInCell(currentGrid, model.getMark())) {
            view.showError("Warning: Current mark size may not fit in grid cells. Consider adjusting mark size or grid dimensions.");
        }
        
        // Show preview
        view.showGridPreview(currentGrid);
    }
    
    public void autoMapGridNumbers() {
        if (currentGrid == null || !currentGrid.isValid()) {
            view.showError("Please define a valid grid first");
            return;
        }
        
        String mappingMode = view.getSelectedMappingMode();
        if (!"Main Number".equals(mappingMode) && !"Bonus Number".equals(mappingMode)) {
            view.showError("Grid mapping only works with Main Number or Bonus Number modes");
            return;
        }
        
        // Calculate all positions using optimized method if existing coordinates are available
        PlayPanel currentPanel = getOrCreatePlayPanel(view.getSelectedPanel());
        Map<String, Coordinate> existingCoords = null;
        
        if ("Main Number".equals(mappingMode) && currentPanel != null) {
            existingCoords = currentPanel.getMainNumbers();
        } else if ("Bonus Number".equals(mappingMode) && currentPanel != null) {
            existingCoords = currentPanel.getBonusNumbers();
        }
        
        // Use optimized calculation if we have existing coordinates, otherwise use standard method
        Map<String, Coordinate> positions;
        if (existingCoords != null && !existingCoords.isEmpty()) {
            // For now, fall back to standard method - we can enhance this later
            positions = GridCalculator.calculateNumberPositions(currentGrid);
        } else {
            positions = GridCalculator.calculateNumberPositions(currentGrid);
        }
        
        if (positions.isEmpty()) {
            view.showError("Failed to calculate number positions");
            return;
        }
        
        // Get or create panel
        PlayPanel panel = getOrCreatePlayPanel(currentGrid.getPanelId());
        
        // Store old state for undo
        Map<String, Coordinate> oldPositions;
        if ("Main Number".equals(mappingMode)) {
            oldPositions = new HashMap<>(panel.getMainNumbers());
        } else {
            oldPositions = new HashMap<>(panel.getBonusNumbers());
        }
        
        // Apply new positions
        int count = 0;
        int total = positions.size();
        
        for (Map.Entry<String, Coordinate> entry : positions.entrySet()) {
            if ("Main Number".equals(mappingMode)) {
                panel.getMainNumbers().put(entry.getKey(), entry.getValue());
            } else {
                panel.getBonusNumbers().put(entry.getKey(), entry.getValue());
            }
            count++;
            view.setGridMappingProgress(count, total);
        }
        
        // Add to undo stack
        final String mode = mappingMode;
        undoStack.add(() -> {
            if ("Main Number".equals(mode)) {
                panel.getMainNumbers().clear();
                panel.getMainNumbers().putAll(oldPositions);
            } else {
                panel.getBonusNumbers().clear();
                panel.getBonusNumbers().putAll(oldPositions);
            }
            redrawAllMarkings();
        });
        
        // Redraw
        redrawAllMarkings();
        view.clearGridVisuals();
        view.setGridMappingMode(GridMappingMode.GRID_MAPPED);
        view.updateGridMappingStatus(String.format("Successfully mapped %d numbers", total));
        
        // Keep grid definition available for column/row tuning - don't set to null!
    }
    
    private FillOrder parseFillOrder(String orderStr) {
        switch (orderStr) {
            case "COLUMN_TOP_TO_BOTTOM":
                return FillOrder.COLUMN_TOP_TO_BOTTOM;
            case "ROW_LEFT_TO_RIGHT":
                return FillOrder.ROW_LEFT_TO_RIGHT;
            case "ROW_RIGHT_TO_LEFT":
                return FillOrder.ROW_RIGHT_TO_LEFT;
            case "COLUMN_BOTTOM_TO_TOP":
            default:
                return FillOrder.COLUMN_BOTTOM_TO_TOP;
        }
    }
    
    // Grid adjustment methods
    public GridDefinition getCurrentGrid() {
        return currentGrid;
    }
    
    public void updateGridDefinition(GridDefinition newGrid) {
        this.currentGrid = newGrid;
        if (view != null) {
            view.showGridOverlay(newGrid);
        }
    }
    
    /**
     * Reset both grid position and number positions to their original state
     * This is used when the reset button is clicked during grid fine-tuning
     */
    public void resetGridAndNumbers(GridDefinition originalGrid) {
        if (originalGrid == null) {
            view.showError("No original grid position available for reset");
            return;
        }
        
        // Reset the grid overlay
        this.currentGrid = originalGrid;
        if (view != null) {
            view.showGridOverlay(originalGrid);
        }
        
        // Reset number positions if we have stored originals
        String panelId = view.getSelectedPanel();
        if (panelId != null && (originalMainNumbers != null || originalBonusNumbers != null)) {
            PlayPanel panel = getOrCreatePlayPanel(panelId);
            
            // Store current positions for undo
            Map<String, Coordinate> currentMainNumbers = new HashMap<>(panel.getMainNumbers());
            Map<String, Coordinate> currentBonusNumbers = new HashMap<>(panel.getBonusNumbers());
            
            // Restore original positions
            if (originalMainNumbers != null) {
                panel.getMainNumbers().clear();
                panel.getMainNumbers().putAll(originalMainNumbers);
            }
            if (originalBonusNumbers != null) {
                panel.getBonusNumbers().clear();
                panel.getBonusNumbers().putAll(originalBonusNumbers);
            }
            
            // Add to undo stack
            undoStack.add(() -> {
                panel.getMainNumbers().clear();
                panel.getMainNumbers().putAll(currentMainNumbers);
                panel.getBonusNumbers().clear();
                panel.getBonusNumbers().putAll(currentBonusNumbers);
                redrawAllMarkings();
            });
            
            // Redraw with restored positions
            redrawAllMarkings();
        }
    }
    
    /**
     * Store the current number positions before starting grid adjustment
     * This allows them to be restored later when reset is called
     */
    public void storeOriginalNumberPositions() {
        String panelId = view.getSelectedPanel();
        if (panelId == null) return;
        
        PlayPanel panel = getOrCreatePlayPanel(panelId);
        
        // Store deep copies of current positions
        if (!panel.getMainNumbers().isEmpty()) {
            originalMainNumbers = new HashMap<>();
            for (Map.Entry<String, Coordinate> entry : panel.getMainNumbers().entrySet()) {
                originalMainNumbers.put(entry.getKey(), 
                    new Coordinate(entry.getValue().getX(), entry.getValue().getY()));
            }
        }
        
        if (!panel.getBonusNumbers().isEmpty()) {
            originalBonusNumbers = new HashMap<>();
            for (Map.Entry<String, Coordinate> entry : panel.getBonusNumbers().entrySet()) {
                originalBonusNumbers.put(entry.getKey(), 
                    new Coordinate(entry.getValue().getX(), entry.getValue().getY()));
            }
        }
    }
    
    public void clearAllMarkingsData() {
        if (model == null) return;
        
        // Clear all panel data
        if (model.getPlayPanels() != null) {
            for (PlayPanel panel : model.getPlayPanels()) {
                if (panel.getMainNumbers() != null) {
                    panel.getMainNumbers().clear();
                }
                if (panel.getBonusNumbers() != null) {
                    panel.getBonusNumbers().clear();
                }
                panel.setQuickPick(null);
            }
        }
        
        // Clear global options
        if (model.getGlobalOptions() != null) {
            model.getGlobalOptions().clear();
        }
        
        // Clear scanner marks
        if (model.getScannerMarks() != null) {
            model.getScannerMarks().clear();
        }
        
        // Clear undo stack
        undoStack.clear();
        
        // Reset grid state
        currentGrid = null;
        
        // Update scanner mark count
        if (view != null) {
            view.setScannerMarkCount(0);
        }
    }
    
    /**
     * Get main numbers for a specific panel
     */
    public Map<String, Coordinate> getMainNumbersForPanel(String panelId) {
        if (panelId == null) return new HashMap<>();
        
        PlayPanel panel = model.getPlayPanels().stream()
                .filter(p -> panelId.equals(p.getPanelId()))
                .findFirst()
                .orElse(null);
                
        return panel != null ? panel.getMainNumbers() : new HashMap<>();
    }
    
    /**
     * Add a runnable to the undo stack for external use
     */
    public void addToUndoStack(Runnable undoAction) {
        undoStack.add(undoAction);
    }
    
    /**
     * Trigger redraw from external classes
     */
    public void triggerRedraw() {
        redrawAllMarkings();
    }
}
