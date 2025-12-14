package com.example.lottooptionspro.controller;

import com.example.lottooptionspro.GameInformation;
import com.example.lottooptionspro.model.cache.DeltaSelectionCache;
import com.example.lottooptionspro.model.deltapick.DeltaInputMode;
import com.example.lottooptionspro.model.range.*;
import com.example.lottooptionspro.presenter.RangeAnalysisPresenter;
import com.example.lottooptionspro.presenter.RangeAnalysisView;
import com.example.lottooptionspro.service.DeltaSelectionCacheService;
import jakarta.annotation.PostConstruct;
import javafx.application.Platform;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.*;
import javafx.scene.control.ButtonBar.ButtonData;
import javafx.scene.layout.HBox;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Region;
import javafx.geometry.Pos;
import javafx.animation.FadeTransition;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.util.Duration;
import java.util.Timer;
import java.util.TimerTask;
import javafx.scene.layout.VBox;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import net.rgielen.fxweaver.core.FxmlView;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Controller
@FxmlView("/com.example.lottooptionspro/controller/RangeAnalysisView.fxml")
public class RangeAnalysisController implements RangeAnalysisView, GameInformation {
    
    private static final Logger logger = LoggerFactory.getLogger(RangeAnalysisController.class);
    
    // FXML Controls
    @FXML private Label stateGameLabel;
    @FXML private ProgressIndicator loadingIndicator;
    @FXML private TextField rangeSizeField;
    @FXML private Label rangeSizeHint;
    @FXML private ComboBox<AnalysisType> analysisTypeComboBox;
    @FXML private FlowPane positionCheckboxContainer;
    @FXML private Label chartTitle;
    @FXML private LineChart<Number, Number> rangeHitChart;
    @FXML private NumberAxis drawNumberAxis;
    @FXML private Label lastUpdatedLabel;
    @FXML private Label mostLikelyRangesLabel;
    @FXML private Label leastLikelyRangesLabel;
    @FXML private VBox positionDistributionChart;
    @FXML private ComboBox<Integer> rowsPerPageCombo;
    @FXML private Button previousButton;
    @FXML private Button nextButton;
    @FXML private StackPane loadingOverlay;
    @FXML private AnchorPane rootContainer;
    
    private Timer debounceTimer;
    private final int DEBOUNCE_DELAY = 300; // milliseconds
    @FXML private NumberAxis hitCountAxis;
    @FXML private TableView<DrawResultTableRow> fixedDateTable;
    @FXML private TableView<DrawResultTableRow> scrollableRangeTable;
    @FXML private ScrollPane rangeTableScrollPane;
    @FXML private HBox rangeCardsContainer;
    @FXML private VBox noDataContainer;
    @FXML private Label noDataLabel;
    
    // Delta Builder Panel Controls
    @FXML private VBox deltaBuilderPanel;
    @FXML private Label deltaGameLabel;
    @FXML private ComboBox<String> deltaModeComboBox;
    @FXML private ComboBox<String> positionSelectorCombo;
    @FXML private VBox cachePositionsContainer;
    @FXML private Label cacheProgressLabel;
    @FXML private Button sendToGeneratorButton;
    @FXML private Button clearCacheButton;
    
    // Dependencies
    private final RangeAnalysisPresenter presenter;
    private final DeltaSelectionCacheService deltaCache;
    
    // State
    private String currentState;
    private String currentGame;
    private RangeAnalysisResponse currentResponse;
    private String selectedRange;
    private List<CheckBox> positionCheckboxes = new ArrayList<>();
    private ObservableList<DrawResultTableRow> tableData = FXCollections.observableArrayList();
    private boolean updatingCheckboxes = false;
    private boolean isProcessingAPICall = false;
    
    @Autowired
    public RangeAnalysisController(RangeAnalysisPresenter presenter,
                                   DeltaSelectionCacheService deltaCache) {
        this.presenter = presenter;
        this.deltaCache = deltaCache;
    }
    
    @PostConstruct
    public void init() {
        presenter.setView(this);
    }
    
    public void initialize() {
        logger.info("Initializing Range Analysis Controller");
        setupControls();
        setupTable();
        setupChart();
        initializePagination();
    }
    
    private void initializePagination() {
        if (rowsPerPageCombo != null) {
            rowsPerPageCombo.getItems().addAll(10, 20, 50, 100);
            rowsPerPageCombo.setValue(10);
            rowsPerPageCombo.setOnAction(e -> {
                // Handle rows per page change
                refreshAnalysisWithCurrentSettings();
            });
        }
    }
    
    public void initialize_old() {
        logger.info("Initializing Range Analysis Controller");
        setupControls();
        setupTable();
        setupChart();
    }
    
    private void setupControls() {
        // Range size field setup
        rangeSizeField.setText("5"); // Default value
        rangeSizeField.focusedProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal) { // Lost focus
                handleRangeSizeChange();
            }
        });
        
        // Analysis type ComboBox setup
        analysisTypeComboBox.getItems().addAll(AnalysisType.values());
        analysisTypeComboBox.setValue(AnalysisType.ACTUAL); // Default to ACTUAL
        analysisTypeComboBox.setOnAction(e -> handleAnalysisTypeChange());
        
        // Table data will be set in the split table setup methods
    }
    
    private void setupTable() {
        // Initialize tables with empty data
        if (fixedDateTable != null) {
            fixedDateTable.setItems(tableData);
            fixedDateTable.setPlaceholder(new Label("No data available"));
            logger.info("Fixed date table initialized");
        }

        if (scrollableRangeTable != null) {
            scrollableRangeTable.setItems(tableData);
            scrollableRangeTable.setPlaceholder(new Label("Loading..."));
            logger.info("Scrollable range table initialized");
        }

        // Tables will be dynamically populated based on API response
        // Split table setup will be handled in createTableColumns method
    }
    
    private void setupChart() {
        rangeHitChart.setTitle("Range Hit Count by Draw");
        rangeHitChart.setCreateSymbols(true);
        rangeHitChart.setLegendVisible(false);
        
        drawNumberAxis.setLabel("Draw #");
        drawNumberAxis.setLowerBound(1);
        drawNumberAxis.setTickUnit(1);
        drawNumberAxis.setMinorTickVisible(false);
        
        hitCountAxis.setLabel("Hit Count");
        hitCountAxis.setLowerBound(0);
        hitCountAxis.setTickUnit(1);
        hitCountAxis.setMinorTickVisible(false);
    }
    
    @Override
    public Mono<Void> setUpUi(String stateName, String gameName) {
        this.currentState = stateName;
        this.currentGame = gameName;

        Platform.runLater(() -> {
            stateGameLabel.setText(stateName.toUpperCase() + " - " + gameName);
            // Setup delta cache after state/game are set
            setupDeltaCache();
        });
        
        return presenter.initializeRangeAnalysis(stateName, gameName)
            .then(Mono.fromRunnable(() -> {
                // Automatically load default analysis after initialization
                Platform.runLater(() -> {
                    loadingIndicator.setVisible(true);
                    noDataContainer.setVisible(false);
                    noDataContainer.setManaged(false);
                });
                
                logger.info("Auto-loading range analysis with defaults for {}:{}", stateName, gameName);
                presenter.performDefaultAnalysis().subscribe();
            }));
    }
    
    @FXML
    private void handleRangeSizeChange() {
        String rangeSizeText = rangeSizeField.getText().trim();
        
        if (rangeSizeText.isEmpty()) {
            return;
        }
        
        try {
            int rangeSize = Integer.parseInt(rangeSizeText);
            if (rangeSize <= 0) {
                showRangeSizeError("Range size must be positive");
                return;
            }
            
            rangeSizeHint.setText("Updating...");
            rangeSizeHint.setStyle("-fx-text-fill: #2196F3;");

            // Use debounced refresh for range size changes too
            debounceRefreshAnalysis();
            
        } catch (NumberFormatException e) {
            showRangeSizeError("Invalid number format");
        }
    }
    
    @FXML
    private void handleAnalysisTypeChange() {
        AnalysisType selectedType = analysisTypeComboBox.getValue();
        if (selectedType != null) {
            logger.info("Analysis type changed to: {}", selectedType);
            refreshAnalysisWithCurrentSettings();
        }
    }
    
    @FXML
    private void handleRefresh() {
        refreshAnalysisWithCurrentSettings();
    }
    
    private void showRangeSizeError(String message) {
        rangeSizeHint.setText(message);
        rangeSizeHint.setStyle("-fx-text-fill: #f44336;");
    }
    
    private void setupPositionCheckboxes(String state, String game) {
        try {
            // Determine total positions based on game type
            int totalPositions = getGameTotalPositions(state, game);
            generatePositionCheckboxes(totalPositions);
            
            logger.info("Position checkboxes set up with {} total positions for {}:{}", totalPositions, state, game);
        } catch (Exception e) {
            logger.error("Error setting up position checkboxes for {}:{}", state, game, e);
            // Fall back to default 5 positions
            generatePositionCheckboxes(5);
        }
    }
    
    private int getGameTotalPositions(String state, String game) {
        try {
            // Use GameConfiguration to get the actual numbersDrawn for the game
            GameConfiguration config = new GameConfiguration(state, game);
            int numbersDrawn = config.getNumbersDrawn();
            
            logger.debug("Dynamic position detection for {}:{} - found {} positions", state, game, numbersDrawn);
            return numbersDrawn;
            
        } catch (Exception e) {
            logger.warn("Could not determine positions dynamically for {}:{}, falling back to static detection", state, game, e);
            
            // Fallback to static detection with improved game matching
            String normalizedGame = game.toLowerCase().replaceAll("[\\s-]", "");
            
            switch (normalizedGame) {
                case "powerball":
                case "megamillions":
                case "lotto":
                case "lottoamerica":
                case "cash4life":
                case "cashfive":
                case "cash5":
                case "take5":
                case "pick5":
                case "fantasy5":
                case "lucky4life":
                    return 5; // 5 main number positions
                case "lottotexas":
                    return 6; // Lotto Texas has 6 positions
                case "pick6":
                    return 6; // 6 number positions
                case "pick4":
                case "daily4":
                    return 4; // 4 digit positions
                case "pick3":
                case "daily3":
                    return 3; // 3 digit positions
                default:
                    logger.warn("Unknown game type: {} (normalized: {}), defaulting to 5 positions", game, normalizedGame);
                    return 5; // Default to 5 positions for unknown games
            }
        }
    }
    
    private void generatePositionCheckboxes(int numbersDrawn) {
        Platform.runLater(() -> {
            updatingCheckboxes = true;
            
            // Preserve current selection state before clearing
            Set<Integer> currentlySelected = new HashSet<>();
            for (int i = 0; i < positionCheckboxes.size(); i++) {
                if (positionCheckboxes.get(i).isSelected()) {
                    currentlySelected.add(i + 1); // Position numbers are 1-based
                }
            }
            
            positionCheckboxContainer.getChildren().clear();
            positionCheckboxes.clear();
            
            for (int i = 1; i <= numbersDrawn; i++) {
                CheckBox checkbox = new CheckBox("Position " + i);
                
                // Restore previous selection state if available, otherwise default to selected
                boolean shouldBeSelected = currentlySelected.isEmpty() ? true : currentlySelected.contains(i);
                checkbox.setSelected(shouldBeSelected);
                
                checkbox.setOnAction(e -> handlePositionSelectionChange());
                
                positionCheckboxes.add(checkbox);
                positionCheckboxContainer.getChildren().add(checkbox);
            }
            updatingCheckboxes = false;
            
            logger.debug("Regenerated {} position checkboxes, preserved selection for positions: {}", 
                        numbersDrawn, currentlySelected);
        });
    }
    
    private void handlePositionSelectionChange() {
        if (updatingCheckboxes) {
            return; // Avoid recursive calls during UI updates
        }

        List<Integer> selectedPositions = new ArrayList<>();

        for (int i = 0; i < positionCheckboxes.size(); i++) {
            if (positionCheckboxes.get(i).isSelected()) {
                selectedPositions.add(i + 1); // Position numbers start from 1
            }
        }

        if (selectedPositions.isEmpty()) {
            // Prevent deselecting all positions
            updatingCheckboxes = true;
            positionCheckboxes.get(0).setSelected(true);
            updatingCheckboxes = false;
            selectedPositions.add(1);
        }

        // Use debounced refresh to avoid API spam
        debounceRefreshAnalysis();
    }

    private void debounceRefreshAnalysis() {
        // Cancel existing timer
        if (debounceTimer != null) {
            debounceTimer.cancel();
        }

        // Create new timer for debounced execution
        debounceTimer = new Timer(true);
        debounceTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                Platform.runLater(() -> {
                    refreshAnalysisWithCurrentSettings();
                });
            }
        }, DEBOUNCE_DELAY);
    }
    
    private List<Integer> getSelectedPositions() {
        List<Integer> positions = new ArrayList<>();
        for (int i = 0; i < positionCheckboxes.size(); i++) {
            if (positionCheckboxes.get(i).isSelected()) {
                positions.add(i + 1);
            }
        }
        return positions.isEmpty() ? List.of(1) : positions; // Default to position 1 if none selected
    }
    
    private void refreshAnalysisWithCurrentSettings() {
        // Prevent recursive calls - but allow new calls after debounce period
        if (isProcessingAPICall) {
            logger.debug("API call already in progress, skipping this request");
            return;
        }
        
        // Add stack trace logging to identify call source
        StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
        logger.info("API CALL INITIATED from: {}.{}() at line {} -> {}.{}()", 
                   stackTrace[3].getClassName(), stackTrace[3].getMethodName(), stackTrace[3].getLineNumber(),
                   stackTrace[2].getClassName(), stackTrace[2].getMethodName());
        
        try {
            int rangeSize = Integer.parseInt(rangeSizeField.getText().trim());
            List<Integer> positions = getSelectedPositions();
            AnalysisType analysisType = analysisTypeComboBox.getValue() != null ? 
                                      analysisTypeComboBox.getValue() : AnalysisType.ACTUAL;
            
            logger.info("Making API call with analysisType={}, rangeSize={}, positions={}, state={}, game={}",
                       analysisType, rangeSize, positions, currentState, currentGame);

            isProcessingAPICall = true;
            showLoadingIndicator(true);
            
            // Call the correct presenter method
            Platform.runLater(() -> {
                logger.info("Calling presenter method with: analysisType={}, rangeSize={}, positions={}", 
                           analysisType, rangeSize, positions);
                
                try {
                    // First check if currentState and currentGame are set
                    if (currentState == null || currentGame == null) {
                        logger.error("Cannot make API call - currentState={}, currentGame={}", currentState, currentGame);
                        isProcessingAPICall = false;
                        showRangeSizeError("View not properly initialized");
                        return;
                    }
                    
                    // Call performAnalysis method with proper parameters
                    int maxDraws = 60; // Default value
                    presenter.performAnalysis(analysisType, rangeSize, maxDraws, positions).subscribe(
                        null, // onNext (void)
                        error -> {
                            logger.error("Error in performAnalysis: {}", error.getMessage(), error);
                            Platform.runLater(() -> {
                                isProcessingAPICall = false;
                                showDataError(error);
                            });
                        },
                        () -> {
                            logger.debug("performAnalysis completed successfully");
                        }
                    );
                    
                    logger.debug("Successfully called presenter.performAnalysis");
                } catch (Exception e) {
                    logger.error("Error calling presenter.performAnalysis: {}", e.getMessage(), e);
                    isProcessingAPICall = false;
                    showDataError(e);
                }
                
                // Reset the flag after a delay to prevent immediate successive calls
                Platform.runLater(() -> {
                    isProcessingAPICall = false;
                    logger.debug("API processing flag reset");
                });
            });
            
        } catch (NumberFormatException e) {
            isProcessingAPICall = false;
            showRangeSizeError("Invalid range size");
            logger.error("Range size parsing error: {}", e.getMessage());
        }
    }
    
    private void createTableColumns(List<String> rangeHeaders) {
        Platform.runLater(() -> {
            // Clear both tables
            fixedDateTable.getColumns().clear();
            scrollableRangeTable.getColumns().clear();
            
            // Setup fixed Date table
            setupFixedDateTable();
            
            // Setup scrollable range table  
            setupScrollableRangeTable(rangeHeaders);
            
            // Synchronize scrolling between the two tables
            synchronizeTableScrolling();
            
            // Select first range by default
            if (!rangeHeaders.isEmpty()) {
                selectRangeColumn(rangeHeaders.get(0));
            }
        });
    }
    
    private void setupFixedDateTable() {
        // Date column only
        TableColumn<DrawResultTableRow, String> dateColumn = new TableColumn<>("Date");
        dateColumn.setCellValueFactory(cellData -> cellData.getValue().drawDateProperty());
        dateColumn.setPrefWidth(100);
        dateColumn.setMinWidth(100);
        dateColumn.setMaxWidth(100);
        dateColumn.setSortable(false);
        dateColumn.setResizable(false);
        fixedDateTable.getColumns().add(dateColumn);
        
        // Disable sorting
        fixedDateTable.setSortPolicy(null);
        
        // Set the same data as the scrollable table
        fixedDateTable.setItems(tableData);
    }
    
    private void setupScrollableRangeTable(List<String> rangeHeaders) {
        // Range columns
        for (String range : rangeHeaders) {
            TableColumn<DrawResultTableRow, Number> rangeColumn = new TableColumn<>(range);
            rangeColumn.setCellValueFactory(cellData -> 
                new SimpleIntegerProperty(cellData.getValue().getHitCountForRange(range)));
            rangeColumn.setPrefWidth(80);
            rangeColumn.setMinWidth(80);
            rangeColumn.setMaxWidth(80);
            rangeColumn.setSortable(false);
            rangeColumn.setResizable(false); // Prevent column resizing
            
            // Style clickable headers
            rangeColumn.setStyle("-fx-cursor: hand;");
            
            // Add click handler for column selection using graphic property
            Label headerLabel = new Label(range);
            headerLabel.setStyle("-fx-cursor: hand; -fx-padding: 5px;");
            headerLabel.setOnMouseClicked(event -> selectRangeColumn(range));
            rangeColumn.setGraphic(headerLabel);
            rangeColumn.setText(""); // Clear text since we're using graphic
            
            scrollableRangeTable.getColumns().add(rangeColumn);
        }
        
        // Total hits column
        TableColumn<DrawResultTableRow, Number> totalColumn = new TableColumn<>("Total");
        totalColumn.setCellValueFactory(cellData -> cellData.getValue().totalHitsProperty());
        totalColumn.setPrefWidth(80);
        totalColumn.setMinWidth(80);
        totalColumn.setMaxWidth(80);
        totalColumn.setSortable(false);
        totalColumn.setResizable(false); // Prevent column resizing
        scrollableRangeTable.getColumns().add(totalColumn);
        
        // Disable sorting
        scrollableRangeTable.setSortPolicy(null);
        
        // Set the same data as the fixed table
        scrollableRangeTable.setItems(tableData);
    }
    
    private void synchronizeTableScrolling() {
        // Flag to prevent infinite scroll loops
        final boolean[] isScrolling = {false};
        
        // Synchronize vertical scrolling between the two tables
        fixedDateTable.setRowFactory(tv -> {
            TableRow<DrawResultTableRow> row = new TableRow<>();
            
            // Synchronize row selection between tables
            row.setOnMouseClicked(event -> {
                if (row.getItem() != null) {
                    int index = row.getIndex();
                    scrollableRangeTable.getSelectionModel().select(index);
                    scrollableRangeTable.scrollTo(index);
                }
            });
            
            return row;
        });
        
        scrollableRangeTable.setRowFactory(tv -> {
            TableRow<DrawResultTableRow> row = new TableRow<>();
            
            // Synchronize row selection between tables
            row.setOnMouseClicked(event -> {
                if (row.getItem() != null) {
                    int index = row.getIndex();
                    fixedDateTable.getSelectionModel().select(index);
                    fixedDateTable.scrollTo(index);
                }
            });
            
            return row;
        });
        
        // Synchronize actual scrolling using scroll events
        Platform.runLater(() -> {
            // Get the scroll panes for both tables
            javafx.scene.Node fixedDateNode = fixedDateTable.lookup(".scroll-bar:vertical");
            javafx.scene.Node rangeScrollNode = rangeTableScrollPane.lookup(".scroll-bar:vertical");
            
            if (fixedDateNode instanceof javafx.scene.control.ScrollBar && 
                rangeScrollNode instanceof javafx.scene.control.ScrollBar) {
                
                javafx.scene.control.ScrollBar fixedScrollBar = (javafx.scene.control.ScrollBar) fixedDateNode;
                javafx.scene.control.ScrollBar rangeScrollBar = (javafx.scene.control.ScrollBar) rangeScrollNode;
                
                // Sync fixed table scroll to range table
                fixedScrollBar.valueProperty().addListener((obs, oldVal, newVal) -> {
                    if (!isScrolling[0]) {
                        isScrolling[0] = true;
                        rangeScrollBar.setValue(newVal.doubleValue());
                        isScrolling[0] = false;
                    }
                });
                
                // Sync range table scroll to fixed table
                rangeScrollBar.valueProperty().addListener((obs, oldVal, newVal) -> {
                    if (!isScrolling[0]) {
                        isScrolling[0] = true;
                        fixedScrollBar.setValue(newVal.doubleValue());
                        isScrolling[0] = false;
                    }
                });
                
                logger.info("Table scroll synchronization configured successfully");
            } else {
                logger.warn("Could not find scroll bars for table synchronization");
            }
        });
    }
    
    private void selectRangeColumn(String range) {
        logger.info("CHART UPDATE: Selecting range column '{}' (previous: '{}')", range, selectedRange);
        selectedRange = range;
        chartTitle.setText("Hit Count by Draw - " + range);
        updateLineChart();
        
        // Visual feedback for selected column in scrollable table
        Platform.runLater(() -> {
            scrollableRangeTable.getColumns().forEach(col -> {
                if (col.getGraphic() instanceof Label) {
                    Label headerLabel = (Label) col.getGraphic();
                    if (headerLabel.getText().equals(range)) {
                        headerLabel.setStyle("-fx-background-color: #e3f2fd; -fx-cursor: hand; -fx-padding: 5px; -fx-border-radius: 3px; -fx-background-radius: 3px;");
                        col.setStyle("-fx-background-color: #e3f2fd;");
                    } else {
                        headerLabel.setStyle("-fx-cursor: hand; -fx-padding: 5px;");
                        col.setStyle("-fx-cursor: hand;");
                    }
                }
            });
        });
    }
    
    private void updateLineChart() {
        if (currentResponse == null || selectedRange == null) {
            logger.warn("CHART UPDATE: Cannot update chart - currentResponse={}, selectedRange='{}'", 
                       currentResponse != null ? "available" : "null", selectedRange);
            return;
        }
        
        logger.info("CHART UPDATE: Updating line chart for range '{}' with {} draw results", 
                   selectedRange, currentResponse.getDrawResults() != null ? currentResponse.getDrawResults().size() : 0);
        
        Platform.runLater(() -> {
            rangeHitChart.getData().clear();
            
            XYChart.Series<Number, Number> series = new XYChart.Series<>();
            series.setName(selectedRange);
            
            int dataPointsAdded = 0;
            int drawNumber = 1;
            
            // Add data points from draw results
            if (currentResponse.getDrawResults() != null) {
                for (DrawResult drawResult : currentResponse.getDrawResults()) {
                    Integer hitCount = drawResult.getRangeHits().get(selectedRange);
                    
                    if (hitCount != null) {
                        XYChart.Data<Number, Number> dataPoint = new XYChart.Data<>(drawNumber, hitCount);
                        series.getData().add(dataPoint);
                        dataPointsAdded++;
                        drawNumber++;
                    }
                }
            }
            
            rangeHitChart.getData().add(series);
            
            // Add labels to data points after a short delay to ensure chart is rendered
            javafx.concurrent.Task<Void> labelTask = new javafx.concurrent.Task<Void>() {
                @Override
                protected Void call() throws Exception {
                    Thread.sleep(100); // Small delay to ensure chart rendering
                    Platform.runLater(() -> addLabelsToDataPoints(series));
                    return null;
                }
            };
            new Thread(labelTask).start();
            
            logger.info("CHART UPDATE: Added {} data points to chart for range '{}'", dataPointsAdded, selectedRange);
        });
    }
    
    private void addLabelsToDataPoints(XYChart.Series<Number, Number> series) {
        Platform.runLater(() -> {
            for (XYChart.Data<Number, Number> data : series.getData()) {
                if (data.getNode() == null) { // Only add label if not already added
                    Label label = new Label(data.getYValue().toString());
                    label.setStyle("-fx-font-size: 10px; -fx-font-weight: bold; -fx-text-fill: #333333; " +
                                  "-fx-background-color: rgba(255,255,255,0.8); -fx-padding: 2px; " +
                                  "-fx-border-radius: 3px; -fx-background-radius: 3px;");
                    data.setNode(label);
                    logger.debug("Added label '{}' to data point ({}, {})", 
                               data.getYValue(), data.getXValue(), data.getYValue());
                }
            }
        });
    }
    
    private void populateTableData() {
        if (currentResponse == null) {
            return;
        }
        
        Platform.runLater(() -> {
            tableData.clear();
            
            for (DrawResult drawResult : currentResponse.getDrawResults()) {
                // Calculate dynamic total based on selected ranges only
                int dynamicTotal = calculateDynamicTotal(drawResult.getRangeHits());
                
                DrawResultTableRow row = new DrawResultTableRow(
                    drawResult.getDrawDate(),
                    drawResult.getRangeHits(),
                    dynamicTotal
                );
                tableData.add(row);
            }
        });
    }
    
    /**
     * Calculate total hits based on currently displayed ranges only
     * @param rangeHits Map of range to hit count
     * @return Sum of hits for all displayed ranges
     */
    private int calculateDynamicTotal(Map<String, Integer> rangeHits) {
        if (rangeHits == null || currentResponse == null || currentResponse.getRangeHeaders() == null) {
            return 0;
        }
        
        return currentResponse.getRangeHeaders().stream()
            .mapToInt(range -> rangeHits.getOrDefault(range, 0))
            .sum();
    }
    
    private void populatePerformanceMetrics() {
        if (currentResponse == null || currentResponse.getPerformanceMetrics() == null) {
            logger.warn("Cannot populate performance metrics - currentResponse or metrics is null");
            return;
        }

        Platform.runLater(() -> {
            if (rangeCardsContainer != null) {
                rangeCardsContainer.getChildren().clear();

                int cardCount = 0;
                for (Map.Entry<String, PerformanceMetrics> entry : currentResponse.getPerformanceMetrics().entrySet()) {
                    String range = entry.getKey();
                    PerformanceMetrics metrics = entry.getValue();

                    logger.info("Creating performance card for range: {}, status: {}, streak: {}, lastHit: {}, daysSince: {}",
                        range, metrics.getStatus(), metrics.getCurrentStreak(),
                        metrics.getLastHitDraw(), metrics.getDaysSinceLastHit());

                    VBox metricCard = createPerformanceMetricCard(range, metrics);
                    rangeCardsContainer.getChildren().add(metricCard);
                    cardCount++;
                }
                logger.info("Added {} performance metric cards to container", cardCount);
            } else {
                logger.error("rangeCardsContainer is NULL - cannot populate performance metrics");
            }
        });
    }
    
    private VBox createPerformanceMetricCard(String range, PerformanceMetrics metrics) {
        VBox card = new VBox(8);
        card.getStyleClass().add("range-card-modern");

        // Determine border color based on status
        String status = metrics.getStatus().toUpperCase();
        if (status.equals("HOT")) {
            card.getStyleClass().add("range-card-hot");
        } else if (status.equals("COLD")) {
            card.getStyleClass().add("range-card-cold");
        } else if (status.equals("OVERDUE") || status.equals("HOLD")) {
            card.getStyleClass().add("range-card-overdue");
        } else {
            card.getStyleClass().add("range-card-normal");
        }

        // Range header with status badge
        HBox header = new HBox(8);
        header.setAlignment(Pos.CENTER_LEFT);

        Label rangeLabel = new Label(range);
        rangeLabel.getStyleClass().add("range-card-label");

        // Status badge
        Label statusBadge = new Label(status);
        statusBadge.getStyleClass().add("range-card-status");
        if (status.equals("HOT")) {
            statusBadge.getStyleClass().add("range-card-status-hot");
        } else if (status.equals("COLD")) {
            statusBadge.getStyleClass().add("range-card-status-cold");
        } else {
            statusBadge.getStyleClass().add("range-card-status-normal");
        }

        header.getChildren().addAll(rangeLabel, statusBadge);

        // Metric details
        VBox details = new VBox(3);

        String streakText = metrics.getCurrentStreak() != null ?
            metrics.getCurrentStreak() : "No Streak";
        Label streakLabel = new Label(streakText);
        streakLabel.getStyleClass().add("range-card-info");

        String lastHitText = metrics.getLastHitDraw() != null ?
            "Last Hit: " + metrics.getLastHitDraw() : "No Data";
        Label lastHitLabel = new Label(lastHitText);
        lastHitLabel.getStyleClass().add("range-card-info");

        Label hitsLabel = new Label(String.format("Hit: %d",
            metrics.getDaysSinceLastHit()));
        hitsLabel.getStyleClass().add("range-card-info");

        details.getChildren().addAll(streakLabel, lastHitLabel, hitsLabel);

        card.getChildren().addAll(header, details);

        return card;
    }
    
    private String getStatusColor(String status) {
        switch (status.toUpperCase()) {
            case "HOT": return "#f44336";
            case "COLD": return "#2196f3";
            case "NORMAL": return "#4caf50";
            default: return "#757575";
        }
    }
    
    @Override
    public void showAnalysisResults(RangeAnalysisResponse response) {
        this.currentResponse = response;
        
        Platform.runLater(() -> {
            if (response == null || !response.hasData()) {
                showNoDataMessage("No analysis data available");
                return;
            }
            
            // Hide no data message
            noDataContainer.setVisible(false);
            noDataContainer.setManaged(false);
            
            // Generate position checkboxes only once
            if (positionCheckboxes.isEmpty() && response.getGameConfiguration() != null) {
                generatePositionCheckboxes(response.getGameConfiguration().getTotalNumbers());
            }
            
            // Update range size hint
            rangeSizeHint.setText("Current: " + response.getGameConfiguration().getRangeSize());
            rangeSizeHint.setStyle("-fx-text-fill: #4caf50;");
            
            // Create table columns based on range headers
            createTableColumns(response.getRangeHeaders());
            
            // Populate data
            populateTableData();
            populatePerformanceMetrics();
            
            // Update chart
            if (!response.getRangeHeaders().isEmpty()) {
                selectRangeColumn(response.getRangeHeaders().get(0));
            }
            
            showLoadingIndicator(false);
        });
    }
    
    @Override
    public void showLoadingIndicator(boolean loading) {
        Platform.runLater(() -> {
            loadingIndicator.setVisible(loading);
        });
    }
    
    @Override
    public void showDataError(Throwable error) {
        Platform.runLater(() -> {
            showNoDataMessage("Error loading data: " + error.getMessage());
            showLoadingIndicator(false);
        });
    }
    
    @Override
    public void showNoDataMessage(String message) {
        Platform.runLater(() -> {
            noDataLabel.setText(message);
            noDataContainer.setVisible(true);
            noDataContainer.setManaged(true);
            showLoadingIndicator(false);
        });
    }
    
    // View interface methods (stubs for compatibility)
    @Override
    public void setUpAnalysisControls(List<AnalysisType> analysisTypes, List<Integer> rangeSizes, 
                                    String currentState, String currentGame) {
        Platform.runLater(() -> {
            // Set default range size to 5
            rangeSizeField.setText("5");
            rangeSizeHint.setText("Using default range size");
            rangeSizeHint.setStyle("-fx-text-fill: #6c757d;");
            
            // Set default analysis type to ACTUAL
            if (analysisTypeComboBox != null && !analysisTypes.isEmpty()) {
                AnalysisType defaultType = analysisTypes.contains(AnalysisType.ACTUAL) ? 
                    AnalysisType.ACTUAL : analysisTypes.get(0);
                analysisTypeComboBox.setValue(defaultType);
            }
            
            // Setup position checkboxes for the specific game
            setupPositionCheckboxes(currentState, currentGame);
            
            logger.info("Analysis controls set up with defaults - Range Size: 5, Analysis Type: ACTUAL");
        });
    }
    
    @Override
    public void setUpRangeGrid(List<String> rangeHeaders, Map<String, PerformanceMetrics> performanceMetrics) {
        // Not used - handled by populatePerformanceMetrics()
    }
    
    @Override
    public void setUpPerformanceMetricsTable(Map<String, PerformanceMetrics> performanceMetrics) {
        // Not used - handled by populatePerformanceMetrics()
    }
    
    @Override
    public void setUpPositionAnalysisCharts(Map<String, PositionAnalysis> positionAnalysis) {
        // Not used in simplified design
    }
    
    @Override
    public void setUpDrawResultsTimeline(List<DrawResult> drawResults) {
        // Not used - handled by updateLineChart()
    }
    
    @Override
    public void updateAnalysisConfiguration(RangeAnalysisResponse.GameConfiguration config) {
        // DO NOT regenerate checkboxes based on API response!
        // The API response only contains analyzed positions, not all available positions.
        // Checkboxes should only be generated during initial setup for the game's total positions.
        logger.debug("updateAnalysisConfiguration called - skipping checkbox regeneration to preserve user selections");
    }
    
    @Override
    public void enableExportControls(boolean enabled) {
        // Not implemented in simplified design
    }
    
    @Override
    public void highlightRange(String range) {
        selectRangeColumn(range);
    }
    
    @Override
    public void clearRangeHighlights() {
        Platform.runLater(() -> {
            scrollableRangeTable.getColumns().forEach(col -> {
                if (col.getGraphic() instanceof Label) {
                    Label headerLabel = (Label) col.getGraphic();
                    headerLabel.setStyle("-fx-cursor: hand; -fx-padding: 5px;");
                    col.setStyle("-fx-cursor: hand;");
                }
            });
        });
    }
    
    @Override
    public void refreshUI() {
        logger.info("refreshUI() called - FIXED: No longer triggering recursive API calls");
        // CRITICAL FIX: Do not call refreshAnalysisWithCurrentSettings() here!
        // This was causing the infinite loop because the presenter calls refreshUI() after each successful API response
        // Just refresh the display elements without making new API calls
        Platform.runLater(() -> {
            if (currentResponse != null) {
                updateLineChart();
            }
        });
    }
    
    @Override
    public void setAnalysisInProgress(boolean inProgress) {
        showLoadingIndicator(inProgress);
    }
    
    @Override
    public void showAnalysisTypeTooltip(AnalysisType analysisType, String description) {
        // Not used in simplified design
    }

    // Additional FXML event handlers
    @FXML
    private void decreaseRangeSize() {
        try {
            int currentSize = Integer.parseInt(rangeSizeField.getText().trim());
            if (currentSize > 1) {
                rangeSizeField.setText(String.valueOf(currentSize - 1));
                handleRangeSizeChange();
            }
        } catch (NumberFormatException e) {
            logger.error("Error decreasing range size", e);
        }
    }

    @FXML
    private void increaseRangeSize() {
        try {
            int currentSize = Integer.parseInt(rangeSizeField.getText().trim());
            rangeSizeField.setText(String.valueOf(currentSize + 1));
            handleRangeSizeChange();
        } catch (NumberFormatException e) {
            logger.error("Error increasing range size", e);
        }
    }

    @FXML
    private void handleApplySettings() {
        refreshAnalysisWithCurrentSettings();
    }

    @FXML
    private void handlePreviousPage() {
        // Pagination logic - to be implemented
        logger.info("Previous page requested");
    }

    @FXML
    private void handleNextPage() {
        // Pagination logic - to be implemented
        logger.info("Next page requested");
    }
    
    // ==================== Delta Cache Methods ====================
    
    private void setupDeltaCache() {
        if (currentState != null && currentGame != null) {
            // Initialize cache for current game
            int numPositions = positionCheckboxes.size();
            deltaCache.initializeForGame(currentState, currentGame, numPositions);
            
            // Setup delta mode combo
            deltaModeComboBox.setItems(FXCollections.observableArrayList("RAW", "SORTED"));
            deltaModeComboBox.setValue("RAW");
            deltaModeComboBox.setOnAction(e -> handleDeltaModeChange());
            
            // Setup position selector
            setupPositionSelector();
            
            // Setup cache listeners
            setupCacheListeners();
            
            // Update UI
            updateDeltaCacheUI();
            
            // Set game label
            deltaGameLabel.setText(currentGame);
            
            logger.info("Delta cache setup complete for {}:{}", currentState, currentGame);
        }
    }
    
    private void setupPositionSelector() {
        List<String> positions = new ArrayList<>();
        String prefix = "RAW".equals(deltaModeComboBox.getValue()) ? "D" : "S";
        int numPositions = positionCheckboxes.size();
        
        for (int i = 1; i <= numPositions; i++) {
            positions.add(prefix + i);
        }
        
        positionSelectorCombo.setItems(FXCollections.observableArrayList(positions));
        if (!positions.isEmpty()) {
            positionSelectorCombo.setValue(positions.get(0));
        }
    }
    
    private void setupCacheListeners() {
        deltaCache.addListener(event -> Platform.runLater(this::updateDeltaCacheUI));
    }
    
    private void updateDeltaCacheUI() {
        cachePositionsContainer.getChildren().clear();
        
        deltaCache.getCache().getPositionSelections().forEach((position, values) -> {
            HBox positionRow = createPositionRow(position, values);
            cachePositionsContainer.getChildren().add(positionRow);
        });
        
        int filled = deltaCache.getCache().getFilledPositions();
        int total = positionCheckboxes.size();
        cacheProgressLabel.setText(String.format("Progress: %d/%d positions", filled, total));
        
        sendToGeneratorButton.setDisable(filled == 0);
    }
    
    private HBox createPositionRow(String position, javafx.collections.ObservableList<Integer> values) {
        HBox row = new HBox(10);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(5));
        row.setStyle("-fx-background-color: #F5F5F5; -fx-background-radius: 5px;");
        
        Label posLabel = new Label(position + ":");
        posLabel.setStyle("-fx-font-weight: bold; -fx-min-width: 40px;");
        
        String valuesText = values.stream()
            .map(String::valueOf)
            .collect(Collectors.joining(","));
        Label valuesLabel = new Label("[" + valuesText + "]");
        valuesLabel.setStyle("-fx-font-family: monospace; -fx-font-size: 11px;");
        
        Label checkmark = new Label("✓");
        checkmark.setStyle("-fx-text-fill: green; -fx-font-size: 16px;");
        
        Region spacer = new Region();
        HBox.setHgrow(spacer, javafx.scene.layout.Priority.ALWAYS);
        
        Button editBtn = new Button("✏️");
        editBtn.setStyle("-fx-font-size: 10px; -fx-padding: 2 5 2 5;");
        editBtn.setOnAction(e -> editPosition(position, values));
        
        Button deleteBtn = new Button("🗑️");
        deleteBtn.setStyle("-fx-font-size: 10px; -fx-padding: 2 5 2 5;");
        deleteBtn.setOnAction(e -> deltaCache.clearPosition(position));
        
        row.getChildren().addAll(posLabel, valuesLabel, checkmark, spacer, editBtn, deleteBtn);
        return row;
    }
    
    private void editPosition(String position, javafx.collections.ObservableList<Integer> currentValues) {
        TextInputDialog dialog = new TextInputDialog(
            currentValues.stream().map(String::valueOf).collect(Collectors.joining(","))
        );
        dialog.setTitle("Edit Position");
        dialog.setHeaderText("Edit delta values for " + position);
        dialog.setContentText("Enter comma-separated values:");
        
        dialog.showAndWait().ifPresent(input -> {
            try {
                List<Integer> newValues = java.util.Arrays.stream(input.split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .map(Integer::parseInt)
                    .collect(Collectors.toList());
                
                deltaCache.updatePosition(position, newValues);
                logger.info("Updated position {} with {} values", position, newValues.size());
            } catch (NumberFormatException e) {
                showError("Invalid input. Please enter comma-separated numbers.");
            }
        });
    }
    
    @FXML
    private void handleAddToCache() {
        if (selectedRange == null) {
            showError("Please select a range column first by clicking on a range header.");
            return;
        }
        
        String position = positionSelectorCombo.getValue();
        if (position == null) {
            showError("Please select a position from the dropdown.");
            return;
        }
        
        // Extract delta values from the selected range
        List<Integer> deltaValues = extractDeltaValuesFromRange(selectedRange);
        
        if (deltaValues.isEmpty()) {
            showError("No delta values could be extracted from range: " + selectedRange);
            return;
        }
        
        // Check if position already has values
        if (deltaCache.getCache().getPositionSelections().containsKey(position)) {
            // Ask user if they want to replace or append
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setTitle("Position Already Has Values");
            alert.setHeaderText(String.format("Position %s already contains values", position));
            alert.setContentText("Do you want to:\n" +
                "• REPLACE existing values with new range\n" +
                "• APPEND new range to existing values");
            
            ButtonType replaceButton = new ButtonType("Replace");
            ButtonType appendButton = new ButtonType("Append");
            ButtonType cancelButton = new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE);
            
            alert.getButtonTypes().setAll(replaceButton, appendButton, cancelButton);
            
            alert.showAndWait().ifPresent(response -> {
                if (response == replaceButton) {
                    // Replace existing values
                    deltaCache.updatePosition(position, deltaValues);
                    Platform.runLater(this::updateDeltaCacheUI);
                    showInfo(String.format("Replaced %s with %d values from range %s", 
                        position, deltaValues.size(), selectedRange));
                    logger.info("Replaced position {} with range {} ({} values)", 
                        position, selectedRange, deltaValues.size());
                } else if (response == appendButton) {
                    // Append to existing values
                    deltaCache.appendToPosition(position, deltaValues);
                    Platform.runLater(this::updateDeltaCacheUI);
                    int totalValues = deltaCache.getCache().getPositionSelections()
                        .get(position).size();
                    showInfo(String.format("Added %d values to %s (total: %d values)", 
                        deltaValues.size(), position, totalValues));
                    logger.info("Appended range {} to position {} ({} new values, {} total)", 
                        selectedRange, position, deltaValues.size(), totalValues);
                }
            });
        } else {
            // Position is empty, just add the values
            deltaCache.updatePosition(position, deltaValues);
            Platform.runLater(this::updateDeltaCacheUI);
            showInfo(String.format("Added %d delta values to %s from range %s", 
                deltaValues.size(), position, selectedRange));
            logger.info("Added range {} to position {} with {} values", 
                selectedRange, position, deltaValues.size());
        }
    }
    
    private List<Integer> extractDeltaValuesFromRange(String range) {
        try {
            // Parse range like "4-6" and extract all values in that range
            String[] parts = range.split("-");
            int start = Integer.parseInt(parts[0].trim());
            int end = Integer.parseInt(parts[1].trim());
            
            List<Integer> deltas = new ArrayList<>();
            // Extract all values from start to end (inclusive)
            for (int i = start; i <= end; i++) {
                deltas.add(i);
            }
            
            logger.debug("Extracted {} delta values from range {} (values: {})", 
                deltas.size(), range, deltas);
            return deltas;
        } catch (Exception e) {
            logger.error("Error extracting delta values from range: {}", range, e);
            return new ArrayList<>();
        }
    }
    
    @FXML
    private void handleSendToGenerator() {
        // This will be handled by navigation - for now just show info
        showInfo("Opening Delta Pick Generator with cached selections...\n" +
                "Navigate to Delta Pick Generator from the main menu.");
        logger.info("Send to generator requested with {} positions filled", 
            deltaCache.getCache().getFilledPositions());
    }
    
    @FXML
    private void handleClearCache() {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Clear Cache");
        confirm.setHeaderText("Clear all delta selections?");
        confirm.setContentText("This will remove all cached delta values for all positions.");
        
        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                deltaCache.clearAll();
                logger.info("Cache cleared by user");
            }
        });
    }
    
    private void handleDeltaModeChange() {
        String newMode = deltaModeComboBox.getValue();
        DeltaInputMode mode = "RAW".equals(newMode) ? DeltaInputMode.RAW : DeltaInputMode.SORTED;
        deltaCache.setMode(mode);
        setupPositionSelector();
        logger.info("Delta mode changed to: {}", newMode);
    }
    
    private void showInfo(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Information");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
    
    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}