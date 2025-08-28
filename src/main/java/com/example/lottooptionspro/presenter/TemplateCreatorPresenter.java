package com.example.lottooptionspro.presenter;

import com.example.lottooptionspro.models.*;
import com.example.lottooptionspro.util.GridCalculator;
import com.example.lottooptionspro.controller.TemplateCreatorController;
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
        
        // Initialize new scanner mark size from current model defaults
        this.newScannerMarkWidth = this.model.getMark().getWidth();
        this.newScannerMarkHeight = this.model.getMark().getHeight();
        
        // Only renumber Scanner Marks if they exist and have inconsistent IDs
        if (this.model.getScannerMarks() != null && !this.model.getScannerMarks().isEmpty()) {
            boolean needsRenumbering = false;
            for (int i = 0; i < this.model.getScannerMarks().size(); i++) {
                if (this.model.getScannerMarks().get(i).getId() != i + 1) {
                    needsRenumbering = true;
                    break;
                }
            }
            if (needsRenumbering) {
                System.out.println("Renumbering Scanner Marks during initialization due to inconsistent IDs");
                renumberScannerMarks();
            }
        }
    }

    public void onPanelOrModeChanged() {
        String currentPanelId = view.getSelectedPanel();
        String currentMode = view.getSelectedMappingMode();
        if (currentPanelId == null || currentMode == null) return;

        // Panel/mode changed - no longer need to update Next Number field
        
        // Update grid definition for new panel to enable column/row selection
        updateGridDefinitionForCurrentPanel();
        
        // Debug: Print current grid status after panel change
        if (currentGrid != null) {
            System.out.println("Panel changed to " + currentPanelId + 
                ", currentGrid panel: " + currentGrid.getPanelId() + 
                ", grid valid: " + currentGrid.isValid());
        } else {
            System.out.println("Panel changed to " + currentPanelId + ", currentGrid is NULL");
        }
    }
    
    /**
     * Update grid definition for the current panel to enable column/row selection
     */
    private void updateGridDefinitionForCurrentPanel() {
        String currentPanelId = view.getSelectedPanel();
        if (currentPanelId == null) return;
        
        // Update grid for both existing panels and new panels  
        if (model != null) {
            // Try grid config first, then fall back to coordinate inference
            boolean hasGridConfig = model.getGridConfig() != null && model.getGridConfig().isValid();
            
            // Debug: Print grid config details
            if (model.getGridConfig() != null) {
                GridConfiguration config = model.getGridConfig();
                System.out.println("DEBUG: Grid config found - Range: " + config.getNumberRange() + 
                    ", Columns: " + config.getColumns() + 
                    ", Rows: " + config.getRows() + 
                    ", FillOrder: " + config.getFillOrder() + 
                    ", Valid: " + config.isValid());
            } else {
                System.out.println("DEBUG: No grid config found in model");
            }
            PlayPanel currentPanel = getOrCreatePlayPanel(currentPanelId);
            
            // If current panel has coordinates, reconstruct grid from coordinates
            if (currentPanel.getMainNumbers() != null && !currentPanel.getMainNumbers().isEmpty()) {
                
                // ALWAYS try to reconstruct from saved grid config first if available
                if (hasGridConfig) {
                    // Clear current grid to force reconstruction from saved config
                    currentGrid = null;
                    reconstructGridDefinitionFromData();
                    
                    if (currentGrid != null) {
                        System.out.println("Reconstructed grid from saved config for panel " + currentPanelId + ": " + 
                            currentGrid.getColumns() + "x" + currentGrid.getRows());
                    }
                }
                
                // If no saved config or reconstruction failed, try coordinate inference as fallback
                if (currentGrid == null) {
                    System.out.println("DEBUG: Saved config reconstruction failed, trying fallback methods...");
                    currentGrid = createGridFromExistingData(currentPanel, currentPanelId);
                    if (currentGrid != null) {
                        System.out.println("Created grid via fallback method for panel " + currentPanelId + ": " + 
                            currentGrid.getColumns() + "x" + currentGrid.getRows());
                    } else {
                        System.out.println("FAILED: Could not create grid for panel " + currentPanelId + 
                            " - saved config invalid and coordinate inference failed");
                    }
                }
                
                // Update the current grid's panel ID to match the current panel
                if (currentGrid != null) {
                    currentGrid = new GridDefinition(
                        currentGrid.getTopLeftX(),
                        currentGrid.getTopLeftY(),
                        currentGrid.getBottomRightX(),
                        currentGrid.getBottomRightY(),
                        currentGrid.getColumns(),
                        currentGrid.getRows(),
                        currentGrid.getStartNumber(),
                        currentGrid.getEndNumber(),
                        currentGrid.getFillOrder(),
                        currentPanelId // Update panel ID
                    );
                }
            } else if (hasGridConfig) {
                // Panel has no coordinates yet, but we can still use the grid configuration
                // Also check if we can use an existing grid from another panel as a template
                if (currentGrid != null && currentGrid.isValid()) {
                    // We have a valid grid from another panel, adapt it for this panel
                    currentGrid = new GridDefinition(
                        currentGrid.getTopLeftX(),
                        currentGrid.getTopLeftY(),
                        currentGrid.getBottomRightX(),
                        currentGrid.getBottomRightY(),
                        currentGrid.getColumns(),
                        currentGrid.getRows(),
                        currentGrid.getStartNumber(),
                        currentGrid.getEndNumber(),
                        currentGrid.getFillOrder(),
                        currentPanelId // Update panel ID for new panel
                    );
                    System.out.println("Adapted existing grid for panel " + currentPanelId);
                } else {
                    // Create a basic grid from the saved config that can be refined later
                    GridConfiguration config = model.getGridConfig();
                    String[] rangeParts = config.getNumberRange().split("-");
                    if (rangeParts.length == 2) {
                        try {
                            int startNumber = Integer.parseInt(rangeParts[0].trim());
                            int endNumber = Integer.parseInt(rangeParts[1].trim());
                            
                            // Create a placeholder grid that will be positioned when user defines corners
                            currentGrid = new GridDefinition(
                                0, 0, // Placeholder coordinates - will be set when user defines grid
                                100, 100, // Placeholder coordinates
                                config.getColumns(),
                                config.getRows(),
                                startNumber,
                                endNumber,
                                FillOrder.valueOf(config.getFillOrder()),
                                currentPanelId
                            );
                            
                            System.out.println("Created placeholder grid for panel " + currentPanelId + 
                                             " with config: " + config.getColumns() + "x" + config.getRows());
                            
                        } catch (Exception e) {
                            System.err.println("Error creating grid from config: " + e.getMessage());
                            currentGrid = null;
                        }
                    }
                }
            }
        }
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
        // First priority: Try to save grid config from UI fields (whether grid mode is enabled or not)
        String numberRange = view.getGridNumberRange();
        int columns = view.getGridColumns();
        int rows = view.getGridRows();
        String fillOrder = view.getGridFillOrder();
        
        // Save if we have valid configuration in the UI fields
        if (numberRange != null && !numberRange.trim().isEmpty() && columns > 0 && rows > 0) {
            GridConfiguration gridConfig = new GridConfiguration(numberRange, columns, rows, fillOrder);
            model.setGridConfig(gridConfig);
            String source = view.isGridModeEnabled() ? "grid mode" : "UI fields";
            System.out.println("Saved grid config from " + source + ": " + columns + "x" + rows + " range " + numberRange);
            return;
        }
        
        // Second priority: Try to infer and save grid config from coordinate patterns (if coordinates exist)
        // This ensures templates created manually will have grid config for future loading
        GridConfiguration inferredConfig = inferGridConfigurationFromCoordinates();
        if (inferredConfig != null) {
            model.setGridConfig(inferredConfig);
            System.out.println("Inferred and saved grid config: " + inferredConfig.getColumns() + "x" + 
                inferredConfig.getRows() + " range " + inferredConfig.getNumberRange());
        } else {
            // No grid configuration available - don't save any grid config
            model.setGridConfig(null);
            System.out.println("No grid configuration available to save");
        }
    }
    
    /**
     * Infer grid configuration from existing coordinate patterns for saving
     */
    private GridConfiguration inferGridConfigurationFromCoordinates() {
        if (model == null || model.getPlayPanels() == null || model.getPlayPanels().isEmpty()) {
            return null;
        }
        
        // Find the first panel with sufficient coordinates
        for (PlayPanel panel : model.getPlayPanels()) {
            if (panel.getMainNumbers() != null && panel.getMainNumbers().size() >= 10) {
                Map<String, Coordinate> mainNumbers = panel.getMainNumbers();
                
                try {
                    // Determine number range
                    int minNumber = mainNumbers.keySet().stream()
                        .filter(key -> key.matches("\\d+"))
                        .mapToInt(Integer::parseInt)
                        .min().orElse(1);
                    int maxNumber = mainNumbers.keySet().stream()
                        .filter(key -> key.matches("\\d+"))
                        .mapToInt(Integer::parseInt)
                        .max().orElse(35);
                    
                    // Analyze grid structure from coordinate patterns
                    GridConfig gridStructure = analyzeExistingCoordinates(mainNumbers, minNumber, maxNumber);
                    if (gridStructure != null) {
                        String numberRange = minNumber + "-" + maxNumber;
                        return new GridConfiguration(numberRange, gridStructure.columns, gridStructure.rows, gridStructure.fillOrder);
                    }
                    
                } catch (Exception e) {
                    System.err.println("Error inferring grid config for save: " + e.getMessage());
                }
            }
        }
        
        return null;
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
        
        // Try to reconstruct grid definition for column/row selection
        reconstructGridDefinitionFromData();
        
        // Notify view if grid was successfully reconstructed
        if (currentGrid != null && view != null) {
            System.out.println("Grid reconstructed successfully for panel: " + currentGrid.getPanelId());
            // Force update of UI state now that we have a valid grid
            view.updateGridMappingStatus("Grid loaded from template. Column/row editing available.");
        }
    }
    
    /**
     * Reconstruct grid definition from existing coordinate data to enable column/row selection
     */
    private void reconstructGridDefinitionFromData() {
        if (model == null || model.getPlayPanels() == null) {
            return;
        }
        
        // Try to reconstruct grid for any panel that has coordinates
        // First check all panels to find one with coordinates
        String targetPanelId = null;
        PlayPanel targetPanel = null;
        
        for (PlayPanel panel : model.getPlayPanels()) {
            if (panel.getMainNumbers() != null && !panel.getMainNumbers().isEmpty()) {
                targetPanelId = panel.getPanelId();
                targetPanel = panel;
                break; // Use the first panel with coordinates
            }
        }
        
        if (targetPanel == null) {
            currentGrid = null;
            return;
        }
        
        // ALWAYS prioritize saved grid config if available - don't fall back to coordinate inference
        if (model.getGridConfig() != null && model.getGridConfig().isValid()) {
            GridConfiguration config = model.getGridConfig();
            
            // Parse number range
            String[] rangeParts = config.getNumberRange().split("-");
            if (rangeParts.length == 2) {
                try {
                    int startNumber = Integer.parseInt(rangeParts[0].trim());
                    int endNumber = Integer.parseInt(rangeParts[1].trim());
                    
                    // Calculate bounds from actual coordinates
                    double minX = Double.MAX_VALUE, minY = Double.MAX_VALUE;
                    double maxX = Double.MIN_VALUE, maxY = Double.MIN_VALUE;
                    
                    for (Coordinate coord : targetPanel.getMainNumbers().values()) {
                        minX = Math.min(minX, coord.getX());
                        minY = Math.min(minY, coord.getY());
                        maxX = Math.max(maxX, coord.getX());
                        maxY = Math.max(maxY, coord.getY());
                    }
                    
                    // Add some padding to create proper grid bounds
                    int markWidth = model.getMark() != null ? model.getMark().getWidth() : 20;
                    int markHeight = model.getMark() != null ? model.getMark().getHeight() : 20;
                    double padding = Math.max(markWidth, markHeight) / 2.0;
                    
                    currentGrid = new GridDefinition(
                        minX - padding, minY - padding,
                        maxX + padding, maxY + padding,
                        config.getColumns(), config.getRows(), // ALWAYS use saved config dimensions
                        startNumber, endNumber,
                        FillOrder.valueOf(config.getFillOrder()),
                        targetPanelId // Use the panel that actually has coordinates
                    );
                    
                    System.out.println("Successfully reconstructed grid from SAVED CONFIG: " + 
                        config.getColumns() + "x" + config.getRows() + 
                        " for panel " + targetPanelId + 
                        " with " + targetPanel.getMainNumbers().size() + " coordinates");
                    return; // Successfully used saved config - don't try coordinate inference
                        
                } catch (Exception e) {
                    System.err.println("Grid reconstruction from saved config failed: " + e.getMessage());
                    e.printStackTrace();
                    // Don't set currentGrid to null here - still try coordinate inference as final fallback
                }
            }
        }
        
        // Only try coordinate inference if saved config is not available or failed to parse
        // This is a fallback for older templates that don't have grid config
        System.out.println("No saved grid config found or config failed. Attempting to infer grid from coordinate patterns as fallback...");
        
        if (targetPanel != null) {
            GridDefinition inferredGrid = inferGridFromCoordinates(targetPanel, targetPanelId);
            if (inferredGrid != null && inferredGrid.isValid()) {
                currentGrid = inferredGrid;
                System.out.println("Successfully inferred grid as FALLBACK: " + inferredGrid.getColumns() + "x" + inferredGrid.getRows() + " for panel " + targetPanelId);
            } else {
                System.out.println("Failed to infer grid from coordinate patterns - column/row fine-tuning will not be available");
                currentGrid = null;
            }
        } else {
            System.out.println("No panels with coordinates found - column/row fine-tuning will not be available");
            currentGrid = null;
        }
    }
    
    /**
     * Create grid from existing template data - prioritize saved config over coordinate inference
     */
    private GridDefinition createGridFromExistingData(PlayPanel panel, String panelId) {
        // First priority: Use saved grid configuration if available
        if (model.getGridConfig() != null && model.getGridConfig().isValid()) {
            GridDefinition gridFromConfig = createGridFromSavedConfig(panel, panelId);
            if (gridFromConfig != null) {
                System.out.println("Using SAVED grid config: " + 
                    model.getGridConfig().getColumns() + "x" + model.getGridConfig().getRows() + 
                    " for panel " + panelId);
                return gridFromConfig;
            }
        }
        
        // Second priority: Try to infer from coordinate patterns (fallback for old templates)
        System.out.println("Falling back to coordinate inference for panel " + panelId);
        return inferGridFromCoordinates(panel, panelId);
    }
    
    /**
     * Create grid using saved grid configuration and actual coordinate bounds
     */
    private GridDefinition createGridFromSavedConfig(PlayPanel panel, String panelId) {
        GridConfiguration config = model.getGridConfig();
        Map<String, Coordinate> mainNumbers = panel.getMainNumbers();
        
        if (mainNumbers == null || mainNumbers.isEmpty()) {
            return null;
        }
        
        try {
            // Parse number range from config
            String[] rangeParts = config.getNumberRange().split("-");
            if (rangeParts.length != 2) {
                return null;
            }
            
            int startNumber = Integer.parseInt(rangeParts[0].trim());
            int endNumber = Integer.parseInt(rangeParts[1].trim());
            
            // Calculate actual bounds from coordinates
            double minX = mainNumbers.values().stream().mapToDouble(Coordinate::getX).min().orElse(0);
            double maxX = mainNumbers.values().stream().mapToDouble(Coordinate::getX).max().orElse(0);
            double minY = mainNumbers.values().stream().mapToDouble(Coordinate::getY).min().orElse(0);
            double maxY = mainNumbers.values().stream().mapToDouble(Coordinate::getY).max().orElse(0);
            
            // Add padding
            double padding = 20;
            
            // Use saved config for dimensions and fill order
            return new GridDefinition(
                minX - padding, minY - padding,
                maxX + padding, maxY + padding,
                config.getColumns(), config.getRows(), // Use saved columns/rows
                startNumber, endNumber,
                FillOrder.valueOf(config.getFillOrder()), // Use saved fill order
                panelId
            );
            
        } catch (Exception e) {
            System.err.println("Error creating grid from saved config: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * Fallback: Infer grid structure from coordinate patterns when no saved grid config exists
     */
    private GridDefinition inferGridFromCoordinates(PlayPanel panel, String panelId) {
        Map<String, Coordinate> mainNumbers = panel.getMainNumbers();
        if (mainNumbers == null || mainNumbers.size() < 10) {
            return null; // Need at least 10 coordinates to infer pattern
        }
        
        try {
            // Get all coordinates and number values
            List<Integer> numbers = mainNumbers.keySet().stream()
                .filter(key -> key.matches("\\d+"))
                .map(Integer::parseInt)
                .sorted()
                .collect(Collectors.toList());
                
            if (numbers.isEmpty()) {
                return null;
            }
            
            // Determine number range
            int startNumber = numbers.get(0);
            int endNumber = numbers.get(numbers.size() - 1);
            
            // Group coordinates by X and Y positions to determine grid structure
            Set<Integer> uniqueXPositions = mainNumbers.values().stream()
                .mapToInt(Coordinate::getX)
                .boxed()
                .collect(Collectors.toSet());
                
            Set<Integer> uniqueYPositions = mainNumbers.values().stream()
                .mapToInt(Coordinate::getY)
                .boxed()
                .collect(Collectors.toSet());
            
            int inferredColumns = uniqueXPositions.size();
            int inferredRows = uniqueYPositions.size();
            
            // Validate the inferred grid makes sense
            int totalNumbers = endNumber - startNumber + 1;
            int gridCells = inferredColumns * inferredRows;
            
            if (Math.abs(gridCells - totalNumbers) > totalNumbers * 0.2) {
                // Grid structure doesn't match well
                System.out.println("WARNING: Inferred grid " + inferredColumns + "x" + inferredRows + 
                    " doesn't match " + totalNumbers + " numbers well (" + mainNumbers.size() + " coordinates available). " +
                    "This suggests saved grid config should be used instead of coordinate inference.");
                return null;
            }
            
            // Determine fill order by analyzing coordinate patterns
            FillOrder fillOrder = inferFillOrderFromCoordinates(mainNumbers, numbers);
            
            // Calculate grid bounds
            double minX = mainNumbers.values().stream().mapToDouble(Coordinate::getX).min().orElse(0);
            double maxX = mainNumbers.values().stream().mapToDouble(Coordinate::getX).max().orElse(0);
            double minY = mainNumbers.values().stream().mapToDouble(Coordinate::getY).min().orElse(0);
            double maxY = mainNumbers.values().stream().mapToDouble(Coordinate::getY).max().orElse(0);
            
            // Add padding for grid bounds
            double padding = 20;
            
            return new GridDefinition(
                minX - padding, minY - padding,
                maxX + padding, maxY + padding,
                inferredColumns, inferredRows,
                startNumber, endNumber,
                fillOrder, panelId
            );
            
        } catch (Exception e) {
            System.err.println("Error inferring grid from coordinates: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * Infer fill order from coordinate patterns
     */
    private FillOrder inferFillOrderFromCoordinates(Map<String, Coordinate> coordinates, List<Integer> sortedNumbers) {
        if (sortedNumbers.size() < 4) {
            return FillOrder.COLUMN_BOTTOM_TO_TOP; // Default
        }
        
        try {
            // Check first few numbers to determine pattern
            Coordinate first = coordinates.get(String.valueOf(sortedNumbers.get(0)));
            Coordinate second = coordinates.get(String.valueOf(sortedNumbers.get(1)));
            Coordinate third = coordinates.get(String.valueOf(sortedNumbers.get(2)));
            
            if (first == null || second == null || third == null) {
                return FillOrder.COLUMN_BOTTOM_TO_TOP;
            }
            
            // Check if first few numbers are in same column (X position)
            if (first.getX() == second.getX()) {
                // Column-wise fill
                if (first.getY() > second.getY()) {
                    return FillOrder.COLUMN_BOTTOM_TO_TOP;
                } else {
                    return FillOrder.COLUMN_TOP_TO_BOTTOM;
                }
            } else if (first.getY() == second.getY()) {
                // Row-wise fill
                if (first.getX() < second.getX()) {
                    return FillOrder.ROW_LEFT_TO_RIGHT;
                } else {
                    return FillOrder.ROW_RIGHT_TO_LEFT;
                }
            }
            
        } catch (Exception e) {
            System.err.println("Error inferring fill order: " + e.getMessage());
        }
        
        return FillOrder.COLUMN_BOTTOM_TO_TOP; // Safe default
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
            int totalNumbers = maxNumber - minNumber + 1;
            int expectedCells = columns * rows;
            
            System.out.println("Grid analysis: Found " + columns + " unique X positions, " + rows + " unique Y positions");
            System.out.println("Grid analysis: Expected " + totalNumbers + " numbers (" + minNumber + "-" + maxNumber + "), calculated grid " + columns + "x" + rows + "=" + expectedCells + " cells");
            
            // For texas-lottotexas case: 54 numbers should be 9x6, not 20x15
            // The inference might be wrong due to slight coordinate variations
            // Let's try to detect the correct grid size for common lottery formats
            
            // First check if this matches common lottery patterns
            GridConfig knownPattern = detectKnownLotteryPattern(totalNumbers, coordinates);
            if (knownPattern != null) {
                System.out.println("Detected known lottery pattern: " + knownPattern.columns + "x" + knownPattern.rows);
                return knownPattern;
            }
            
            // Validate the inferred grid makes sense
            if (Math.abs(expectedCells - totalNumbers) > totalNumbers * 0.3) {
                System.out.println("Grid analysis: cell count (" + expectedCells + ") doesn't match number count (" + totalNumbers + ") well - difference too large");
                
                // Try to find a better grid size that matches the number count
                GridConfig betterGrid = findBetterGridSize(totalNumbers, coordinatesByX, coordinatesByY);
                if (betterGrid != null) {
                    System.out.println("Found better grid size: " + betterGrid.columns + "x" + betterGrid.rows);
                    return betterGrid;
                }
                
                return null; // Grid structure doesn't match the data well
            }
            
            // Check if coordinates are reasonably distributed across grid positions
            boolean wellDistributed = coordinatesByX.values().stream()
                .allMatch(list -> list.size() >= Math.max(1, rows * 0.2)) && // Each column has at least 20% of expected numbers (more lenient)
                coordinatesByY.values().stream()
                .allMatch(list -> list.size() >= Math.max(1, columns * 0.2)); // Each row has at least 20% of expected numbers (more lenient)
            
            if (!wellDistributed) {
                System.out.println("Grid analysis: coordinates not well distributed across grid");
                // Still try to provide a reasonable grid size even if distribution is poor
                GridConfig reasonableGrid = findReasonableGridSize(totalNumbers);
                if (reasonableGrid != null) {
                    System.out.println("Using reasonable grid size fallback: " + reasonableGrid.columns + "x" + reasonableGrid.rows);
                    return reasonableGrid;
                }
                return null;
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
    
    /**
     * Detect known lottery patterns based on total numbers
     */
    private GridConfig detectKnownLotteryPattern(int totalNumbers, Map<String, Coordinate> coordinates) {
        switch (totalNumbers) {
            case 35: // Cash Five
                return new GridConfig(7, 5, "COLUMN_BOTTOM_TO_TOP");
            case 54: // Lotto Texas
                return new GridConfig(9, 6, "COLUMN_BOTTOM_TO_TOP");
            case 69: // Powerball main numbers
                return new GridConfig(10, 7, "ROW_LEFT_TO_RIGHT");
            case 70: // Mega Millions
                return new GridConfig(10, 7, "ROW_LEFT_TO_RIGHT");
            default:
                return null;
        }
    }
    
    /**
     * Find a better grid size that matches the number count more closely
     */
    private GridConfig findBetterGridSize(int totalNumbers, Map<Integer, List<Coordinate>> coordinatesByX, Map<Integer, List<Coordinate>> coordinatesByY) {
        // Try to find factors of totalNumbers that make more sense
        for (int cols = 1; cols <= 20; cols++) {
            if (totalNumbers % cols == 0) {
                int rows = totalNumbers / cols;
                if (rows > 0 && rows <= 20) {
                    // Check if this grid size is reasonable given the coordinate distribution
                    boolean reasonable = Math.abs(coordinatesByX.size() - cols) <= 2 && 
                                       Math.abs(coordinatesByY.size() - rows) <= 2;
                    if (reasonable) {
                        return new GridConfig(cols, rows, "COLUMN_BOTTOM_TO_TOP");
                    }
                }
            }
        }
        return null;
    }
    
    /**
     * Provide a reasonable grid size based on total numbers
     */
    private GridConfig findReasonableGridSize(int totalNumbers) {
        // Find a reasonable grid size for the number count
        int bestCols = 7, bestRows = 5; // Default
        int bestDiff = Math.abs(bestCols * bestRows - totalNumbers);
        
        for (int cols = 5; cols <= 12; cols++) {
            for (int rows = 4; rows <= 8; rows++) {
                int cells = cols * rows;
                int diff = Math.abs(cells - totalNumbers);
                if (diff < bestDiff && cells >= totalNumbers) {
                    bestCols = cols;
                    bestRows = rows;
                    bestDiff = diff;
                }
            }
        }
        
        return new GridConfig(bestCols, bestRows, "COLUMN_BOTTOM_TO_TOP");
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
                // Reset Scanner Mark ID counter when all are cleared
                ScannerMark.setNextId(1);
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
        // Use the preset size for new scanner marks instead of the provided width/height
        ScannerMark newMark = new ScannerMark(x, y, newScannerMarkWidth, newScannerMarkHeight);
        model.getScannerMarks().add(newMark);
        undoStack.add(() -> {
            model.getScannerMarks().remove(newMark);
            // Use size-preserving redraw to prevent affecting other markings
            redrawAllMarkingsPreservingSizes();
        });
        // Just draw the new scanner mark, don't redraw everything
        view.drawScannerMark(newMark);
        
        // Update scanner mark count and ensure consistent numbering
        view.setScannerMarkCount(model.getScannerMarks().size());
        
        // Update new mark size tracking
        lastScannerMarkWidth = (int) newMark.getWidth();
        lastScannerMarkHeight = (int) newMark.getHeight();
        
        // Auto-select the newly placed scanner mark so controls appear immediately
        view.selectScannerMark(newMark);
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
        
        // Record the size for this specific Global Option name
        int currentWidth = model.getMark().getWidth();
        int currentHeight = model.getMark().getHeight();
        globalOptionSizes.put(optionName, new int[]{currentWidth, currentHeight});
        
        Coordinate oldGlobal = model.getGlobalOptions().put(optionName, coordinate);
        undoStack.add(() -> {
            model.getGlobalOptions().put(optionName, oldGlobal);
            // Also restore the size mapping
            if (oldGlobal == null) {
                globalOptionSizes.remove(optionName);
            }
        });
        redrawAllMarkings();
    }

    /**
     * Remove last marking for a specific panel (simplified version)
     */
    public void removeLastMarkingForPanel(String panelId) {
        if (panelId == null) return;
        
        view.clearPreviewRectangles();
        
        // Find the panel and remove the last coordinate if any exist
        PlayPanel panel = getOrCreatePlayPanel(panelId);
        if (panel.getMainNumbers() != null && !panel.getMainNumbers().isEmpty()) {
            // Find the coordinate with the highest number (assuming numbers are added sequentially)
            String lastNumber = panel.getMainNumbers().keySet().stream()
                .filter(key -> key.matches("\\d+"))
                .map(Integer::parseInt)
                .max(Integer::compareTo)
                .map(String::valueOf)
                .orElse(null);
                
            if (lastNumber != null) {
                panel.getMainNumbers().remove(lastNumber);
                redrawAllMarkings();
            }
        }
    }
    
    public void removeLastMarking() {
        view.clearPreviewRectangles();
        if (!undoStack.isEmpty()) {
            // Store current mark size to preserve it
            int originalWidth = model.getMark().getWidth();
            int originalHeight = model.getMark().getHeight();
            
            undoStack.remove(undoStack.size() - 1).run();
            
            // The undo action might have changed the global mark size
            // Restore it to prevent scanner mark sizes from affecting betslip numbers
            model.getMark().setWidth(originalWidth);
            model.getMark().setHeight(originalHeight);
            
            // Renumber Scanner Marks to keep IDs sequential (1, 2, 3, ...)
            renumberScannerMarks();
            
            // Redraw with preserved sizes
            redrawAllMarkingsPreservingSizes();
        }
    }
    
    /**
     * Redraw all markings while preserving individual scanner mark sizes
     */
    private void redrawAllMarkingsPreservingSizes() {
        view.clearAllRectangles();
        int w = model.getMark().getWidth();
        int h = model.getMark().getHeight();
        
        // Redraw panels with individual sizes where applicable
        for (PlayPanel panel : model.getPlayPanels()) {
            String panelId = panel.getPanelId();
            
            // Main and bonus numbers always use standard sizes
            panel.getMainNumbers().values().forEach(c -> view.drawRectangle(c, w, h, "MAIN_NUMBER", panelId));
            panel.getBonusNumbers().values().forEach(c -> view.drawRectangle(c, w, h, "BONUS_NUMBER", panelId));
            
            // Quick Pick uses individual size if set
            if (panel.getQuickPick() != null) {
                int[] qpSize = quickPickSizes.getOrDefault(panelId, new int[]{w, h});
                view.drawRectangle(panel.getQuickPick(), qpSize[0], qpSize[1], "QUICK_PICK", panelId);
            }
        }
        
        // Redraw global options with individual sizes if set
        if (model.getGlobalOptions() != null) {
            model.getGlobalOptions().forEach((optionName, coord) -> {
                int[] goSize = globalOptionSizes.getOrDefault(optionName, new int[]{w, h});
                view.drawRectangle(coord, goSize[0], goSize[1], "GLOBAL_OPTION", null);
            });
        }
        
        // Redraw scanner marks with their individual sizes (NOT standard mark size)
        model.getScannerMarks().forEach(view::drawScannerMark);
        view.setScannerMarkCount(model.getScannerMarks().size());
    }

    public void updateMarkSize(int width, int height) {
        String currentMode = view.getSelectedMappingMode();
        
        if ("Scanner Mark".equals(currentMode)) {
            // For scanner marks, don't update the global mark size
            // Scanner mark resizing should be handled separately
            return;
        }
        
        model.getMark().setWidth(width);
        model.getMark().setHeight(height);
        // Only redraw the current panel's markings, not all panels
        redrawCurrentPanelMarkings();
    }
    
    /**
     * Update mark size for the current panel and mode only, without affecting other panels or modes
     */
    public void updateMarkSizeForCurrentPanel(int width, int height, String mode) {
        String currentPanelId = view.getSelectedPanel();
        if (currentPanelId == null) return;
        
        PlayPanel currentPanel = getOrCreatePlayPanel(currentPanelId);
        
        // For Main/Bonus number modes, we need to redraw all markings but only update
        // the specific type with the new size
        if ("Main Number".equals(mode) || "Bonus Number".equals(mode)) {
            // Clear all rectangles and redraw everything to maintain consistency
            view.clearAllRectangles();
            
            // Redraw all panels with their original sizes, except current panel's specific type
            for (PlayPanel panel : model.getPlayPanels()) {
                int w = model.getMark().getWidth();
                int h = model.getMark().getHeight();
                
                // Use custom size only for the current panel and mode
                if (panel.getPanelId().equals(currentPanelId)) {
                    if ("Main Number".equals(mode)) {
                        // Use custom size for main numbers in current panel
                        panel.getMainNumbers().values().forEach(c -> 
                            view.drawRectangle(c, width, height, "MAIN_NUMBER", panel.getPanelId()));
                        // Use normal size for bonus numbers in current panel
                        panel.getBonusNumbers().values().forEach(c -> 
                            view.drawRectangle(c, w, h, "BONUS_NUMBER", panel.getPanelId()));
                    } else if ("Bonus Number".equals(mode)) {
                        // Use normal size for main numbers in current panel
                        panel.getMainNumbers().values().forEach(c -> 
                            view.drawRectangle(c, w, h, "MAIN_NUMBER", panel.getPanelId()));
                        // Use custom size for bonus numbers in current panel
                        panel.getBonusNumbers().values().forEach(c -> 
                            view.drawRectangle(c, width, height, "BONUS_NUMBER", panel.getPanelId()));
                    }
                } else {
                    // Use normal size for all other panels
                    panel.getMainNumbers().values().forEach(c -> 
                        view.drawRectangle(c, w, h, "MAIN_NUMBER", panel.getPanelId()));
                    panel.getBonusNumbers().values().forEach(c -> 
                        view.drawRectangle(c, w, h, "BONUS_NUMBER", panel.getPanelId()));
                }
                
                // Redraw quick pick with normal size
                if (panel.getQuickPick() != null) {
                    view.drawRectangle(panel.getQuickPick(), w, h, "QUICK_PICK", panel.getPanelId());
                }
            }
            
            // Redraw global options and scanner marks with their own sizes
            model.getGlobalOptions().values().forEach(c -> 
                view.drawRectangle(c, model.getMark().getWidth(), model.getMark().getHeight(), "GLOBAL_OPTION", null));
            model.getScannerMarks().forEach(view::drawScannerMark);
            view.setScannerMarkCount(model.getScannerMarks().size());
        }
    }
    
    /**
     * Redraw markings only for the currently selected panel
     */
    public void redrawCurrentPanelMarkings() {
        String currentPanelId = view.getSelectedPanel();
        if (currentPanelId == null) return;
        
        // Clear only rectangles belonging to the current panel
        clearCurrentPanelRectangles();
        
        // Redraw only current panel's markings
        int w = model.getMark().getWidth();
        int h = model.getMark().getHeight();
        
        PlayPanel currentPanel = getOrCreatePlayPanel(currentPanelId);
        currentPanel.getMainNumbers().values().forEach(c -> view.drawRectangle(c, w, h, "MAIN_NUMBER", currentPanelId));
        currentPanel.getBonusNumbers().values().forEach(c -> view.drawRectangle(c, w, h, "BONUS_NUMBER", currentPanelId));
        if (currentPanel.getQuickPick() != null) {
            view.drawRectangle(currentPanel.getQuickPick(), w, h, "QUICK_PICK", currentPanelId);
        }
        
        // Also redraw global options and scanner marks since they're not panel-specific
        model.getGlobalOptions().values().forEach(c -> view.drawRectangle(c, w, h, "GLOBAL_OPTION", null));
        model.getScannerMarks().forEach(view::drawScannerMark);
        view.setScannerMarkCount(model.getScannerMarks().size());
    }
    
    /**
     * Clear rectangles that belong to the current panel only
     */
    private void clearCurrentPanelRectangles() {
        String currentPanelId = view.getSelectedPanel();
        if (currentPanelId != null) {
            view.clearPanelRectangles(currentPanelId);
        }
    }

    public void updateScannerMarkSize(ScannerMark mark, int width, int height) {
        // Update the Scanner Mark size directly without adding to undo stack
        // Size adjustments are not creation/removal operations, so they shouldn't be undoable
        // This prevents size changes from interfering with the "Clear Last Marking" functionality
        mark.setWidth(width);
        mark.setHeight(height);
        view.updateScannerMarkRectangle(mark, width, height);
        
        // Remember the last used Scanner Mark size for new marks
        lastScannerMarkWidth = width;
        lastScannerMarkHeight = height;
        
        // Update the size for new Scanner Marks to match the current adjustment
        setNewScannerMarkSize(width, height);
    }
    
    public void updateScannerMarkDefaultSize(int width, int height) {
        // Update the default mark size which will be used for new scanner marks
        model.getMark().setWidth(width);
        model.getMark().setHeight(height);
        // No need to redraw anything since this just sets the default for future marks
    }
    
    /**
     * @deprecated This method is no longer used. Each Scanner Mark has individual size.
     * Use updateScannerMarkSize(ScannerMark mark, int width, int height) for individual marks.
     * Use setNewScannerMarkSize(int width, int height) to set size for newly placed marks.
     */
    @Deprecated
    public void updateAllScannerMarksSize(int width, int height) {
        // METHOD DEPRECATED - Scanner marks now have individual sizes
        // Each scanner mark maintains its own size independently
        // This method is kept for backward compatibility but should not be used
        System.out.println("WARNING: updateAllScannerMarksSize() is deprecated. Scanner marks now have individual sizes.");
    }
    
    public void updateMarkSizeForMode(int width, int height, String mode) {
        if ("Global Option".equals(mode)) {
            // Redraw only global options without affecting other marking types
            redrawGlobalOptionsOnly(width, height);
        } else if ("Quick Pick".equals(mode)) {
            // Redraw only quick pick marks without affecting other marking types
            redrawQuickPickMarksOnly(width, height);
        }
    }
    
    private void redrawGlobalOptionsOnly(int width, int height) {
        // Clear and redraw everything to maintain visual consistency
        view.clearAllRectangles();
        
        // Redraw all markings with normal sizes except global options
        int w = model.getMark().getWidth();
        int h = model.getMark().getHeight();
        
        // Redraw all panels with normal sizes
        for (PlayPanel panel : model.getPlayPanels()) {
            panel.getMainNumbers().values().forEach(c -> 
                view.drawRectangle(c, w, h, "MAIN_NUMBER", panel.getPanelId()));
            panel.getBonusNumbers().values().forEach(c -> 
                view.drawRectangle(c, w, h, "BONUS_NUMBER", panel.getPanelId()));
            if (panel.getQuickPick() != null) {
                view.drawRectangle(panel.getQuickPick(), w, h, "QUICK_PICK", panel.getPanelId());
            }
        }
        
        // Redraw global options with custom size
        model.getGlobalOptions().values().forEach(c -> 
            view.drawRectangle(c, width, height, "GLOBAL_OPTION", null));
            
        // Redraw scanner marks with their individual sizes
        model.getScannerMarks().forEach(view::drawScannerMark);
        view.setScannerMarkCount(model.getScannerMarks().size());
    }
    
    private void redrawQuickPickMarksOnly(int width, int height) {
        // Clear and redraw everything to maintain visual consistency
        view.clearAllRectangles();
        
        // Redraw all markings with normal sizes except quick pick
        int w = model.getMark().getWidth();
        int h = model.getMark().getHeight();
        
        // Redraw all panels
        for (PlayPanel panel : model.getPlayPanels()) {
            panel.getMainNumbers().values().forEach(c -> 
                view.drawRectangle(c, w, h, "MAIN_NUMBER", panel.getPanelId()));
            panel.getBonusNumbers().values().forEach(c -> 
                view.drawRectangle(c, w, h, "BONUS_NUMBER", panel.getPanelId()));
            // Use custom size for quick pick marks
            if (panel.getQuickPick() != null) {
                view.drawRectangle(panel.getQuickPick(), width, height, "QUICK_PICK", panel.getPanelId());
            }
        }
        
        // Redraw global options with normal size
        model.getGlobalOptions().values().forEach(c -> 
            view.drawRectangle(c, w, h, "GLOBAL_OPTION", null));
            
        // Redraw scanner marks with their individual sizes
        model.getScannerMarks().forEach(view::drawScannerMark);
        view.setScannerMarkCount(model.getScannerMarks().size());
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
            panel.getMainNumbers().values().forEach(c -> view.drawRectangle(c, w, h, "MAIN_NUMBER", panel.getPanelId()));
            panel.getBonusNumbers().values().forEach(c -> view.drawRectangle(c, w, h, "BONUS_NUMBER", panel.getPanelId()));
            if (panel.getQuickPick() != null) view.drawRectangle(panel.getQuickPick(), w, h, "QUICK_PICK", panel.getPanelId());
        }
        model.getGlobalOptions().values().forEach(c -> view.drawRectangle(c, w, h, "GLOBAL_OPTION", null));
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
    
    /**
     * Check if we have a valid grid configuration that can be used for grid mapping
     */
    public boolean hasValidGridConfiguration() {
        return model != null && 
               model.getGridConfig() != null && 
               model.getGridConfig().isValid();
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
    
    /**
     * Clear markings data for a specific panel only
     */
    public void clearPanelMarkingsData(String panelId) {
        if (model == null || panelId == null) return;
        
        // Find and clear the specific panel
        if (model.getPlayPanels() != null) {
            PlayPanel panelToClear = model.getPlayPanels().stream()
                .filter(p -> panelId.equals(p.getPanelId()))
                .findFirst()
                .orElse(null);
                
            if (panelToClear != null) {
                if (panelToClear.getMainNumbers() != null) {
                    panelToClear.getMainNumbers().clear();
                }
                if (panelToClear.getBonusNumbers() != null) {
                    panelToClear.getBonusNumbers().clear();
                }
                panelToClear.setQuickPick(null);
            }
        }
        
        // Note: Don't clear global options, scanner marks, or reset grid - these are shared
        // Only clear undo stack items related to this panel (simplified: clear all for now)
        undoStack.clear();
        
        // Update scanner mark count (this is global, but still accurate)
        if (view != null) {
            int totalScannerMarks = model.getScannerMarks() != null ? model.getScannerMarks().size() : 0;
            view.setScannerMarkCount(totalScannerMarks);
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
    
    // Individual marking size management methods
    private Map<String, int[]> quickPickSizes = new HashMap<>();  // panelId -> [width, height]
    private Map<String, int[]> globalOptionSizes = new HashMap<>(); // optionName -> [width, height]
    
    // Size for new Scanner Marks (when placing new ones)
    private int newScannerMarkWidth = 20;  // Default size for new scanner marks
    private int newScannerMarkHeight = 20;
    
    // Track the last used Scanner Mark size to maintain consistency
    private int lastScannerMarkWidth = 20;
    private int lastScannerMarkHeight = 20;
    
    /**
     * Get the current dimensions for a marking (Quick Pick or Global Option)
     */
    public int[] getMarkingDimensions(Object coordinateInfo) {
        if (coordinateInfo instanceof TemplateCreatorController.CoordinateInfo) {
            TemplateCreatorController.CoordinateInfo coordInfo = (TemplateCreatorController.CoordinateInfo) coordinateInfo;
            String type = coordInfo.getType();
            String panelId = coordInfo.getPanelId();
            
            if ("QUICK_PICK".equals(type) && panelId != null) {
                return quickPickSizes.getOrDefault(panelId, 
                    new int[]{model.getMark().getWidth(), model.getMark().getHeight()});
            } else if ("GLOBAL_OPTION".equals(type)) {
                // Find the option name for this coordinate
                String optionName = findGlobalOptionName(coordInfo.getCoordinate());
                if (optionName != null) {
                    return globalOptionSizes.getOrDefault(optionName,
                        new int[]{model.getMark().getWidth(), model.getMark().getHeight()});
                }
            }
        }
        return new int[]{model.getMark().getWidth(), model.getMark().getHeight()};
    }
    
    /**
     * Update the size of a specific marking (Quick Pick or Global Option)
     */
    public void updateMarkingSize(Object coordinateInfo, int width, int height) {
        if (coordinateInfo instanceof TemplateCreatorController.CoordinateInfo) {
            TemplateCreatorController.CoordinateInfo coordInfo = (TemplateCreatorController.CoordinateInfo) coordinateInfo;
            String type = coordInfo.getType();
            String panelId = coordInfo.getPanelId();
            Coordinate coordinate = coordInfo.getCoordinate();
            
            if ("QUICK_PICK".equals(type) && panelId != null) {
                // Store the custom size for this Quick Pick
                quickPickSizes.put(panelId, new int[]{width, height});
                
                // Update the visual rectangle
                view.clearAllRectangles();
                redrawAllMarkingsPreservingSizes();
                
            } else if ("GLOBAL_OPTION".equals(type)) {
                // For global options, find which option name this coordinate matches
                String optionName = findGlobalOptionName(coordinate);
                if (optionName != null) {
                    globalOptionSizes.put(optionName, new int[]{width, height});
                    
                    // Update the visual rectangle
                    view.clearAllRectangles();
                    redrawAllMarkingsPreservingSizes();
                }
            }
        }
    }
    
    /**
     * Find the global option name that matches a coordinate
     */
    private String findGlobalOptionName(Coordinate targetCoord) {
        if (model.getGlobalOptions() != null) {
            for (Map.Entry<String, Coordinate> entry : model.getGlobalOptions().entrySet()) {
                Coordinate coord = entry.getValue();
                if (coord.getX() == targetCoord.getX() && coord.getY() == targetCoord.getY()) {
                    return entry.getKey();
                }
            }
        }
        return null;
    }
    
    /**
     * Set the size for newly placed Scanner Marks
     */
    public void setNewScannerMarkSize(int width, int height) {
        this.newScannerMarkWidth = width;
        this.newScannerMarkHeight = height;
    }
    
    /**
     * Renumber all Scanner Marks to keep IDs sequential (1, 2, 3, ...)
     * This ensures the ID matches the visual count and eliminates gaps
     */
    private void renumberScannerMarks() {
        if (model.getScannerMarks() == null || model.getScannerMarks().isEmpty()) {
            return;
        }
        
        // Renumber all Scanner Marks sequentially starting from 1
        for (int i = 0; i < model.getScannerMarks().size(); i++) {
            ScannerMark mark = model.getScannerMarks().get(i);
            mark.setId(i + 1);  // Set ID to 1, 2, 3, ...
        }
        
        // Update the nextId counter to the next available ID
        int nextAvailableId = model.getScannerMarks().size() + 1;
        ScannerMark.setNextId(nextAvailableId);
        
        // Debug logging to track ID assignment
        System.out.println("Scanner Marks renumbered: Count=" + model.getScannerMarks().size() + ", NextID=" + nextAvailableId);
        
        // Update scanner mark count display (once after renumbering)
        view.setScannerMarkCount(model.getScannerMarks().size());
    }
    
}
