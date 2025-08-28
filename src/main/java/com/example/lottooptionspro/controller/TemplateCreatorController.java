package com.example.lottooptionspro.controller;

import com.example.lottooptionspro.models.BetslipTemplate;
import com.example.lottooptionspro.models.Coordinate;
import com.example.lottooptionspro.models.GridDefinition;
import com.example.lottooptionspro.models.GridMappingMode;
import com.example.lottooptionspro.models.FillOrder;
import com.example.lottooptionspro.util.GridCalculator;
import com.example.lottooptionspro.models.ScannerMark;
import com.example.lottooptionspro.presenter.TemplateCreatorPresenter;
import com.example.lottooptionspro.presenter.TemplateCreatorView;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.control.ButtonBar.ButtonData;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.layout.HBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import javafx.scene.shape.Rectangle;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import net.rgielen.fxweaver.core.FxmlView;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
@FxmlView("/com.example.lottooptionspro/controller/TemplateCreatorView.fxml")
public class TemplateCreatorController implements TemplateCreatorView {

    private TemplateCreatorPresenter presenter;

    @FXML
    private TextField gameNameField, jurisdictionField, globalOptionNameField;
    @FXML
    private Label globalOptionNameLabel, scannerMarkCountLabel;
    @FXML
    private ImageView betslipImageView;
    @FXML
    private Pane drawingPane;
    @FXML
    private ComboBox<String> mappingModeComboBox, panelComboBox;
    @FXML
    private Spinner<Integer> markWidthSpinner, markHeightSpinner;
    @FXML
    private VBox selectedMarkControls;
    @FXML
    private Label selectedMarkTypeLabel;
    @FXML
    private Spinner<Integer> selectedMarkWidthSpinner, selectedMarkHeightSpinner;
    
    // Grid mapping fields
    @FXML
    private VBox gridMappingSection, gridConfigSection;
    @FXML
    private CheckBox useGridModeCheckBox;
    @FXML
    private TextField gridRangeField;
    @FXML
    private Spinner<Integer> gridColumnsSpinner, gridRowsSpinner;
    @FXML
    private RadioButton columnBottomTopRadio, columnTopBottomRadio, rowLeftRightRadio, rowRightLeftRadio;
    @FXML
    private ToggleGroup fillOrderGroup;
    @FXML
    private Button defineGridButton, clearGridButton, autoMapButton, gridHelpButton;
    @FXML
    private Label gridStatusLabel;
    
    // Grid adjustment controls
    @FXML
    private VBox gridAdjustmentSection;
    @FXML
    private Button moveLeftButton, moveRightButton, moveUpButton, moveDownButton;
    @FXML
    private Button expandButton, shrinkButton, resetGridButton, refineGridButton;
    @FXML
    private Spinner<Integer> adjustmentStepSpinner;
    
    // Zoom controls
    @FXML
    private Button zoomInButton, zoomOutButton, resetZoomButton;
    @FXML
    private Label zoomLabel;
    
    
    // Column/Row fine-tuning controls
    @FXML
    private VBox columnRowTuningSection;
    @FXML
    private ToggleButton columnModeButton, rowModeButton;
    @FXML
    private Button clearSelectionButton;
    @FXML
    private Label selectionStatusLabel;

    private Rectangle selectedRectangle;
    private Node pressTarget;
    private boolean wasDragged = false;
    private double dragOffsetX, dragOffsetY;
    
    // Grid mapping state
    private GridMappingMode gridMode = GridMappingMode.NORMAL;
    private Circle firstCornerMarker, secondCornerMarker;
    private Rectangle gridBoundingBox;
    private final List<Line> gridLines = new ArrayList<>();
    private GridDefinition originalGridDefinition; // Store original for reset
    private boolean inTuningMode = false; // Track if we're in grid fine-tuning mode
    
    // Column/Row selection state
    private boolean columnRowSelectionMode = false;
    private boolean isColumnMode = true; // true = column mode, false = row mode
    private int selectedColumn = -1;
    private int selectedRow = -1;
    private final List<Rectangle> highlightedRectangles = new ArrayList<>();
    private final List<Rectangle> selectedRectangles = new ArrayList<>(); // Rectangles in selected column/row
    
    // Zoom state
    private double currentZoom = 1.0;
    private final double MIN_ZOOM = 0.25;
    private final double MAX_ZOOM = 4.0;
    private final double ZOOM_STEP = 0.25;

    @FXML
    public void initialize() {
        this.presenter = new TemplateCreatorPresenter(new BetslipTemplate(), this);
        drawingPane.setOnMousePressed(this::paneMousePressed);
        drawingPane.setOnMouseDragged(this::paneMouseDragged);
        drawingPane.setOnMouseReleased(this::paneMouseReleased);
        
        // Add keyboard event handling for column/row movement
        drawingPane.setFocusTraversable(true);
        drawingPane.setOnKeyPressed(this::handleKeyPress);
        
        populateComboBoxes();
        configureSpinners();
        addListeners();
        configureGridMapping();
        presenter.onPanelOrModeChanged();
        setSelectedMarkControlsVisible(false, null);
        // Make grid mapping visible by default for testing
        setGridMappingControlsVisible(true);
        // Initialize zoom controls
        initializeZoomControls();
    }

    private void populateComboBoxes() {
        mappingModeComboBox.setItems(FXCollections.observableArrayList("Main Number", "Bonus Number", "Quick Pick", "Global Option", "Scanner Mark"));
        mappingModeComboBox.getSelectionModel().selectFirst();
        
        // Smart panel population
        populatePanelComboBoxIntelligently();
    }
    
    /**
     * Intelligently populate panel dropdown by showing next panel that needs mappings
     */
    private void populatePanelComboBoxIntelligently() {
        populatePanelComboBoxIntelligently(false);
    }
    
    /**
     * Populate panel dropdown with option to prefer completed panels (for template loading)
     */
    private void populatePanelComboBoxIntelligently(boolean preferCompletedPanel) {
        List<String> allPanels = Arrays.asList("A", "B", "C", "D", "E");
        List<String> availablePanels = new ArrayList<>();
        String nextPanelToSelect = null;
        String firstCompletedPanel = null;
        
        // Check which panels have coordinate mappings
        for (String panelId : allPanels) {
            Map<String, Coordinate> mainNumbers = presenter.getMainNumbersForPanel(panelId);
            boolean hasCoordinates = mainNumbers != null && !mainNumbers.isEmpty();
            
            availablePanels.add(panelId + (hasCoordinates ? " ✓" : ""));
            
            // Find first completed panel for template loading preference
            if (firstCompletedPanel == null && hasCoordinates) {
                firstCompletedPanel = panelId + " ✓";
            }
            
            // Find first panel without coordinates
            if (nextPanelToSelect == null && !hasCoordinates) {
                nextPanelToSelect = panelId + (hasCoordinates ? " ✓" : "");
            }
        }
        
        // Set panel items with status indicators
        panelComboBox.setItems(FXCollections.observableArrayList(availablePanels));
        
        // Determine which panel to select
        String panelToSelect;
        if (preferCompletedPanel && firstCompletedPanel != null) {
            // When loading templates, prefer showing a completed panel first
            panelToSelect = firstCompletedPanel;
        } else if (nextPanelToSelect == null) {
            // All panels complete - show message and default to first panel
            showAllPanelsCompleteDialog();
            panelComboBox.getSelectionModel().selectFirst();
            return;
        } else {
            // Select the first panel that needs mappings
            panelToSelect = nextPanelToSelect;
        }
        
        panelComboBox.getSelectionModel().select(panelToSelect);
    }
    
    /**
     * Show dialog when all panels have coordinate mappings
     */
    private void showAllPanelsCompleteDialog() {
        // Create a more detailed completion status
        List<String> allPanels = Arrays.asList("A", "B", "C", "D", "E");
        int completedPanels = 0;
        StringBuilder statusMessage = new StringBuilder("Coordinate Mapping Status:\n\n");
        
        for (String panelId : allPanels) {
            Map<String, Coordinate> mainNumbers = presenter.getMainNumbersForPanel(panelId);
            boolean hasCoordinates = mainNumbers != null && !mainNumbers.isEmpty();
            int coordinateCount = hasCoordinates ? mainNumbers.size() : 0;
            
            statusMessage.append(String.format("Panel %s: %s (%d coordinates)\n", 
                panelId, 
                hasCoordinates ? "✓ Complete" : "⚠ Needs mapping", 
                coordinateCount));
            
            if (hasCoordinates) {
                completedPanels++;
            }
        }
        
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Template Status");
        alert.setHeaderText(String.format("%d of %d panels complete", completedPanels, allPanels.size()));
        alert.setContentText(
            statusMessage.toString() + "\n" +
            "Options:\n" +
            "✓ Select any panel to modify existing mappings\n" +
            "✓ Use 'Clear Grid → Clear All' to start over\n" +
            "✓ Individual coordinates can be moved by dragging them\n" +
            "✓ Use column/row selection for bulk adjustments"
        );
        alert.showAndWait();
    }
    
    /**
     * Refresh the panel dropdown to update status indicators
     * Call this after loading templates or making coordinate changes
     */
    public void refreshPanelDropdown() {
        refreshPanelDropdown(false);
    }
    
    /**
     * Refresh the panel dropdown with option to prefer completed panels
     */
    public void refreshPanelDropdown(boolean preferCompletedPanel) {
        String currentSelection = getSelectedPanel();
        populatePanelComboBoxIntelligently(preferCompletedPanel);
        
        // Try to maintain current selection if it's still valid (unless preferring completed panel)
        if (currentSelection != null && !preferCompletedPanel) {
            for (int i = 0; i < panelComboBox.getItems().size(); i++) {
                String item = panelComboBox.getItems().get(i);
                if (item.startsWith(currentSelection)) {
                    panelComboBox.getSelectionModel().select(i);
                    break;
                }
            }
        }
    }
    
    /**
     * Update grid status with current panel completion info
     */
    private void updateGridStatusWithPanelInfo() {
        String currentPanelId = getSelectedPanel();
        if (currentPanelId == null) return;
        
        Map<String, Coordinate> currentPanelNumbers = getMainNumbersForPanel(currentPanelId);
        int coordinateCount = currentPanelNumbers != null ? currentPanelNumbers.size() : 0;
        
        // Count total completed panels
        List<String> allPanels = Arrays.asList("A", "B", "C", "D", "E");
        int completedPanels = 0;
        for (String panelId : allPanels) {
            Map<String, Coordinate> numbers = presenter.getMainNumbersForPanel(panelId);
            if (numbers != null && !numbers.isEmpty()) {
                completedPanels++;
            }
        }
        
        String statusMessage = String.format(
            "Panel %s complete! (%d coordinates) | %d of %d panels done | Ready for column/row fine-tuning",
            currentPanelId, coordinateCount, completedPanels, allPanels.size()
        );
        
        updateGridMappingStatus(statusMessage);
    }

    private void configureSpinners() {
        markWidthSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 100, 20));
        markHeightSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 100, 20));
        selectedMarkWidthSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 100, 20));
        selectedMarkHeightSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 100, 20));
        
        // Grid mapping spinners - ensure they are initialized
        if (gridColumnsSpinner != null) {
            gridColumnsSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 20, 7));
            gridColumnsSpinner.setEditable(true);
        }
        if (gridRowsSpinner != null) {
            gridRowsSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 20, 5));
            gridRowsSpinner.setEditable(true);
        }
        
        // Adjustment step spinner
        if (adjustmentStepSpinner != null) {
            adjustmentStepSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 20, 2));
            adjustmentStepSpinner.setEditable(true);
        }
        
        
        // Add listeners to grid configuration fields to provide real-time validation feedback
        if (gridRangeField != null) {
            gridRangeField.textProperty().addListener((obs, oldVal, newVal) -> updateGridValidationStatus());
        }
        if (gridColumnsSpinner != null) {
            gridColumnsSpinner.valueProperty().addListener((obs, oldVal, newVal) -> updateGridValidationStatus());
        }
        if (gridRowsSpinner != null) {
            gridRowsSpinner.valueProperty().addListener((obs, oldVal, newVal) -> updateGridValidationStatus());
        }
        
    }

    private void configureGridMapping() {
        if (gridRangeField != null) {
            gridRangeField.setText("1-35"); // Default for Cash Five
        }
        
        if (useGridModeCheckBox != null) {
            useGridModeCheckBox.selectedProperty().addListener((obs, old, selected) -> {
                if (gridConfigSection != null) {
                    gridConfigSection.setVisible(selected);
                    gridConfigSection.setManaged(selected);
                }
                if (!selected) {
                    clearGrid();
                }
            });
        }
    }
    
    private void initializeZoomControls() {
        if (zoomLabel != null) {
            zoomLabel.setText("100%");
        }
        // Initialize button states
        applyZoom();
    }
    
    private void addListeners() {
        mappingModeComboBox.getSelectionModel().selectedItemProperty().addListener((obs, o, n) -> {
            updateVisibleControls(n);
            presenter.onPanelOrModeChanged();
            unselectRectangle();
            // Show grid mapping for main/bonus numbers only
            boolean showGrid = "Main Number".equals(n) || "Bonus Number".equals(n);
            setGridMappingControlsVisible(showGrid);
        });
        panelComboBox.getSelectionModel().selectedItemProperty().addListener((obs, o, n) -> {
            presenter.onPanelOrModeChanged();
            checkAndEnableColumnRowTuningForPanel();
        });
        markWidthSpinner.valueProperty().addListener((obs, o, n) -> {
            String currentMode = getSelectedMappingMode();
            if ("Scanner Mark".equals(currentMode)) {
                // For scanner marks in mapping mode, this sets the size for NEW scanner marks
                // Individual scanner marks are modified via the Selected Mark Controls
                presenter.setNewScannerMarkSize(n, markHeightSpinner.getValue());
            } else if ("Global Option".equals(currentMode)) {
                // For Global Option, update only global option markings without affecting others
                presenter.updateMarkSizeForMode(n, markHeightSpinner.getValue(), currentMode);
            } else if ("Quick Pick".equals(currentMode)) {
                // For Quick Pick, update only quick pick markings without affecting others
                presenter.updateMarkSizeForMode(n, markHeightSpinner.getValue(), currentMode);
            } else {
                // For Main Number and Bonus Number modes, update only those types
                presenter.updateMarkSizeForCurrentPanel(n, markHeightSpinner.getValue(), currentMode);
            }
        });
        markHeightSpinner.valueProperty().addListener((obs, o, n) -> {
            String currentMode = getSelectedMappingMode();
            if ("Scanner Mark".equals(currentMode)) {
                // For scanner marks in mapping mode, this sets the size for NEW scanner marks
                // Individual scanner marks are modified via the Selected Mark Controls
                presenter.setNewScannerMarkSize(markWidthSpinner.getValue(), n);
            } else if ("Global Option".equals(currentMode)) {
                // For Global Option, update only global option markings without affecting others
                presenter.updateMarkSizeForMode(markWidthSpinner.getValue(), n, currentMode);
            } else if ("Quick Pick".equals(currentMode)) {
                // For Quick Pick, update only quick pick markings without affecting others
                presenter.updateMarkSizeForMode(markWidthSpinner.getValue(), n, currentMode);
            } else {
                // For Main Number and Bonus Number modes, update only those types
                presenter.updateMarkSizeForCurrentPanel(markWidthSpinner.getValue(), n, currentMode);
            }
        });

        selectedMarkWidthSpinner.valueProperty().addListener((obs, o, n) -> {
            if (selectedRectangle != null) {
                Object userData = selectedRectangle.getUserData();
                if (userData instanceof ScannerMark) {
                    presenter.updateScannerMarkSize((ScannerMark) userData, n, selectedMarkHeightSpinner.getValue());
                } else if (userData instanceof CoordinateInfo) {
                    CoordinateInfo coordInfo = (CoordinateInfo) userData;
                    presenter.updateMarkingSize(coordInfo, n, selectedMarkHeightSpinner.getValue());
                }
            }
        });
        selectedMarkHeightSpinner.valueProperty().addListener((obs, o, n) -> {
            if (selectedRectangle != null) {
                Object userData = selectedRectangle.getUserData();
                if (userData instanceof ScannerMark) {
                    presenter.updateScannerMarkSize((ScannerMark) userData, selectedMarkWidthSpinner.getValue(), n);
                } else if (userData instanceof CoordinateInfo) {
                    CoordinateInfo coordInfo = (CoordinateInfo) userData;
                    presenter.updateMarkingSize(coordInfo, selectedMarkWidthSpinner.getValue(), n);
                }
            }
        });
    }

    private void updateVisibleControls(String mode) {
        boolean isGlobal = "Global Option".equals(mode);
        boolean isNumber = "Main Number".equals(mode) || "Bonus Number".equals(mode);
        boolean isScanner = "Scanner Mark".equals(mode);

        globalOptionNameField.setVisible(isGlobal);
        globalOptionNameLabel.setVisible(isGlobal);

        if (!isScanner) {
            setSelectedMarkControlsVisible(false, null);
        }
    }

    private void paneMousePressed(MouseEvent event) {
        // Handle grid definition mode
        if (gridMode == GridMappingMode.DEFINING_FIRST_CORNER || 
            gridMode == GridMappingMode.DEFINING_SECOND_CORNER) {
            handleGridCornerClick(event.getX(), event.getY());
            event.consume();
            return;
        }
        
        pressTarget = (Node) event.getTarget();
        wasDragged = false;
        if (pressTarget instanceof Rectangle) {
            Rectangle rect = (Rectangle) pressTarget;
            handleRectangleSelection(rect);
            dragOffsetX = event.getX() - rect.getX();
            dragOffsetY = event.getY() - rect.getY();
            rect.setCursor(Cursor.MOVE);
            if (rect.getUserData() instanceof CoordinateInfo) {
                CoordinateInfo coordInfo = (CoordinateInfo) rect.getUserData();
                presenter.startCoordinateMove(coordInfo.getCoordinate());
            } else if (rect.getUserData() instanceof ScannerMark) {
                presenter.startScannerMarkMove((ScannerMark) rect.getUserData());
            }
        } else {
            unselectRectangle();
        }
        event.consume();
    }

    private void paneMouseDragged(MouseEvent event) {
        if (pressTarget instanceof Rectangle) {
            wasDragged = true;
            Rectangle rect = (Rectangle) pressTarget;
            rect.setX(event.getX() - dragOffsetX);
            rect.setY(event.getY() - dragOffsetY);
        }
        event.consume();
    }

    private void paneMouseReleased(MouseEvent event) {
        // Don't handle normal clicks during grid definition
        if (gridMode == GridMappingMode.DEFINING_FIRST_CORNER || 
            gridMode == GridMappingMode.DEFINING_SECOND_CORNER) {
            event.consume();
            return;
        }
        
        if (pressTarget instanceof Rectangle) {
            Rectangle rect = (Rectangle) pressTarget;
            if (wasDragged) {
                Object userData = rect.getUserData();
                double newX = rect.getX();
                double newY = rect.getY();
                if (userData instanceof CoordinateInfo) {
                    CoordinateInfo coordInfo = (CoordinateInfo) userData;
                    presenter.finishCoordinateMove(coordInfo.getCoordinate(), (int) (newX + rect.getWidth() / 2), (int) (newY + rect.getHeight() / 2));
                } else if (userData instanceof ScannerMark) {
                    presenter.finishScannerMarkMove((ScannerMark) userData, newX, newY);
                }
            } else {
                // Handle column/row selection when not dragging
                if (columnRowSelectionMode && rect.getUserData() instanceof CoordinateInfo) {
                    handleNumberSelection(rect);
                }
            }
            rect.setCursor(Cursor.DEFAULT);
        } else if (pressTarget == drawingPane && !wasDragged && !isGridModeEnabled()) {
            // Only allow normal clicking when grid mode is disabled
            presenter.onPaneClicked(event.getX(), event.getY(), markWidthSpinner.getValue(), markHeightSpinner.getValue());
        }
        pressTarget = null;
        wasDragged = false;
        event.consume();
    }

    private void handleRectangleSelection(Rectangle rect) {
        unselectRectangle();
        Object userData = rect.getUserData();
        
        if (userData instanceof ScannerMark) {
            selectedRectangle = rect;
            selectedRectangle.setStroke(Color.RED);
            ScannerMark mark = (ScannerMark) userData;
            setSelectedMarkDimensions(mark.getWidth(), mark.getHeight());
            setSelectedMarkControlsVisible(true, "Scanner Mark #" + mark.getId());
        } else if (userData instanceof CoordinateInfo) {
            CoordinateInfo coordInfo = (CoordinateInfo) userData;
            if ("QUICK_PICK".equals(coordInfo.getType()) || "GLOBAL_OPTION".equals(coordInfo.getType())) {
                selectedRectangle = rect;
                selectedRectangle.setStroke(Color.RED);
                
                // Get current dimensions from presenter
                int[] dimensions = presenter.getMarkingDimensions(coordInfo);
                setSelectedMarkDimensions(dimensions[0], dimensions[1]);
                setSelectedMarkControlsVisible(true, coordInfo.getType());
            }
        }
    }

    private void unselectRectangle() {
        if (selectedRectangle != null) {
            Object userData = selectedRectangle.getUserData();
            if (userData instanceof ScannerMark) {
                selectedRectangle.setStroke(Color.BLUE);
            } else if (userData instanceof CoordinateInfo) {
                CoordinateInfo coordInfo = (CoordinateInfo) userData;
                switch (coordInfo.getType()) {
                    case "MAIN_NUMBER":
                        selectedRectangle.setStroke(Color.BLACK);
                        break;
                    case "BONUS_NUMBER":
                        selectedRectangle.setStroke(Color.GREEN);
                        break;
                    case "QUICK_PICK":
                        selectedRectangle.setStroke(Color.ORANGE);
                        break;
                    case "GLOBAL_OPTION":
                        selectedRectangle.setStroke(Color.PURPLE);
                        break;
                    default:
                        selectedRectangle.setStroke(Color.BLACK);
                        break;
                }
            } else {
                selectedRectangle.setStroke(Color.BLACK);
            }
        }
        selectedRectangle = null;
        setSelectedMarkControlsVisible(false, null);
    }

    @Override
    public void drawRectangle(Coordinate coordinate, int width, int height) {
        drawRectangleWithPanelInfo(coordinate, width, height, "MAIN_NUMBER", null);
    }
    
    @Override
    public void drawRectangle(Coordinate coordinate, int width, int height, String type) {
        drawRectangleWithPanelInfo(coordinate, width, height, type, null);
    }
    
    @Override
    public void drawRectangle(Coordinate coordinate, int width, int height, String type, String panelId) {
        drawRectangleWithPanelInfo(coordinate, width, height, type, panelId);
    }
    
    /**
     * Enhanced drawRectangle with panel information for better management
     */
    private void drawRectangleWithPanelInfo(Coordinate coordinate, int width, int height, String type, String panelId) {
        double x = coordinate.getX() - (double) width / 2;
        double y = coordinate.getY() - (double) height / 2;
        Rectangle rect = new Rectangle(x, y, width, height);
        
        // Create wrapper to store coordinate + metadata
        CoordinateInfo coordInfo = new CoordinateInfo(coordinate, type, panelId);
        rect.setUserData(coordInfo);
        rect.setFill(Color.TRANSPARENT);
        
        // Set color based on type
        Color strokeColor;
        switch (type) {
            case "MAIN_NUMBER":
                strokeColor = Color.BLACK;
                break;
            case "BONUS_NUMBER":
                strokeColor = Color.GREEN;
                break;
            case "QUICK_PICK":
                strokeColor = Color.ORANGE;
                break;
            case "GLOBAL_OPTION":
                strokeColor = Color.PURPLE;
                break;
            default:
                strokeColor = Color.BLACK;
                break;
        }
        
        rect.setStroke(strokeColor);
        rect.setStrokeWidth(1);
        
        drawingPane.getChildren().add(rect);
    }
    
    /**
     * Wrapper class to store coordinate information with metadata
     */
    public static class CoordinateInfo {
        private final Coordinate coordinate;
        private final String type;
        private final String panelId;
        
        public CoordinateInfo(Coordinate coordinate, String type, String panelId) {
            this.coordinate = coordinate;
            this.type = type;
            this.panelId = panelId;
        }
        
        public Coordinate getCoordinate() { return coordinate; }
        public String getType() { return type; }
        public String getPanelId() { return panelId; }
    }


    @Override
    public void drawScannerMark(ScannerMark mark) {
        Rectangle rect = new Rectangle(mark.getX(), mark.getY(), mark.getWidth(), mark.getHeight());
        rect.setUserData(mark);
        rect.setFill(Color.TRANSPARENT);
        rect.setStroke(Color.BLUE);
        rect.setStrokeWidth(2);
        drawingPane.getChildren().add(rect);
    }

    @Override
    public void updateScannerMarkRectangle(ScannerMark mark, double width, double height) {
        for (Node node : drawingPane.getChildren()) {
            if (node instanceof Rectangle && mark.equals(node.getUserData())) {
                ((Rectangle) node).setWidth(width);
                ((Rectangle) node).setHeight(height);
                break;
            }
        }
    }

    @Override
    public void setSelectedMarkControlsVisible(boolean visible, String markType) {
        selectedMarkControls.setVisible(visible);
        if (visible && markType != null) {
            if (markType.startsWith("Scanner Mark #")) {
                selectedMarkTypeLabel.setText(markType + " Selected");
            } else {
                switch (markType) {
                    case "QUICK_PICK":
                        selectedMarkTypeLabel.setText("Quick Pick Selected");
                        break;
                    case "GLOBAL_OPTION":
                        selectedMarkTypeLabel.setText("Global Option Selected");
                        break;
                    default:
                        selectedMarkTypeLabel.setText(markType + " Selected");
                        break;
                }
            }
        }
    }

    @Override
    public void setSelectedMarkDimensions(double width, double height) {
        selectedMarkWidthSpinner.getValueFactory().setValue((int) width);
        selectedMarkHeightSpinner.getValueFactory().setValue((int) height);
    }

    @Override
    public void setScannerMarkCount(int count) {
        scannerMarkCountLabel.setText("Scanner Marks: " + count);
    }
    
    @Override
    public void selectScannerMark(ScannerMark mark) {
        // Find the rectangle that corresponds to this scanner mark and select it
        for (Node node : drawingPane.getChildren()) {
            if (node instanceof Rectangle) {
                Rectangle rect = (Rectangle) node;
                if (mark.equals(rect.getUserData())) {
                    // Select this scanner mark rectangle
                    handleRectangleSelection(rect);
                    break;
                }
            }
        }
    }

    @FXML
    private void saveTemplate() {
        presenter.saveTemplate();
    }

    @FXML
    private void saveTemplateAs() {
        presenter.saveTemplateAs();
    }

    @FXML
    private void loadTemplate() {
        presenter.loadTemplate();
        // Refresh panel dropdown after loading, preferring completed panels
        refreshPanelDropdown(true);
        // Check if loaded panel has coordinates and enable column/row tuning
        checkAndEnableColumnRowTuningForPanel();
    }

    @FXML
    private void previewTemplate() {
        presenter.previewTemplate();
    }

    @Override
    public File showSaveDialog(String initialFileName) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Save Template As");
        fileChooser.setInitialFileName(initialFileName);
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("JSON Files", "*.json"));
        return fileChooser.showSaveDialog(getStage());
    }

    @Override
    public Optional<String> askForPreviewNumbers() {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Live Preview");
        dialog.setHeaderText("Enter numbers to preview their plot locations.");
        dialog.setContentText("Numbers (e.g., 5 10 15, 8):");
        return dialog.showAndWait();
    }

    @Override
    public void drawPreviewRectangle(Coordinate coordinate, int width, int height) {
        double x = coordinate.getX() - (double) width / 2;
        double y = coordinate.getY() - (double) height / 2;
        Rectangle rect = new Rectangle(x, y, width, height);
        rect.getStyleClass().add("preview-mark");
        rect.setFill(Color.BLACK);
        drawingPane.getChildren().add(rect);
    }

    @Override
    public void clearPreviewRectangles() {
        drawingPane.getChildren().removeIf(node -> node.getStyleClass().contains("preview-mark"));
    }

    @FXML
    private void clearLastMarking() { 
        // Use undo stack to remove last marking of any type
        presenter.removeLastMarking();
        // Refresh panel dropdown quietly without triggering completion dialogs
        refreshPanelDropdownQuietly();
    }
    
    /**
     * Refresh panel dropdown without showing completion dialogs
     */
    private void refreshPanelDropdownQuietly() {
        String currentSelection = getSelectedPanel();
        populatePanelComboBoxQuietly(false);
        
        // Try to maintain current selection
        if (currentSelection != null) {
            for (int i = 0; i < panelComboBox.getItems().size(); i++) {
                String item = panelComboBox.getItems().get(i);
                if (item.startsWith(currentSelection)) {
                    panelComboBox.getSelectionModel().select(i);
                    break;
                }
            }
        }
    }
    
    /**
     * Populate panel dropdown without showing completion dialogs (for quiet operations)
     */
    private void populatePanelComboBoxQuietly(boolean preferCompletedPanel) {
        List<String> allPanels = Arrays.asList("A", "B", "C", "D", "E");
        List<String> availablePanels = new ArrayList<>();
        String nextPanelToSelect = null;
        String firstCompletedPanel = null;
        
        // Check which panels have coordinate mappings
        for (String panelId : allPanels) {
            Map<String, Coordinate> mainNumbers = presenter.getMainNumbersForPanel(panelId);
            boolean hasCoordinates = mainNumbers != null && !mainNumbers.isEmpty();
            
            availablePanels.add(panelId + (hasCoordinates ? " ✓" : ""));
            
            // Find first completed panel for template loading preference
            if (firstCompletedPanel == null && hasCoordinates) {
                firstCompletedPanel = panelId + " ✓";
            }
            
            // Find first panel without coordinates
            if (nextPanelToSelect == null && !hasCoordinates) {
                nextPanelToSelect = panelId + (hasCoordinates ? " ✓" : "");
            }
        }
        
        // Set panel items with status indicators
        panelComboBox.setItems(FXCollections.observableArrayList(availablePanels));
        
        // Determine which panel to select (NO DIALOG SHOWN)
        String panelToSelect;
        if (preferCompletedPanel && firstCompletedPanel != null) {
            panelToSelect = firstCompletedPanel;
        } else if (nextPanelToSelect == null) {
            // All panels complete - just select first panel WITHOUT showing dialog
            panelComboBox.getSelectionModel().selectFirst();
            return;
        } else {
            panelToSelect = nextPanelToSelect;
        }
        
        panelComboBox.getSelectionModel().select(panelToSelect);
    }
    
    /**
     * Update the size of the currently selected scanner mark in real time
     * ONLY updates the selected scanner mark - no bulk updates
     */
    private void updateSelectedScannerMarkSize(int width, int height) {
        if (selectedRectangle != null && selectedRectangle.getUserData() instanceof ScannerMark) {
            // Update ONLY the selected scanner mark using the presenter
            presenter.updateScannerMarkSize((ScannerMark) selectedRectangle.getUserData(), width, height);
        }
        // REMOVED: No longer update all scanner marks - each mark has its own individual size
        // Each scanner mark must be selected individually to be modified
    }

    @FXML
    private void loadImage() { presenter.loadImage(); }

    @Override
    public void showView() { /* Handled by ScreenManager */ }

    @Override
    public File showOpenImageDialog() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Open Betslip Image");
        fileChooser.getExtensionFilters().addAll(new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg"));
        return fileChooser.showOpenDialog(getStage());
    }

    @Override
    public File showOpenTemplateDialog() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Open Template File");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("JSON Files", "*.json"));
        return fileChooser.showOpenDialog(getStage());
    }

    @Override
    public void displayImage(String imagePath) {
        betslipImageView.setImage(new javafx.scene.image.Image(imagePath));
    }

    @Override
    public void clearAllRectangles() {
        drawingPane.getChildren().removeIf(node -> node instanceof Rectangle);
    }
    
    /**
     * Clear rectangles that belong to a specific panel
     */
    @Override
    public void clearPanelRectangles(String panelId) {
        drawingPane.getChildren().removeIf(node -> {
            if (node instanceof Rectangle) {
                Object userData = ((Rectangle) node).getUserData();
                if (userData instanceof CoordinateInfo) {
                    CoordinateInfo coordInfo = (CoordinateInfo) userData;
                    return panelId.equals(coordInfo.getPanelId());
                }
            }
            return false;
        });
    }

    @Override
    public String getGameName() { return gameNameField.getText(); }

    @Override
    public String getJurisdiction() { return jurisdictionField.getText(); }

    @Override
    public void setGameName(String name) { gameNameField.setText(name); }

    @Override
    public void setJurisdiction(String name) { jurisdictionField.setText(name); }


    @Override
    public String getSelectedMappingMode() { return mappingModeComboBox.getValue(); }

    @Override
    public String getSelectedPanel() { 
        String selectedValue = panelComboBox.getValue();
        if (selectedValue == null) return null;
        
        // Extract just the panel ID (remove status indicator if present)
        if (selectedValue.contains(" ✓")) {
            return selectedValue.substring(0, selectedValue.indexOf(" ✓"));
        }
        return selectedValue;
    }

    @Override
    public String getGlobalOptionName() { return globalOptionNameField.getText(); }

    @Override
    public void showError(String message) { new Alert(Alert.AlertType.ERROR, message).showAndWait(); }

    @Override
    public void showSuccess(String message) { new Alert(Alert.AlertType.INFORMATION, message).showAndWait(); }

    private Stage getStage() { return (Stage) gameNameField.getScene().getWindow(); }
    
    // Grid mapping methods
    @FXML
    private void startGridDefinition() {
        if (!isGridModeEnabled()) {
            showError("Please enable grid mode first");
            return;
        }
        
        // Validate grid configuration before starting
        String validationError = validateGridConfiguration();
        if (validationError != null) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Invalid Grid Configuration");
            alert.setHeaderText("Grid configuration error:");
            alert.setContentText(validationError);
            alert.showAndWait();
            return;
        }
        
        gridMode = GridMappingMode.DEFINING_FIRST_CORNER;
        clearGridVisuals();
        updateGridMappingStatus("Click the first corner of the number grid");
        defineGridButton.setDisable(true);
        clearGridButton.setVisible(true);
    }
    
    private String validateGridConfiguration() {
        // Get current grid settings
        String range = getGridNumberRange();
        int columns = getGridColumns();
        int rows = getGridRows();
        
        // Parse and validate number range
        int[] rangeValues;
        try {
            if (range.contains("-")) {
                String[] parts = range.split("-");
                if (parts.length != 2) {
                    return "Number range must be in format 'start-end' (e.g., '1-35')";
                }
                int start = Integer.parseInt(parts[0].trim());
                int end = Integer.parseInt(parts[1].trim());
                if (start <= 0 || end <= 0) {
                    return "Numbers must be positive (greater than 0)";
                }
                if (start >= end) {
                    return "Start number (" + start + ") must be less than end number (" + end + ")";
                }
                rangeValues = new int[]{start, end};
            } else {
                return "Number range must be in format 'start-end' (e.g., '1-35')";
            }
        } catch (NumberFormatException e) {
            return "Number range contains invalid numbers. Use format 'start-end' (e.g., '1-35')";
        }
        
        // Validate grid dimensions
        if (columns <= 0 || rows <= 0) {
            return "Grid dimensions must be positive numbers. Columns: " + columns + ", Rows: " + rows;
        }
        
        if (columns > 20 || rows > 20) {
            return "Grid dimensions too large. Maximum 20 columns and 20 rows for practical use.";
        }
        
        // Validate that grid can accommodate the numbers
        int totalNumbers = rangeValues[1] - rangeValues[0] + 1;
        int gridCells = columns * rows;
        
        if (gridCells < totalNumbers) {
            return "Grid too small: " + columns + "×" + rows + " = " + gridCells + " cells, but you need " + totalNumbers + " numbers (" + range + ")";
        }
        
        if (gridCells > totalNumbers * 2) {
            return "Grid too large: " + columns + "×" + rows + " = " + gridCells + " cells for only " + totalNumbers + " numbers (" + range + "). Consider reducing grid size.";
        }
        
        // All validations passed
        return null;
    }
    
    private void updateGridValidationStatus() {
        if (!isGridModeEnabled()) {
            return; // Don't validate when grid mode is off
        }
        
        String validationError = validateGridConfiguration();
        if (validationError == null) {
            // Configuration is valid - show green status
            updateGridMappingStatus("✓ Grid configuration valid - ready to define grid");
            if (defineGridButton != null) {
                defineGridButton.setDisable(false);
            }
        } else {
            // Configuration has errors - show red status with brief error
            String briefError = validationError.length() > 50 ? 
                validationError.substring(0, 47) + "..." : validationError;
            updateGridMappingStatus("⚠ " + briefError);
            if (defineGridButton != null) {
                defineGridButton.setDisable(true);
            }
        }
    }
    
    @FXML
    private void clearGrid() {
        String currentPanelId = getSelectedPanel();
        
        // Ask user if they want to clear current panel, all panels, or just grid
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Clear Grid");
        alert.setHeaderText("Choose what to clear:");
        
        String panelInfo = currentPanelId != null ? 
            String.format(" (Panel %s has %d coordinates)", 
                currentPanelId, 
                getMainNumbersForPanel(currentPanelId).size()) : "";
        
        alert.setContentText(
            "What would you like to clear?\n\n" +
            "• Current Panel: Clear only Panel " + (currentPanelId != null ? currentPanelId : "?") + panelInfo + "\n" +
            "• All Panels: Clear all markings from all panels (A-E)\n" +
            "• Grid Only: Clear just the grid overlay"
        );
        
        ButtonType clearCurrentPanelButton = new ButtonType("Current Panel Only");
        ButtonType clearAllPanelsButton = new ButtonType("All Panels");
        ButtonType clearGridOnlyButton = new ButtonType("Grid Only");
        ButtonType cancelButton = new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE);
        
        alert.getButtonTypes().setAll(clearCurrentPanelButton, clearAllPanelsButton, clearGridOnlyButton, cancelButton);
        
        Optional<ButtonType> result = alert.showAndWait();
        
        if (result.isPresent()) {
            if (result.get() == clearCurrentPanelButton) {
                clearCurrentPanelMarkings();
            } else if (result.get() == clearAllPanelsButton) {
                clearAllDrawingsAndMarkings();
            } else if (result.get() == clearGridOnlyButton) {
                clearGridOnly();
            }
            // If cancel, do nothing
        }
    }
    
    private void clearGridOnly() {
        gridMode = GridMappingMode.NORMAL;
        inTuningMode = false; // Exit tuning mode
        clearGridVisuals();
        defineGridButton.setDisable(false);
        clearGridButton.setVisible(false);
        autoMapButton.setVisible(false);
        
        // Hide adjustment controls
        if (gridAdjustmentSection != null) {
            gridAdjustmentSection.setVisible(false);
            gridAdjustmentSection.setManaged(false);
        }
        
        // Hide column/row tuning controls
        if (columnRowTuningSection != null) {
            columnRowTuningSection.setVisible(false);
            columnRowTuningSection.setManaged(false);
            columnRowSelectionMode = false;
            clearColumnRowSelection();
        }
        
        originalGridDefinition = null;
        updateGridMappingStatus("Ready");
    }
    
    /**
     * Clear markings for the current panel only
     */
    private void clearCurrentPanelMarkings() {
        String currentPanelId = getSelectedPanel();
        if (currentPanelId == null) {
            showError("No panel selected");
            return;
        }
        
        // Clear grid overlay
        clearGridOnly();
        
        // Get current panel's coordinates to remove only those rectangles
        Map<String, Coordinate> currentPanelNumbers = getMainNumbersForPanel(currentPanelId);
        
        // Remove only rectangles belonging to current panel
        List<Node> rectanglesToRemove = new ArrayList<>();
        for (Node node : drawingPane.getChildren()) {
            if (node instanceof Rectangle && node.getUserData() instanceof CoordinateInfo) {
                if (isRectangleInCurrentPanel((Rectangle) node)) {
                    rectanglesToRemove.add(node);
                }
            }
        }
        drawingPane.getChildren().removeAll(rectanglesToRemove);
        
        // Clear preview rectangles (these are not panel-specific anyway)
        clearPreviewRectangles();
        
        // Clear model data for current panel only via presenter
        if (presenter != null) {
            presenter.clearPanelMarkingsData(currentPanelId);
        }
        
        // Refresh panel dropdown to update status indicators
        refreshPanelDropdown();
        
        updateGridMappingStatus(String.format("Panel %s markings cleared", currentPanelId));
    }
    
    private void clearAllDrawingsAndMarkings() {
        // Clear grid first
        clearGridOnly();
        
        // Clear all visual elements
        clearAllRectangles();
        clearPreviewRectangles();
        
        // Clear model data via presenter
        if (presenter != null) {
            presenter.clearAllMarkingsData();
        }
        
        // Refresh panel dropdown to update status indicators
        refreshPanelDropdown();
        
        updateGridMappingStatus("All drawings and markings cleared");
    }
    
    @FXML
    private void autoMapNumbers() {
        presenter.autoMapGridNumbers();
        
        // Enable column/row fine-tuning after auto-mapping
        enableColumnRowTuning();
        
        // Update status with completion info
        updateGridStatusWithPanelInfo();
        
        // Refresh panel dropdown to update status indicators
        refreshPanelDropdown();
    }
    
    private void enableColumnRowTuning() {
        if (columnRowTuningSection != null) {
            columnRowTuningSection.setVisible(true);
            columnRowTuningSection.setManaged(true);
            columnRowSelectionMode = true;
            
            // Ensure drawing pane can receive focus and keyboard events
            drawingPane.setFocusTraversable(true);
            drawingPane.requestFocus();
            
            // Add additional event filtering to ensure keyboard events reach the handler
            drawingPane.addEventFilter(javafx.scene.input.KeyEvent.KEY_PRESSED, this::handleKeyPress);
            
            updateGridMappingStatus("Numbers auto-mapped! Click a number to select column/row, then use arrow keys to move (Shift+Arrow for larger steps).");
        }
    }
    
    /**
     * Check if current panel has coordinates and enable column/row tuning if needed
     */
    private void checkAndEnableColumnRowTuningForPanel() {
        String currentPanelId = getSelectedPanel();
        if (currentPanelId == null) return;
        
        // Check if current panel has coordinates mapped
        Map<String, Coordinate> mainNumbers = getMainNumbersForPanel(currentPanelId);
        if (mainNumbers != null && !mainNumbers.isEmpty()) {
            // Panel has coordinates - enable column/row tuning
            if (columnRowTuningSection != null) {
                columnRowTuningSection.setVisible(true);
                columnRowTuningSection.setManaged(true);
                columnRowSelectionMode = true;
                
                // Ensure drawing pane can receive focus and keyboard events
                drawingPane.setFocusTraversable(true);
                drawingPane.requestFocus();
                
                // Add additional event filtering to ensure keyboard events reach the handler
                drawingPane.addEventFilter(javafx.scene.input.KeyEvent.KEY_PRESSED, this::handleKeyPress);
                
                // Check if presenter has a valid grid for column/row operations
                if (presenter.getCurrentGrid() != null && presenter.getCurrentGrid().isValid()) {
                    updateGridMappingStatus("Panel " + currentPanelId + " loaded. Click a number to select column/row, then use arrow keys to move (Shift+Arrow for larger steps).");
                } else {
                    updateGridMappingStatus("Panel " + currentPanelId + " loaded but grid not available. Column/row editing may not work properly.");
                    // Try to trigger grid reconstruction through panel change
                    presenter.onPanelOrModeChanged();
                }
            }
        } else {
            // Panel has no coordinates - disable column/row tuning but keep grid mapping available
            if (columnRowTuningSection != null) {
                columnRowTuningSection.setVisible(false);
                columnRowTuningSection.setManaged(false);
                columnRowSelectionMode = false;
                clearColumnRowSelection();
            }
            
            // Keep grid mapping controls visible if we have a saved grid configuration
            // This allows the grid system to work on new panels
            if (presenter.hasValidGridConfiguration()) {
                updateGridMappingStatus("Ready for grid mapping on Panel " + currentPanelId + ". Grid configuration restored.");
                // Enable the Define Grid button since we have a valid configuration
                if (defineGridButton != null) {
                    defineGridButton.setDisable(false);
                }
                // Show the grid configuration section
                if (gridConfigSection != null) {
                    gridConfigSection.setVisible(true);
                    gridConfigSection.setManaged(true);
                }
                // Enable grid mode checkbox
                if (useGridModeCheckBox != null && !useGridModeCheckBox.isSelected()) {
                    useGridModeCheckBox.setSelected(true);
                }
            } else {
                // No grid configuration available, but still allow manual grid setup
                updateGridMappingStatus("Panel " + currentPanelId + " ready. Configure grid settings to enable auto-mapping.");
            }
        }
    }
    
    @FXML
    private void showGridHelp() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Grid Mapping Help");
        alert.setHeaderText("How to use Grid Mapping");
        alert.setContentText(
            "1. Enable 'Use Grid Mode' checkbox\n" +
            "2. Enter number range (e.g., 1-35)\n" +
            "3. Set grid dimensions (e.g., 7×5)\n" +
            "4. Choose fill order\n" +
            "5. Click 'Define Grid'\n" +
            "6. Click two opposite corners of the number grid\n" +
            "7. FINE-TUNE: Use arrow buttons to adjust position\n" +
            "   • Arrow buttons: Move grid in small steps\n" +
            "   • Step size: Adjustable (1-20 pixels)\n" +
            "   • Expand/Shrink: Resize the entire grid\n" +
            "   • Reset: Return to original position\n" +
            "8. Click 'Auto-Map All' to place all numbers\n\n" +
            "For Cash Five: Use 7 columns × 5 rows, Column (bottom→top)"
        );
        alert.showAndWait();
    }
    
    @Override
    public void setGridMappingControlsVisible(boolean visible) {
        if (gridMappingSection != null) {
            gridMappingSection.setVisible(visible);
            gridMappingSection.setManaged(visible);
        }
    }
    
    @Override
    public void setGridMappingMode(GridMappingMode mode) {
        this.gridMode = mode;
        updateGridMappingStatus(mode.getStatusMessage());
    }
    
    @Override
    public void showGridCornerMarker(double x, double y, boolean isFirst) {
        Circle marker = new Circle(x, y, 5, Color.RED);
        marker.setStroke(Color.BLACK);
        marker.setStrokeWidth(1);
        
        if (isFirst) {
            firstCornerMarker = marker;
        } else {
            secondCornerMarker = marker;
        }
        
        drawingPane.getChildren().add(marker);
    }
    
    @Override
    public void showGridOverlay(GridDefinition grid) {
        showGridOverlay(grid, inTuningMode);
    }
    
    // Enhanced method with simplified mode option
    public void showGridOverlay(GridDefinition grid, boolean simplified) {
        if (grid == null || !grid.isValid()) return;
        
        // Clear existing grid visuals before drawing new ones
        clearGridLines();
        
        // Remove existing bounding box if it exists
        if (gridBoundingBox != null) {
            drawingPane.getChildren().remove(gridBoundingBox);
            gridBoundingBox = null;
        }
        
        // Draw new bounding box
        gridBoundingBox = new Rectangle(
            grid.getTopLeftX(), 
            grid.getTopLeftY(),
            grid.getBottomRightX() - grid.getTopLeftX(),
            grid.getBottomRightY() - grid.getTopLeftY()
        );
        gridBoundingBox.setFill(Color.TRANSPARENT);
        gridBoundingBox.setStroke(simplified ? Color.DARKBLUE : Color.BLUE);
        gridBoundingBox.setStrokeWidth(simplified ? 3 : 2);
        gridBoundingBox.setOpacity(simplified ? 0.8 : 0.5);
        drawingPane.getChildren().add(gridBoundingBox);
        
        // Only draw internal grid lines if not in simplified mode
        if (!simplified) {
            double cellWidth = grid.getCellWidth();
            double cellHeight = grid.getCellHeight();
            
            // Vertical lines
            for (int i = 1; i < grid.getColumns(); i++) {
                Line line = new Line(
                    grid.getTopLeftX() + i * cellWidth,
                    grid.getTopLeftY(),
                    grid.getTopLeftX() + i * cellWidth,
                    grid.getBottomRightY()
                );
                line.setStroke(Color.GRAY);
                line.setOpacity(0.3);
                gridLines.add(line);
                drawingPane.getChildren().add(line);
            }
            
            // Horizontal lines
            for (int i = 1; i < grid.getRows(); i++) {
                Line line = new Line(
                    grid.getTopLeftX(),
                    grid.getTopLeftY() + i * cellHeight,
                    grid.getBottomRightX(),
                    grid.getTopLeftY() + i * cellHeight
                );
                line.setStroke(Color.GRAY);
                line.setOpacity(0.3);
                gridLines.add(line);
                drawingPane.getChildren().add(line);
            }
        }
    }
    
    @Override
    public void clearGridVisuals() {
        if (firstCornerMarker != null) {
            drawingPane.getChildren().remove(firstCornerMarker);
            firstCornerMarker = null;
        }
        if (secondCornerMarker != null) {
            drawingPane.getChildren().remove(secondCornerMarker);
            secondCornerMarker = null;
        }
        if (gridBoundingBox != null) {
            drawingPane.getChildren().remove(gridBoundingBox);
            gridBoundingBox = null;
        }
        clearGridLines();
    }
    
    private void clearGridLines() {
        drawingPane.getChildren().removeAll(gridLines);
        gridLines.clear();
    }
    
    private void handleGridCornerClick(double x, double y) {
        if (gridMode == GridMappingMode.DEFINING_FIRST_CORNER) {
            showGridCornerMarker(x, y, true);
            gridMode = GridMappingMode.DEFINING_SECOND_CORNER;
            updateGridMappingStatus("Click the opposite corner to complete the grid");
            presenter.setFirstGridCorner(x, y);
        } else if (gridMode == GridMappingMode.DEFINING_SECOND_CORNER) {
            showGridCornerMarker(x, y, false);
            gridMode = GridMappingMode.GRID_PREVIEW;
            presenter.setSecondGridCorner(x, y);
            presenter.previewGrid();
        }
    }
    
    @Override
    public void updateGridMappingStatus(String status) {
        if (gridStatusLabel != null) {
            gridStatusLabel.setText(status);
        }
    }
    
    @Override
    public String getGridNumberRange() {
        return gridRangeField != null ? gridRangeField.getText() : "1-35";
    }
    
    @Override
    public int getGridColumns() {
        return gridColumnsSpinner != null ? gridColumnsSpinner.getValue() : 7;
    }
    
    @Override
    public int getGridRows() {
        return gridRowsSpinner != null ? gridRowsSpinner.getValue() : 5;
    }
    
    @Override
    public String getGridFillOrder() {
        if (fillOrderGroup == null) return "COLUMN_BOTTOM_TO_TOP";
        
        RadioButton selected = (RadioButton) fillOrderGroup.getSelectedToggle();
        if (selected == columnBottomTopRadio) return "COLUMN_BOTTOM_TO_TOP";
        if (selected == columnTopBottomRadio) return "COLUMN_TOP_TO_BOTTOM";
        if (selected == rowLeftRightRadio) return "ROW_LEFT_TO_RIGHT";
        if (selected == rowRightLeftRadio) return "ROW_RIGHT_TO_LEFT";
        
        return "COLUMN_BOTTOM_TO_TOP";
    }
    
    @Override
    public boolean isGridModeEnabled() {
        return useGridModeCheckBox != null && useGridModeCheckBox.isSelected();
    }
    
    // Grid configuration setters (for auto-population)
    @Override
    public void setGridNumberRange(String range) {
        if (gridRangeField != null) {
            gridRangeField.setText(range);
        }
    }
    
    @Override
    public void setGridColumns(int columns) {
        if (gridColumnsSpinner != null) {
            gridColumnsSpinner.getValueFactory().setValue(columns);
        }
    }
    
    @Override
    public void setGridRows(int rows) {
        if (gridRowsSpinner != null) {
            gridRowsSpinner.getValueFactory().setValue(rows);
        }
    }
    
    @Override
    public void setGridFillOrder(String fillOrder) {
        if (fillOrderGroup == null) return;
        
        switch (fillOrder) {
            case "COLUMN_BOTTOM_TO_TOP":
                fillOrderGroup.selectToggle(columnBottomTopRadio);
                break;
            case "COLUMN_TOP_TO_BOTTOM":
                fillOrderGroup.selectToggle(columnTopBottomRadio);
                break;
            case "ROW_LEFT_TO_RIGHT":
                fillOrderGroup.selectToggle(rowLeftRightRadio);
                break;
            case "ROW_RIGHT_TO_LEFT":
                fillOrderGroup.selectToggle(rowRightLeftRadio);
                break;
        }
    }
    
    @Override
    public void showGridPreview(GridDefinition grid) {
        // Enable tuning mode and use simplified grid display
        inTuningMode = true;
        showGridOverlay(grid, true);
        autoMapButton.setVisible(true);
        clearGridButton.setVisible(true); // Allow clearing during tuning mode
        
        // Show adjustment controls
        if (gridAdjustmentSection != null) {
            gridAdjustmentSection.setVisible(true);
            gridAdjustmentSection.setManaged(true);
        }
        
        // Store original grid for reset
        originalGridDefinition = new GridDefinition(
            grid.getTopLeftX(), grid.getTopLeftY(),
            grid.getBottomRightX(), grid.getBottomRightY(),
            grid.getColumns(), grid.getRows(),
            grid.getStartNumber(), grid.getEndNumber(),
            grid.getFillOrder(), grid.getPanelId()
        );
        
        // Store original number positions before fine-tuning begins
        presenter.storeOriginalNumberPositions();
        
        
        updateGridMappingStatus("Grid defined. Fine-tune position if needed, then 'Auto-Map All'");
    }
    
    @Override
    public boolean confirmGridMapping(String message) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Confirm Grid Mapping");
        alert.setHeaderText(null);
        alert.setContentText(message);
        Optional<ButtonType> result = alert.showAndWait();
        return result.isPresent() && result.get() == ButtonType.OK;
    }
    
    @Override
    public void setGridMappingProgress(int current, int total) {
        if (gridStatusLabel != null) {
            gridStatusLabel.setText(String.format("Mapping: %d/%d", current, total));
        }
    }
    
    // Zoom control methods
    @FXML
    private void zoomIn() {
        if (currentZoom < MAX_ZOOM) {
            currentZoom = Math.min(MAX_ZOOM, currentZoom + ZOOM_STEP);
            applyZoom();
        }
    }
    
    @FXML
    private void zoomOut() {
        if (currentZoom > MIN_ZOOM) {
            currentZoom = Math.max(MIN_ZOOM, currentZoom - ZOOM_STEP);
            applyZoom();
        }
    }
    
    @FXML
    private void resetZoom() {
        currentZoom = 1.0;
        applyZoom();
    }
    
    private void applyZoom() {
        if (betslipImageView != null) {
            betslipImageView.setScaleX(currentZoom);
            betslipImageView.setScaleY(currentZoom);
            
            // Scale the drawing pane as well to keep overlays aligned
            if (drawingPane != null) {
                drawingPane.setScaleX(currentZoom);
                drawingPane.setScaleY(currentZoom);
            }
            
            // Update zoom label
            if (zoomLabel != null) {
                zoomLabel.setText(String.format("%.0f%%", currentZoom * 100));
            }
            
            // Enable/disable zoom buttons based on limits
            if (zoomInButton != null) {
                zoomInButton.setDisable(currentZoom >= MAX_ZOOM);
            }
            if (zoomOutButton != null) {
                zoomOutButton.setDisable(currentZoom <= MIN_ZOOM);
            }
        }
    }
    
    // Grid adjustment methods
    @FXML
    private void moveGridLeft() {
        adjustGrid(-getAdjustmentStep(), 0, 0, 0);
    }
    
    @FXML
    private void moveGridRight() {
        adjustGrid(getAdjustmentStep(), 0, 0, 0);
    }
    
    @FXML
    private void moveGridUp() {
        adjustGrid(0, -getAdjustmentStep(), 0, 0);
    }
    
    @FXML
    private void moveGridDown() {
        adjustGrid(0, getAdjustmentStep(), 0, 0);
    }
    
    @FXML
    private void expandGrid() {
        int step = getAdjustmentStep();
        adjustGrid(-step, -step, step, step);
    }
    
    @FXML
    private void shrinkGrid() {
        int step = getAdjustmentStep();
        adjustGrid(step, step, -step, -step);
    }
    
    @FXML
    private void resetGridPosition() {
        if (originalGridDefinition != null) {
            presenter.resetGridAndNumbers(originalGridDefinition);
            
            
            updateGridMappingStatus("Grid and numbers reset to original positions");
        }
    }
    
    private int getAdjustmentStep() {
        return adjustmentStepSpinner != null ? adjustmentStepSpinner.getValue() : 2;
    }
    
    // Step preset methods
    
    @FXML
    private void refineGrid() {
        if (presenter.getCurrentGrid() == null) {
            showError("Please define a grid first using the Auto-Map function.");
            return;
        }
        
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Grid Refinement Mode");
        alert.setHeaderText("Grid Refinement Tips");
        alert.setContentText(
            "REFINEMENT TIPS:\n\n" +
            "1. Use arrow buttons to nudge the entire grid\n" +
            "2. Use Expand/Shrink to adjust grid cell sizes\n" +
            "3. Adjust step size (1-20px) for precision\n" +
            "4. Use individual drag-and-drop for specific numbers\n" +
            "5. The grid overlay shows current boundaries\n\n" +
            "TIP: Start with small step sizes (1-2px) for fine adjustments!"
        );
        alert.showAndWait();
        
        // Enable special refinement mode with enhanced visual feedback
        updateGridMappingStatus("REFINEMENT MODE: Use arrow buttons or drag individual marks to improve accuracy");
    }
    
    private void adjustGrid(int deltaLeft, int deltaTop, int deltaRight, int deltaBottom) {
        if (presenter.getCurrentGrid() != null && presenter.getCurrentGrid().isValid()) {
            GridDefinition current = presenter.getCurrentGrid();
            GridDefinition adjusted = new GridDefinition(
                current.getTopLeftX() + deltaLeft,
                current.getTopLeftY() + deltaTop,
                current.getBottomRightX() + deltaRight,
                current.getBottomRightY() + deltaBottom,
                current.getColumns(),
                current.getRows(),
                current.getStartNumber(),
                current.getEndNumber(),
                current.getFillOrder(),
                current.getPanelId()
            );
            
            if (adjusted.isValid()) {
                presenter.updateGridDefinition(adjusted);
                updateGridMappingStatus("Grid adjusted. Position looks good? Click 'Auto-Map All'");
            } else {
                showError("Grid adjustment would create invalid dimensions");
            }
        }
    }
    
    
    // Column/Row Selection Methods
    
    @FXML
    private void toggleColumnMode() {
        if (!columnModeButton.isSelected()) {
            columnModeButton.setSelected(true);
            return;
        }
        
        isColumnMode = true;
        rowModeButton.setSelected(false);
        clearColumnRowSelection();
        updateSelectionStatusLabel();
    }
    
    @FXML
    private void toggleRowMode() {
        if (!rowModeButton.isSelected()) {
            rowModeButton.setSelected(true);
            return;
        }
        
        isColumnMode = false;
        columnModeButton.setSelected(false);
        clearColumnRowSelection();
        updateSelectionStatusLabel();
    }
    
    @FXML
    private void clearColumnRowSelection() {
        clearHighlighting();
        selectedColumn = -1;
        selectedRow = -1;
        updateSelectionStatusLabel();
    }
    
    private void clearHighlighting() {
        // Remove highlighting from all rectangles
        for (Rectangle rect : highlightedRectangles) {
            // Reset to original color based on type
            if (rect.getUserData() instanceof CoordinateInfo) {
                rect.setStroke(Color.BLACK);
                rect.setStrokeWidth(1);
            }
        }
        highlightedRectangles.clear();
        selectedRectangles.clear();
    }
    
    private void updateSelectionStatusLabel() {
        if (selectionStatusLabel == null) return;
        
        if (selectedColumn >= 0 || selectedRow >= 0) {
            if (isColumnMode) {
                selectionStatusLabel.setText("Column " + (selectedColumn + 1) + " selected - Use arrow keys to move (click image area first)");
            } else {
                selectionStatusLabel.setText("Row " + (selectedRow + 1) + " selected - Use arrow keys to move (click image area first)");
            }
        } else {
            if (isColumnMode) {
                selectionStatusLabel.setText("Click a number to select its column, then use arrow keys");
            } else {
                selectionStatusLabel.setText("Click a number to select its row, then use arrow keys");
            }
        }
    }
    
    /**
     * Handle number selection for column/row mode
     */
    private void handleNumberSelection(Rectangle rectangle) {
        if (!columnRowSelectionMode || presenter.getCurrentGrid() == null) {
            return;
        }
        
        // Get the coordinate and find the corresponding number
        if (!(rectangle.getUserData() instanceof CoordinateInfo)) {
            return;
        }
        
        CoordinateInfo coordInfo = (CoordinateInfo) rectangle.getUserData();
        Coordinate coord = coordInfo.getCoordinate();
        String numberString = findNumberForCoordinate(coord, coordInfo.getPanelId());
        
        if (numberString == null) {
            return;
        }
        
        try {
            int number = Integer.parseInt(numberString);
            GridDefinition grid = presenter.getCurrentGrid();
            
            clearHighlighting();
            
            if (isColumnMode) {
                // Select column
                selectedColumn = GridCalculator.getColumnForNumber(number, grid);
                selectedRow = -1;
                highlightColumn(selectedColumn);
            } else {
                // Select row
                selectedRow = GridCalculator.getRowForNumber(number, grid);
                selectedColumn = -1;
                highlightRow(selectedRow);
            }
            
            // Ensure drawing pane has focus for keyboard events
            drawingPane.requestFocus();
            
            updateSelectionStatusLabel();
            
        } catch (NumberFormatException e) {
            // Invalid number, ignore
        }
    }
    
    /**
     * Find the number string that corresponds to a coordinate
     */
    private String findNumberForCoordinate(Coordinate target, String panelId) {
        if (panelId == null) return null;
        
        // Check main numbers
        for (Map.Entry<String, Coordinate> entry : getMainNumbersForPanel(panelId).entrySet()) {
            Coordinate coord = entry.getValue();
            if (coord.getX() == target.getX() && coord.getY() == target.getY()) {
                return entry.getKey();
            }
        }
        
        return null;
    }
    
    /**
     * Get main numbers for a panel - helper method
     */
    private Map<String, Coordinate> getMainNumbersForPanel(String panelId) {
        // Access through presenter to get the current panel's main numbers
        return presenter.getMainNumbersForPanel(panelId);
    }
    
    /**
     * Check if a rectangle belongs to the current selected panel
     */
    private boolean isRectangleInCurrentPanel(Rectangle rect) {
        if (!(rect.getUserData() instanceof CoordinateInfo)) {
            return false;
        }
        
        String currentPanelId = getSelectedPanel();
        if (currentPanelId == null) {
            return false;
        }
        
        CoordinateInfo coordInfo = (CoordinateInfo) rect.getUserData();
        return currentPanelId.equals(coordInfo.getPanelId());
    }
    
    private void highlightColumn(int columnIndex) {
        if (columnIndex < 0 || presenter.getCurrentGrid() == null) return;
        
        GridDefinition grid = presenter.getCurrentGrid();
        String currentPanelId = getSelectedPanel();
        
        if (currentPanelId == null) {
            return;
        }
        
        // Get all numbers in the specified column using GridCalculator
        List<Integer> numbersInColumn = GridCalculator.getNumbersInColumn(columnIndex, grid);
        Map<String, Coordinate> currentPanelNumbers = getMainNumbersForPanel(currentPanelId);
        
        // Find rectangles that correspond to numbers in this column AND belong to current panel
        for (Node node : drawingPane.getChildren()) {
            if (node instanceof Rectangle && node.getUserData() instanceof CoordinateInfo) {
                Rectangle rect = (Rectangle) node;
                
                // Only process rectangles that belong to the current panel
                if (!isRectangleInCurrentPanel(rect)) {
                    continue;
                }
                
                CoordinateInfo coordInfo = (CoordinateInfo) rect.getUserData();
                Coordinate coord = coordInfo.getCoordinate();
                
                // Check if this coordinate matches any number in the specified column
                String matchedNumber = null;
                for (Map.Entry<String, Coordinate> entry : currentPanelNumbers.entrySet()) {
                    if (entry.getValue().getX() == coord.getX() && entry.getValue().getY() == coord.getY()) {
                        try {
                            int number = Integer.parseInt(entry.getKey());
                            if (numbersInColumn.contains(number)) {
                                matchedNumber = entry.getKey();
                                break;
                            }
                        } catch (NumberFormatException e) {
                            // Skip non-numeric keys
                        }
                    }
                }
                
                if (matchedNumber != null) {
                    rect.setStroke(Color.ORANGE);
                    rect.setStrokeWidth(3);
                    highlightedRectangles.add(rect);
                    selectedRectangles.add(rect); // Store for movement
                }
            }
        }
    }
    
    private void highlightRow(int rowIndex) {
        if (rowIndex < 0 || presenter.getCurrentGrid() == null) return;
        
        GridDefinition grid = presenter.getCurrentGrid();
        String currentPanelId = getSelectedPanel();
        
        if (currentPanelId == null) {
            return;
        }
        
        // Get all numbers in the specified row using GridCalculator
        List<Integer> numbersInRow = GridCalculator.getNumbersInRow(rowIndex, grid);
        Map<String, Coordinate> currentPanelNumbers = getMainNumbersForPanel(currentPanelId);
        
        // Find rectangles that correspond to numbers in this row AND belong to current panel
        for (Node node : drawingPane.getChildren()) {
            if (node instanceof Rectangle && node.getUserData() instanceof CoordinateInfo) {
                Rectangle rect = (Rectangle) node;
                
                // Only process rectangles that belong to the current panel
                if (!isRectangleInCurrentPanel(rect)) {
                    continue;
                }
                
                CoordinateInfo coordInfo = (CoordinateInfo) rect.getUserData();
                Coordinate coord = coordInfo.getCoordinate();
                
                // Check if this coordinate matches any number in the specified row
                String matchedNumber = null;
                for (Map.Entry<String, Coordinate> entry : currentPanelNumbers.entrySet()) {
                    if (entry.getValue().getX() == coord.getX() && entry.getValue().getY() == coord.getY()) {
                        try {
                            int number = Integer.parseInt(entry.getKey());
                            if (numbersInRow.contains(number)) {
                                matchedNumber = entry.getKey();
                                break;
                            }
                        } catch (NumberFormatException e) {
                            // Skip non-numeric keys
                        }
                    }
                }
                
                if (matchedNumber != null) {
                    rect.setStroke(Color.LIME);
                    rect.setStrokeWidth(3);
                    highlightedRectangles.add(rect);
                    selectedRectangles.add(rect); // Store for movement
                }
            }
        }
    }
    
    /**
     * Find the rectangle that represents a specific number
     */
    private Rectangle findRectangleForNumber(String number) {
        String panelId = getSelectedPanel();
        if (panelId == null) return null;
        
        Map<String, Coordinate> mainNumbers = getMainNumbersForPanel(panelId);
        Coordinate targetCoord = mainNumbers.get(number);
        if (targetCoord == null) return null;
        
        // Find rectangle with matching coordinate
        for (Node node : drawingPane.getChildren()) {
            if (node instanceof Rectangle && node.getUserData() instanceof CoordinateInfo) {
                CoordinateInfo coordInfo = (CoordinateInfo) node.getUserData();
                Coordinate coord = coordInfo.getCoordinate();
                if (coord.getX() == targetCoord.getX() && coord.getY() == targetCoord.getY()) {
                    return (Rectangle) node;
                }
            }
        }
        
        return null;
    }
    
    private void handleKeyPress(javafx.scene.input.KeyEvent event) {
        
        // Handle both arrow keys and shift+arrow keys for column/row movement
        if (columnRowSelectionMode) {
            switch (event.getCode()) {
                case LEFT:
                case RIGHT:
                case UP:
                case DOWN:
                    event.consume(); // Consume first to prevent scrolling
                    break;
                default:
                    return; // Don't handle other keys
            }
            
            // Only proceed with movement if we have a selection
            if (selectedColumn < 0 && selectedRow < 0) {
                return;
            }
            
            // Use different step sizes for fine vs coarse adjustment
            // Shift+Arrow = Fine adjustment (smaller steps), Arrow alone = Normal steps
            int stepSize = event.isShiftDown() ? Math.max(1, getAdjustmentStep() / 2) : getAdjustmentStep();
            
            switch (event.getCode()) {
                case LEFT:
                    if (isColumnMode && selectedColumn >= 0) {
                        moveColumn(-stepSize, 0);
                    } else if (!isColumnMode && selectedRow >= 0) {
                        moveRow(-stepSize, 0);
                    }
                    break;
                case RIGHT:
                    if (isColumnMode && selectedColumn >= 0) {
                        moveColumn(stepSize, 0);
                    } else if (!isColumnMode && selectedRow >= 0) {
                        moveRow(stepSize, 0);
                    }
                    break;
                case UP:
                    if (isColumnMode && selectedColumn >= 0) {
                        moveColumn(0, -stepSize);
                    } else if (!isColumnMode && selectedRow >= 0) {
                        moveRow(0, -stepSize);
                    }
                    break;
                case DOWN:
                    if (isColumnMode && selectedColumn >= 0) {
                        moveColumn(0, stepSize);
                    } else if (!isColumnMode && selectedRow >= 0) {
                        moveRow(0, stepSize);
                    }
                    break;
            }
        }
    }
    
    private void moveColumn(int deltaX, int deltaY) {
        if (selectedColumn < 0 || selectedRectangles.isEmpty()) {
            return;
        }
        
        List<Coordinate> oldPositions = new ArrayList<>();
        
        // Move all rectangles in selected column
        for (Rectangle rect : selectedRectangles) {
            if (rect.getUserData() instanceof CoordinateInfo) {
                CoordinateInfo coordInfo = (CoordinateInfo) rect.getUserData();
                Coordinate coord = coordInfo.getCoordinate();
                
                // Store old position for undo
                oldPositions.add(new Coordinate(coord.getX(), coord.getY()));
                
                // Update coordinate data
                coord.setX(coord.getX() + deltaX);
                coord.setY(coord.getY() + deltaY);
                
                // Update visual position
                rect.setX(rect.getX() + deltaX);
                rect.setY(rect.getY() + deltaY);
            }
        }
        
        // Add to undo stack
        final List<Rectangle> finalRectangles = new ArrayList<>(selectedRectangles);
        final List<Coordinate> finalOldPositions = new ArrayList<>(oldPositions);
        presenter.addToUndoStack(() -> {
            for (int i = 0; i < finalRectangles.size(); i++) {
                Rectangle rect = finalRectangles.get(i);
                Coordinate oldPos = finalOldPositions.get(i);
                if (rect.getUserData() instanceof CoordinateInfo && oldPos != null) {
                    CoordinateInfo coordInfo = (CoordinateInfo) rect.getUserData();
                    Coordinate coord = coordInfo.getCoordinate();
                    coord.setX(oldPos.getX());
                    coord.setY(oldPos.getY());
                    rect.setX(oldPos.getX() - rect.getWidth() / 2);
                    rect.setY(oldPos.getY() - rect.getHeight() / 2);
                }
            }
            clearHighlighting();
            highlightColumn(selectedColumn);
        });
        
        updateGridMappingStatus("Column " + (selectedColumn + 1) + " moved by (" + deltaX + ", " + deltaY + ")");
    }
    
    private void moveRow(int deltaX, int deltaY) {
        if (selectedRow < 0 || selectedRectangles.isEmpty()) return;
        
        List<Coordinate> oldPositions = new ArrayList<>();
        
        // Move all rectangles in selected row
        for (Rectangle rect : selectedRectangles) {
            if (rect.getUserData() instanceof CoordinateInfo) {
                CoordinateInfo coordInfo = (CoordinateInfo) rect.getUserData();
                Coordinate coord = coordInfo.getCoordinate();
                
                // Store old position for undo
                oldPositions.add(new Coordinate(coord.getX(), coord.getY()));
                
                // Update coordinate data
                coord.setX(coord.getX() + deltaX);
                coord.setY(coord.getY() + deltaY);
                
                // Update visual position
                rect.setX(rect.getX() + deltaX);
                rect.setY(rect.getY() + deltaY);
            }
        }
        
        // Add to undo stack
        final List<Rectangle> finalRectangles = new ArrayList<>(selectedRectangles);
        final List<Coordinate> finalOldPositions = new ArrayList<>(oldPositions);
        presenter.addToUndoStack(() -> {
            for (int i = 0; i < finalRectangles.size(); i++) {
                Rectangle rect = finalRectangles.get(i);
                Coordinate oldPos = finalOldPositions.get(i);
                if (rect.getUserData() instanceof CoordinateInfo && oldPos != null) {
                    CoordinateInfo coordInfo = (CoordinateInfo) rect.getUserData();
                    Coordinate coord = coordInfo.getCoordinate();
                    coord.setX(oldPos.getX());
                    coord.setY(oldPos.getY());
                    rect.setX(oldPos.getX() - rect.getWidth() / 2);
                    rect.setY(oldPos.getY() - rect.getHeight() / 2);
                }
            }
            clearHighlighting();
            highlightRow(selectedRow);
        });
        
        updateGridMappingStatus("Row " + (selectedRow + 1) + " moved by (" + deltaX + ", " + deltaY + ")");
    }
}
