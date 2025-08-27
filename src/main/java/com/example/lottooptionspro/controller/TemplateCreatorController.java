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
        setSelectedMarkControlsVisible(false);
        // Make grid mapping visible by default for testing
        setGridMappingControlsVisible(true);
        // Initialize zoom controls
        initializeZoomControls();
    }

    private void populateComboBoxes() {
        mappingModeComboBox.setItems(FXCollections.observableArrayList("Main Number", "Bonus Number", "Quick Pick", "Global Option", "Scanner Mark"));
        mappingModeComboBox.getSelectionModel().selectFirst();
        panelComboBox.setItems(FXCollections.observableArrayList("A", "B", "C", "D", "E"));
        panelComboBox.getSelectionModel().selectFirst();
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
        panelComboBox.getSelectionModel().selectedItemProperty().addListener((obs, o, n) -> presenter.onPanelOrModeChanged());
        markWidthSpinner.valueProperty().addListener((obs, o, n) -> presenter.updateMarkSize(n, markHeightSpinner.getValue()));
        markHeightSpinner.valueProperty().addListener((obs, o, n) -> presenter.updateMarkSize(markWidthSpinner.getValue(), n));

        selectedMarkWidthSpinner.valueProperty().addListener((obs, o, n) -> {
            if (selectedRectangle != null && selectedRectangle.getUserData() instanceof ScannerMark) {
                presenter.updateScannerMarkSize((ScannerMark) selectedRectangle.getUserData(), n, selectedMarkHeightSpinner.getValue());
            }
        });
        selectedMarkHeightSpinner.valueProperty().addListener((obs, o, n) -> {
            if (selectedRectangle != null && selectedRectangle.getUserData() instanceof ScannerMark) {
                presenter.updateScannerMarkSize((ScannerMark) selectedRectangle.getUserData(), selectedMarkWidthSpinner.getValue(), n);
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
            setSelectedMarkControlsVisible(false);
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
            if (rect.getUserData() instanceof Coordinate) {
                presenter.startCoordinateMove((Coordinate) rect.getUserData());
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
                if (userData instanceof Coordinate) {
                    presenter.finishCoordinateMove((Coordinate) userData, (int) (newX + rect.getWidth() / 2), (int) (newY + rect.getHeight() / 2));
                } else if (userData instanceof ScannerMark) {
                    presenter.finishScannerMarkMove((ScannerMark) userData, newX, newY);
                }
            } else {
                // Handle column/row selection when not dragging
                System.out.println("Rectangle clicked (not dragged)");
                System.out.println("columnRowSelectionMode: " + columnRowSelectionMode);
                System.out.println("userData instanceof Coordinate: " + (rect.getUserData() instanceof Coordinate));
                
                if (columnRowSelectionMode && rect.getUserData() instanceof Coordinate) {
                    System.out.println("Calling handleNumberSelection");
                    handleNumberSelection(rect);
                } else {
                    System.out.println("Not calling handleNumberSelection");
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
        if (rect.getUserData() instanceof ScannerMark) {
            selectedRectangle = rect;
            selectedRectangle.setStroke(Color.RED);
            ScannerMark mark = (ScannerMark) rect.getUserData();
            setSelectedMarkDimensions(mark.getWidth(), mark.getHeight());
            setSelectedMarkControlsVisible(true);
        }
    }

    private void unselectRectangle() {
        if (selectedRectangle != null) {
            if (selectedRectangle.getUserData() instanceof ScannerMark) {
                selectedRectangle.setStroke(Color.BLUE);
            } else {
                selectedRectangle.setStroke(Color.BLACK);
            }
        }
        selectedRectangle = null;
        setSelectedMarkControlsVisible(false);
    }

    @Override
    public void drawRectangle(Coordinate coordinate, int width, int height) {
        double x = coordinate.getX() - (double) width / 2;
        double y = coordinate.getY() - (double) height / 2;
        Rectangle rect = new Rectangle(x, y, width, height);
        rect.setUserData(coordinate);
        rect.setFill(Color.TRANSPARENT);
        rect.setStroke(Color.BLACK);
        rect.setStrokeWidth(1);
        
        // Add direct mouse event handlers for column/row selection
        rect.setOnMousePressed(e -> {
            String number = findNumberForCoordinate(coordinate);
            System.out.println("Rectangle mouse pressed: " + number);
            pressTarget = rect;
            wasDragged = false;
            if (rect instanceof Rectangle) {
                handleRectangleSelection(rect);
                dragOffsetX = e.getX() - rect.getX();
                dragOffsetY = e.getY() - rect.getY();
                rect.setCursor(Cursor.MOVE);
                
                // Initialize coordinate move for individual dragging
                if (rect.getUserData() instanceof Coordinate) {
                    presenter.startCoordinateMove((Coordinate) rect.getUserData());
                }
            }
            e.consume();
        });
        
        rect.setOnMouseDragged(e -> {
            if (pressTarget instanceof Rectangle) {
                wasDragged = true;
                Rectangle targetRect = (Rectangle) pressTarget;
                // Use pane-relative coordinates instead of scene coordinates
                double newX = e.getX() - dragOffsetX;
                double newY = e.getY() - dragOffsetY;
                targetRect.setX(newX);
                targetRect.setY(newY);
            }
            e.consume();
        });
        
        rect.setOnMouseReleased(e -> {
            String number = findNumberForCoordinate(coordinate);
            System.out.println("Rectangle mouse released: " + number + ", wasDragged=" + wasDragged);
            if (pressTarget instanceof Rectangle) {
                Rectangle targetRect = (Rectangle) pressTarget;
                if (!wasDragged) {
                    // Handle column/row selection when not dragging
                    System.out.println("Rectangle clicked (not dragged) - " + number);
                    System.out.println("columnRowSelectionMode: " + columnRowSelectionMode);
                    
                    if (columnRowSelectionMode && targetRect.getUserData() instanceof Coordinate) {
                        System.out.println("Calling handleNumberSelection for " + number);
                        handleNumberSelection(targetRect);
                    }
                } else {
                    // Handle drag completion
                    Object userData = targetRect.getUserData();
                    double newX = targetRect.getX();
                    double newY = targetRect.getY();
                    if (userData instanceof Coordinate) {
                        presenter.finishCoordinateMove((Coordinate) userData, (int) (newX + targetRect.getWidth() / 2), (int) (newY + targetRect.getHeight() / 2));
                    }
                }
                targetRect.setCursor(Cursor.DEFAULT);
            }
            pressTarget = null;
            wasDragged = false;
            e.consume();
        });
        
        drawingPane.getChildren().add(rect);
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
    public void setSelectedMarkControlsVisible(boolean visible) {
        selectedMarkControls.setVisible(visible);
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
    private void clearLastMarking() { presenter.removeLastMarking(); }

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
    public String getSelectedPanel() { return panelComboBox.getValue(); }

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
        // Ask user if they want to clear all markings or just grid
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Clear Grid");
        alert.setHeaderText("Choose what to clear:");
        alert.setContentText("Do you want to clear everything (all markings, grid, scanner marks) or just the grid overlay?");
        
        ButtonType clearAllButton = new ButtonType("Clear All");
        ButtonType clearGridOnlyButton = new ButtonType("Grid Only");
        ButtonType cancelButton = new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE);
        
        alert.getButtonTypes().setAll(clearAllButton, clearGridOnlyButton, cancelButton);
        
        Optional<ButtonType> result = alert.showAndWait();
        
        if (result.isPresent()) {
            if (result.get() == clearAllButton) {
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
        
        updateGridMappingStatus("All drawings and markings cleared");
    }
    
    @FXML
    private void autoMapNumbers() {
        presenter.autoMapGridNumbers();
        
        // Enable column/row fine-tuning after auto-mapping
        enableColumnRowTuning();
    }
    
    private void enableColumnRowTuning() {
        System.out.println("enableColumnRowTuning called");
        System.out.println("columnRowTuningSection != null: " + (columnRowTuningSection != null));
        
        if (columnRowTuningSection != null) {
            columnRowTuningSection.setVisible(true);
            columnRowTuningSection.setManaged(true);
            columnRowSelectionMode = true;
            
            System.out.println("Column/Row tuning enabled. columnRowSelectionMode = " + columnRowSelectionMode);
            
            // Ensure drawing pane can receive focus and keyboard events
            drawingPane.setFocusTraversable(true);
            drawingPane.requestFocus();
            
            updateGridMappingStatus("Numbers auto-mapped! Click a number to select column/row, then use arrow keys to move.");
        } else {
            System.out.println("columnRowTuningSection is null!");
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
            if (rect.getUserData() instanceof Coordinate) {
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
        System.out.println("handleNumberSelection called");
        System.out.println("columnRowSelectionMode: " + columnRowSelectionMode);
        System.out.println("currentGrid != null: " + (presenter.getCurrentGrid() != null));
        
        if (!columnRowSelectionMode || presenter.getCurrentGrid() == null) {
            System.out.println("Early return from handleNumberSelection");
            return;
        }
        
        // Get the coordinate and find the corresponding number
        if (!(rectangle.getUserData() instanceof Coordinate)) {
            System.out.println("Rectangle userData is not Coordinate: " + rectangle.getUserData());
            return;
        }
        
        Coordinate coord = (Coordinate) rectangle.getUserData();
        System.out.println("Found coordinate: " + coord.getX() + ", " + coord.getY());
        
        String numberString = findNumberForCoordinate(coord);
        System.out.println("Found number string: " + numberString);
        
        if (numberString == null) {
            System.out.println("Number string is null - returning");
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
    private String findNumberForCoordinate(Coordinate target) {
        String panelId = presenter.getCurrentGrid() != null ? presenter.getCurrentGrid().getPanelId() : getSelectedPanel();
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
    
    private void highlightColumn(int columnIndex) {
        if (columnIndex < 0 || presenter.getCurrentGrid() == null) return;
        
        GridDefinition grid = presenter.getCurrentGrid();
        
        // Instead of finding by number, find rectangles by their position relative to the grid
        double cellWidth = grid.getCellWidth() + grid.getHorizontalSpacing();
        double gridLeft = grid.getAdjustedTopLeftX();
        
        // Calculate the approximate X range for this column
        double columnLeft = gridLeft + (columnIndex * cellWidth) - (cellWidth / 4); // Allow some tolerance
        double columnRight = gridLeft + ((columnIndex + 1) * cellWidth) + (cellWidth / 4);
        
        // Find all rectangles whose center X coordinate falls in this column's range
        for (Node node : drawingPane.getChildren()) {
            if (node instanceof Rectangle && node.getUserData() instanceof Coordinate) {
                Rectangle rect = (Rectangle) node;
                Coordinate coord = (Coordinate) rect.getUserData();
                
                // Check if this rectangle's center X is in this column's range
                double rectCenterX = coord.getX();
                if (rectCenterX >= columnLeft && rectCenterX <= columnRight) {
                    rect.setStroke(Color.ORANGE);
                    rect.setStrokeWidth(3);
                    highlightedRectangles.add(rect);
                    selectedRectangles.add(rect); // Store for movement
                }
            }
        }
        
        System.out.println("Highlighted column " + columnIndex + " with " + selectedRectangles.size() + " rectangles");
    }
    
    private void highlightRow(int rowIndex) {
        if (rowIndex < 0 || presenter.getCurrentGrid() == null) return;
        
        GridDefinition grid = presenter.getCurrentGrid();
        
        // Find rectangles by their Y position relative to the grid
        double cellHeight = grid.getCellHeight() + grid.getVerticalSpacing();
        double gridTop = grid.getAdjustedTopLeftY();
        
        // Calculate the approximate Y range for this row
        double rowTop = gridTop + (rowIndex * cellHeight) - (cellHeight / 4); // Allow some tolerance
        double rowBottom = gridTop + ((rowIndex + 1) * cellHeight) + (cellHeight / 4);
        
        // Find all rectangles whose center Y coordinate falls in this row's range
        for (Node node : drawingPane.getChildren()) {
            if (node instanceof Rectangle && node.getUserData() instanceof Coordinate) {
                Rectangle rect = (Rectangle) node;
                Coordinate coord = (Coordinate) rect.getUserData();
                
                // Check if this rectangle's center Y is in this row's range
                double rectCenterY = coord.getY();
                if (rectCenterY >= rowTop && rectCenterY <= rowBottom) {
                    rect.setStroke(Color.LIME);
                    rect.setStrokeWidth(3);
                    highlightedRectangles.add(rect);
                    selectedRectangles.add(rect); // Store for movement
                }
            }
        }
        
        System.out.println("Highlighted row " + rowIndex + " with " + selectedRectangles.size() + " rectangles");
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
            if (node instanceof Rectangle && node.getUserData() instanceof Coordinate) {
                Coordinate coord = (Coordinate) node.getUserData();
                if (coord.getX() == targetCoord.getX() && coord.getY() == targetCoord.getY()) {
                    return (Rectangle) node;
                }
            }
        }
        
        return null;
    }
    
    private void handleKeyPress(javafx.scene.input.KeyEvent event) {
        System.out.println("handleKeyPress called: " + event.getCode() + 
                          ", columnRowSelectionMode=" + columnRowSelectionMode +
                          ", selectedColumn=" + selectedColumn + ", selectedRow=" + selectedRow);
        
        // Always consume arrow key events when in column/row mode to prevent scroll pane movement
        if (columnRowSelectionMode) {
            switch (event.getCode()) {
                case LEFT:
                case RIGHT:
                case UP:
                case DOWN:
                    System.out.println("Consuming arrow key event: " + event.getCode());
                    event.consume(); // Consume first to prevent scrolling
                    break;
                default:
                    return; // Don't handle other keys
            }
            
            // Only proceed with movement if we have a selection
            if (selectedColumn < 0 && selectedRow < 0) {
                System.out.println("No selection - returning early");
                return;
            }
            
            int stepSize = getAdjustmentStep();
            
            switch (event.getCode()) {
                case LEFT:
                    if (isColumnMode && selectedColumn >= 0) {
                        moveColumn(-stepSize, 0);
                    }
                    break;
                case RIGHT:
                    if (isColumnMode && selectedColumn >= 0) {
                        moveColumn(stepSize, 0);
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
        System.out.println("moveColumn called: deltaX=" + deltaX + ", deltaY=" + deltaY + 
                          ", selectedColumn=" + selectedColumn + ", rectangles=" + selectedRectangles.size());
        
        if (selectedColumn < 0 || selectedRectangles.isEmpty()) {
            System.out.println("moveColumn: Early return - no selection or empty rectangles");
            return;
        }
        
        List<Coordinate> oldPositions = new ArrayList<>();
        
        // Move all rectangles in selected column
        for (Rectangle rect : selectedRectangles) {
            if (rect.getUserData() instanceof Coordinate) {
                Coordinate coord = (Coordinate) rect.getUserData();
                
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
                if (rect.getUserData() instanceof Coordinate && oldPos != null) {
                    Coordinate coord = (Coordinate) rect.getUserData();
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
            if (rect.getUserData() instanceof Coordinate) {
                Coordinate coord = (Coordinate) rect.getUserData();
                
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
                if (rect.getUserData() instanceof Coordinate && oldPos != null) {
                    Coordinate coord = (Coordinate) rect.getUserData();
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
