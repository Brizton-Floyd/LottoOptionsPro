package com.example.lottooptionspro.controller;

import com.example.lottooptionspro.GameInformation;
import com.example.lottooptionspro.model.cache.DeltaSelectionCache;
import com.example.lottooptionspro.model.deltapick.*;
import com.example.lottooptionspro.presenter.DeltaPickGeneratorPresenter;
import com.example.lottooptionspro.presenter.DeltaPickGeneratorView;
import com.example.lottooptionspro.service.BetslipGenerationService;
import com.example.lottooptionspro.service.DeltaSelectionCacheService;
import com.example.lottooptionspro.util.DeltaPickDisplayUtil;
import jakarta.annotation.PostConstruct;
import javafx.application.Platform;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.geometry.Pos;
import javafx.stage.Modality;
import javafx.stage.Stage;
import net.rgielen.fxweaver.core.FxControllerAndView;
import net.rgielen.fxweaver.core.FxWeaver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import net.rgielen.fxweaver.core.FxmlView;
import reactor.core.publisher.Mono;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Controller for the Delta Pick Generator view.
 * Implements dynamic form generation based on game configuration.
 */
@Controller
@FxmlView("/com.example.lottooptionspro/controller/DeltaPickGeneratorView.fxml")
public class DeltaPickGeneratorController implements GameInformation, DeltaPickGeneratorView {
    
    private static final Logger logger = LoggerFactory.getLogger(DeltaPickGeneratorController.class);
    
    // FXML Controls - Top Section
    @FXML private Label stateGameLabel;
    @FXML private Label lastUpdatedLabel;
    @FXML private Label modeDescriptionLabel;
    
    // FXML Controls - Configuration
    @FXML private ComboBox<String> deltaModeComboBox;
    @FXML private TextField numCombinationsField;
    @FXML private GridPane deltaInputsGrid;
    @FXML private Button generatePicksButton;
    
    // FXML Controls - Table
    @FXML private TableView<GeneratedPick> generatedPicksTable;
    @FXML private TableColumn<GeneratedPick, Integer> rankColumn;
    @FXML private TableColumn<GeneratedPick, String> numbersColumn;
    @FXML private TableColumn<GeneratedPick, String> rawDeltasColumn;
    @FXML private TableColumn<GeneratedPick, String> sortedDeltasColumn;
    @FXML private TableColumn<GeneratedPick, String> probabilityColumn;
    
    // FXML Controls - Action Buttons
    @FXML private Button regeneratePicksButton;
    @FXML private Button generateBetslipsButton;
    @FXML private TextField picksToIncludeCountField;
    
    // FXML Controls - Cache Indicator
    @FXML private HBox cacheIndicatorContainer;
    @FXML private Label cacheStatusLabel;
    @FXML private Button loadFromCacheButton;
    @FXML private Button saveToCacheButton;
    @FXML private Button clearCacheButton;
    
    // FXML Controls - Summary
    @FXML private Label configGameLabel;
    @FXML private Label configMaxNumberLabel;
    @FXML private Label configNumPicksLabel;
    @FXML private Label configModeLabel;
    @FXML private Label totalCombinationsLabel;
    @FXML private Label executionTimeLabel;
    @FXML private Label searchSpaceLabel;
    @FXML private Label generationStrategyLabel;
    @FXML private Label totalWinsLabel;
    @FXML private Label match5Label;
    @FXML private Label match4Label;
    @FXML private Label match3Label;
    @FXML private Label performanceFactorLabel;
    @FXML private VBox insightsContainer;
    
    // FXML Controls - Loading
    @FXML private StackPane loadingOverlay;
    @FXML private ProgressIndicator loadingIndicator;
    
    // Dependencies
    private final DeltaPickGeneratorPresenter presenter;
    private final BetslipGenerationService betslipService;
    private final FxWeaver fxWeaver;
    private final DeltaSelectionCacheService deltaCache;
    
    // State
    private String currentState;
    private String currentGame;
    private Integer maxNumber;
    private Integer numPicks;
    private DeltaPickGenerationRequest lastSuccessfulRequest;
    private List<GeneratedPick> generatedPicks = new ArrayList<>();
    private Map<String, TextField> deltaInputFields = new HashMap<>();
    
    @Autowired
    public DeltaPickGeneratorController(DeltaPickGeneratorPresenter presenter,
                                       BetslipGenerationService betslipService,
                                       FxWeaver fxWeaver,
                                       DeltaSelectionCacheService deltaCache) {
        this.presenter = presenter;
        this.betslipService = betslipService;
        this.fxWeaver = fxWeaver;
        this.deltaCache = deltaCache;
    }
    
    @PostConstruct
    public void init() {
        presenter.setView(this);
    }
    
    @Override
    public Mono<Void> setUpUi(String stateName, String gameName) {
        this.currentState = stateName;
        this.currentGame = gameName;
        
        return presenter.loadGameConfiguration(stateName, gameName)
            .doOnSuccess(config -> Platform.runLater(() -> {
                this.maxNumber = config.getMaxNumber();
                this.numPicks = config.getDrawPositionCount();
                initializeUI();
                updateConfigurationPanel(config);
                
                // Check for cached delta selections
                checkAndLoadCache();
            }))
            .doOnError(error -> Platform.runLater(() -> {
                showError("Failed to load game configuration: " + error.getMessage());
            }))
            .then();
    }
    
    private void initializeUI() {
        // Setup state/game label
        stateGameLabel.setText(currentState.toUpperCase() + " - " + currentGame);
        
        // Setup delta mode combo box
        deltaModeComboBox.setItems(FXCollections.observableArrayList("RAW", "SORTED"));
        deltaModeComboBox.setValue("RAW");
        deltaModeComboBox.setOnAction(e -> {
            updateModeDescription();
            regenerateDeltaInputFields();
        });
        
        // Setup table columns
        setupTableColumns();
        
        // Generate initial delta input fields
        regenerateDeltaInputFields();
        updateModeDescription();
        
        // Set default number of combinations
        numCombinationsField.setText("20");
        
        logger.info("UI initialized for {}:{} with maxNumber={}, numPicks={}", 
            currentState, currentGame, maxNumber, numPicks);
    }
    
    private void updateModeDescription() {
        String mode = deltaModeComboBox.getValue();
        if ("RAW".equals(mode)) {
            modeDescriptionLabel.setText("RAW mode: Deltas are used in sequential order (D1, D2, D3...)");
        } else {
            modeDescriptionLabel.setText("SORTED mode: Deltas are sorted by magnitude before generation");
        }
    }
    
    private void regenerateDeltaInputFields() {
        if (numPicks == null || numPicks <= 0) {
            logger.warn("Cannot generate delta input fields: numPicks is {}", numPicks);
            return;
        }
        
        deltaInputsGrid.getChildren().clear();
        deltaInputFields.clear();
        
        String mode = deltaModeComboBox.getValue();
        String prefix = "RAW".equals(mode) ? "D" : "S";
        
        int row = 0;
        int col = 0;
        int maxCols = 3; // 3 columns layout
        
        for (int i = 1; i <= numPicks; i++) {
            String fieldKey = prefix + i;
            
            Label label = new Label(fieldKey + ":");
            label.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");
            
            TextField textField = new TextField();
            textField.setPromptText("e.g., 3,7,13");
            textField.setPrefWidth(150);
            textField.setStyle("-fx-font-size: 13px;");
            
            deltaInputFields.put(fieldKey, textField);
            
            deltaInputsGrid.add(label, col * 2, row);
            deltaInputsGrid.add(textField, col * 2 + 1, row);
            
            col++;
            if (col >= maxCols) {
                col = 0;
                row++;
            }
        }
        
        // Force layout update
        deltaInputsGrid.layout();
        
        logger.info("Generated {} delta input fields in {} mode ({})", numPicks, mode, deltaInputFields.size());
    }
    
    private void setupTableColumns() {
        // Rank column
        rankColumn.setCellValueFactory(cellData -> 
            new SimpleIntegerProperty(cellData.getValue().getRank()).asObject());
        rankColumn.setSortType(TableColumn.SortType.ASCENDING);
        
        // Numbers column with hyphenated display
        numbersColumn.setCellValueFactory(cellData -> 
            new SimpleStringProperty(DeltaPickDisplayUtil.formatNumbersArray(cellData.getValue().getNumbers())));
        
        // Raw deltas column
        rawDeltasColumn.setCellValueFactory(cellData -> 
            new SimpleStringProperty(DeltaPickDisplayUtil.formatNumbersArray(cellData.getValue().getRawDeltas())));
        
        // Sorted deltas column
        sortedDeltasColumn.setCellValueFactory(cellData -> 
            new SimpleStringProperty(DeltaPickDisplayUtil.formatNumbersArray(cellData.getValue().getSortedDeltas())));
        
        // Probability column
        probabilityColumn.setCellValueFactory(cellData -> 
            new SimpleStringProperty(DeltaPickDisplayUtil.formatProbabilityScientific(cellData.getValue().getProbabilityScore())));
        
        // Make table sortable
        generatedPicksTable.setSortPolicy(tv -> {
            FXCollections.sort(tv.getItems(), tv.getComparator());
            return true;
        });
        
        // Default sort by rank
        generatedPicksTable.getSortOrder().add(rankColumn);
    }
    
    @FXML
    private void handleGeneratePicks() {
        try {
            // Validate inputs
            if (!validateInputs()) {
                return;
            }
            
            // Build request
            DeltaPickGenerationRequest request = buildRequest();
            
            // Show loading
            showLoading(true);
            lastUpdatedLabel.setText("Generating picks...");
            
            // Call presenter
            presenter.generatePicks(request)
                .doOnSuccess(response -> Platform.runLater(() -> {
                    displayGeneratedPicks(response);
                    lastSuccessfulRequest = request;
                    regeneratePicksButton.setDisable(false);
                    lastUpdatedLabel.setText("Last generated: Just now");
                }))
                .doOnError(error -> Platform.runLater(() -> {
                    showError("Failed to generate picks: " + error.getMessage());
                    lastUpdatedLabel.setText("Generation failed");
                }))
                .doFinally(signalType -> Platform.runLater(() -> showLoading(false)))
                .subscribe();
                
        } catch (Exception e) {
            showError("Error: " + e.getMessage());
            showLoading(false);
        }
    }
    
    @FXML
    private void handleRegeneratePicks() {
        if (lastSuccessfulRequest != null) {
            showLoading(true);
            lastUpdatedLabel.setText("Regenerating picks...");
            
            presenter.generatePicks(lastSuccessfulRequest)
                .doOnSuccess(response -> Platform.runLater(() -> {
                    displayGeneratedPicks(response);
                    lastUpdatedLabel.setText("Last regenerated: Just now");
                }))
                .doOnError(error -> Platform.runLater(() -> {
                    showError("Failed to regenerate picks: " + error.getMessage());
                }))
                .doFinally(signalType -> Platform.runLater(() -> showLoading(false)))
                .subscribe();
        }
    }
    
    @FXML
    private void handleGenerateBetslips() {
        try {
            String countText = picksToIncludeCountField.getText();
            if (countText == null || countText.trim().isEmpty()) {
                showError("Please enter the number of picks to include");
                return;
            }
            
            int count = Integer.parseInt(countText.trim());
            
            if (count <= 0 || count > generatedPicks.size()) {
                showError(String.format("Please enter a number between 1 and %d", generatedPicks.size()));
                return;
            }
            
            // Extract top N picks
            List<int[]> numberSets = generatedPicks.stream()
                .limit(count)
                .map(pick -> pick.getNumbers().stream().mapToInt(Integer::intValue).toArray())
                .collect(Collectors.toList());
            
            showLoading(true);
            lastUpdatedLabel.setText("Generating betslips...");
            
            betslipService.generatePdf(numberSets, currentState, currentGame)
                .doOnSuccess(result -> Platform.runLater(() -> {
                    showPreviewDialog(result);
                    lastUpdatedLabel.setText("Betslips preview shown");
                }))
                .doOnError(error -> Platform.runLater(() -> {
                    showError("Failed to generate betslips: " + error.getMessage());
                }))
                .doFinally(signalType -> Platform.runLater(() -> showLoading(false)))
                .subscribe();
                
        } catch (NumberFormatException e) {
            showError("Please enter a valid number");
        }
    }
    
    private void showPreviewDialog(BetslipGenerationService.PdfGenerationResult result) {
        Platform.runLater(() -> {
            try {
                FxControllerAndView<PdfPreviewController, Parent> controllerAndView = fxWeaver.load(PdfPreviewController.class);
                PdfPreviewController controller = controllerAndView.getController();
                controller.presenter.setData(result.images, result.template);

                Stage dialogStage = new Stage();
                dialogStage.initModality(Modality.WINDOW_MODAL);
                dialogStage.initOwner(stateGameLabel.getScene().getWindow());
                dialogStage.setTitle("Betslip Preview");

                Scene scene = new Scene(controllerAndView.getView().get());
                dialogStage.setScene(scene);
                dialogStage.showAndWait();
                
                logger.info("Betslip preview dialog shown");
            } catch (Exception e) {
                logger.error("Error showing preview dialog", e);
                showError("Error showing preview: " + e.getMessage());
            }
        });
    }
    
    private boolean validateInputs() {
        // Validate number of combinations
        String numCombText = numCombinationsField.getText();
        if (numCombText == null || numCombText.trim().isEmpty()) {
            showError("Please enter the number of combinations to generate");
            return false;
        }
        
        try {
            int numComb = Integer.parseInt(numCombText.trim());
            if (numComb <= 0) {
                showError("Number of combinations must be positive");
                return false;
            }
        } catch (NumberFormatException e) {
            showError("Please enter a valid number for combinations");
            return false;
        }
        
        // Validate at least one delta input
        boolean hasInput = false;
        for (TextField field : deltaInputFields.values()) {
            if (field.getText() != null && !field.getText().trim().isEmpty()) {
                hasInput = true;
                break;
            }
        }
        
        if (!hasInput) {
            showError("Please enter at least one delta value");
            return false;
        }
        
        return true;
    }
    
    private DeltaPickGenerationRequest buildRequest() {
        DeltaPickGenerationRequest request = new DeltaPickGenerationRequest();
        request.setLotteryState(currentState);
        request.setLotteryGame(currentGame);
        request.setDeltaInputMode(deltaModeComboBox.getValue());
        request.setNumCombinations(Integer.parseInt(numCombinationsField.getText().trim()));
        request.setMaxNumber(maxNumber);
        request.setNumPicks(numPicks);
        
        Map<String, List<Integer>> deltaMap = new HashMap<>();
        
        for (Map.Entry<String, TextField> entry : deltaInputFields.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue().getText();
            
            if (value != null && !value.trim().isEmpty()) {
                List<Integer> values = Arrays.stream(value.split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .map(Integer::parseInt)
                    .collect(Collectors.toList());
                
                if (!values.isEmpty()) {
                    deltaMap.put(key, values);
                }
            }
        }
        
        if ("RAW".equals(request.getDeltaInputMode())) {
            request.setRawDeltas(deltaMap);
        } else {
            request.setSortedDeltaMagnitudes(deltaMap);
        }
        
        return request;
    }
    
    @Override
    public void displayGeneratedPicks(DeltaPickGenerationResponse response) {
        this.generatedPicks = response.getGeneratedPicks();
        
        ObservableList<GeneratedPick> picks = FXCollections.observableArrayList(generatedPicks);
        generatedPicksTable.setItems(picks);
        generatedPicksTable.sort();
        
        // Enable action buttons
        regeneratePicksButton.setDisable(false);
        generateBetslipsButton.setDisable(false);
        picksToIncludeCountField.setText(String.valueOf(Math.min(10, generatedPicks.size())));
        
        // Update summary panels
        updateSummaryPanels(response);
        
        logger.info("Displayed {} generated picks", generatedPicks.size());
    }
    
    private void updateSummaryPanels(DeltaPickGenerationResponse response) {
        try {
            // Configuration
            if (response.getConfiguration() != null) {
                Configuration config = response.getConfiguration();
                configGameLabel.setText("Game: " + (config.getLotteryGame() != null ? config.getLotteryGame() : "N/A"));
                configMaxNumberLabel.setText("Max Number: " + (config.getMaxNumber() != null ? config.getMaxNumber() : "N/A"));
                configNumPicksLabel.setText("Numbers per Pick: " + (config.getNumPicks() != null ? config.getNumPicks() : "N/A"));
                configModeLabel.setText("Mode: " + (config.getDeltaInputMode() != null ? config.getDeltaInputMode() : "N/A"));
                logger.debug("Configuration panel updated");
            }
            
            // Analysis Summary
            totalCombinationsLabel.setText("Total Valid: " + 
                DeltaPickDisplayUtil.formatNumberWithCommas(response.getTotalValidCombinations()));
            executionTimeLabel.setText("Execution Time: " + 
                DeltaPickDisplayUtil.formatExecutionTime(response.getExecutionTimeMs()));
            
            if (response.getMetadata() != null) {
                searchSpaceLabel.setText("Search Space: " + 
                    DeltaPickDisplayUtil.formatNumberWithCommas(response.getMetadata().getSearchSpaceExplored()));
                generationStrategyLabel.setText("Strategy: " + 
                    (response.getMetadata().getGenerationStrategy() != null ? response.getMetadata().getGenerationStrategy() : "N/A"));
                logger.debug("Metadata panel updated");
            }
        
        // Historical Performance
        if (response.getHistoricalPerformance() != null) {
            HistoricalPerformance perf = response.getHistoricalPerformance();
            
            if (perf.getWinSummary() != null) {
                totalWinsLabel.setText("Total Wins: " + 
                    DeltaPickDisplayUtil.formatNumberWithCommas(perf.getWinSummary().getTotalWins()));
            }
            
            if (perf.getPrizeBreakdown() != null) {
                PrizeBreakdown prizes = perf.getPrizeBreakdown();
                if (prizes.getMatch5() != null) {
                    match5Label.setText(String.format("Match 5: %d wins (%.2f/draw)", 
                        prizes.getMatch5().getWins(), prizes.getMatch5().getFrequency()));
                }
                if (prizes.getMatch4() != null) {
                    match4Label.setText(String.format("Match 4: %d wins (%.2f/draw)", 
                        prizes.getMatch4().getWins(), prizes.getMatch4().getFrequency()));
                }
                if (prizes.getMatch3() != null) {
                    match3Label.setText(String.format("Match 3: %d wins (%.2f/draw)", 
                        prizes.getMatch3().getWins(), prizes.getMatch3().getFrequency()));
                }
            }
            
            if (perf.getComparison() != null && perf.getComparison().getVsRandomTickets() != null) {
                ComparisonMetric metric = perf.getComparison().getVsRandomTickets();
                performanceFactorLabel.setText(String.format("Performance: %.2fx (%.1f percentile)", 
                    metric.getPerformanceFactor(), metric.getPercentile()));
            }
            
            // Insights
            if (perf.getInsights() != null && !perf.getInsights().isEmpty()) {
                insightsContainer.getChildren().clear();
                for (String insight : perf.getInsights()) {
                    Label insightLabel = new Label("• " + insight);
                    insightLabel.setWrapText(true);
                    insightLabel.getStyleClass().add("insight-item");
                    insightsContainer.getChildren().add(insightLabel);
                }
                logger.debug("Added {} insights", perf.getInsights().size());
            }
            
            logger.debug("Historical performance panel updated");
        }
        
        logger.info("All summary panels updated successfully");
        
        } catch (Exception e) {
            logger.error("Error updating summary panels", e);
            showError("Error updating summary panels: " + e.getMessage());
        }
    }
    
    @Override
    public void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
        logger.error("Error shown to user: {}", message);
    }
    
    private void showInfo(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Success");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
        logger.info("Info shown to user: {}", message);
    }
    
    @Override
    public void showLoading(boolean show) {
        loadingOverlay.setVisible(show);
        loadingOverlay.setManaged(show);
        generatePicksButton.setDisable(show);
    }
    
    @Override
    public void updateConfigurationPanel(GameConfigResponse config) {
        // Already handled in updateSummaryPanels
    }
    
    // ==================== Delta Cache Methods ====================
    
    private void checkAndLoadCache() {
        if (deltaCache.hasCacheFor(currentState, currentGame)) {
            long ageMinutes = deltaCache.getCacheAgeMinutes();
            
            if (deltaCache.isCacheFresh()) {
                // Show cache indicator
                cacheStatusLabel.setText(String.format("🔄 Cache available (%d min ago)", ageMinutes));
                cacheIndicatorContainer.setVisible(true);
                cacheIndicatorContainer.setManaged(true);
                loadFromCacheButton.setDisable(false);
                
                // Auto-load prompt
                showCacheLoadPrompt();
            } else {
                cacheStatusLabel.setText("⚠️ Cache is stale (> 30 min)");
                cacheIndicatorContainer.setVisible(true);
                cacheIndicatorContainer.setManaged(true);
            }
        } else {
            cacheIndicatorContainer.setVisible(false);
            cacheIndicatorContainer.setManaged(false);
        }
    }
    
    private void showCacheLoadPrompt() {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Load Cached Selections");
        alert.setHeaderText("Delta selections found in cache");
        alert.setContentText(String.format(
            "Load %d cached delta selections for %s - %s?",
            deltaCache.getCache().getFilledPositions(),
            currentState,
            currentGame
        ));
        
        alert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                loadFromCache();
            }
        });
    }
    
    @FXML
    private void handleLoadFromCache() {
        loadFromCache();
    }
    
    private void loadFromCache() {
        DeltaSelectionCache cache = deltaCache.getCache();
        
        // Set mode
        String mode = cache.getMode().toString();
        deltaModeComboBox.setValue(mode);
        
        // Populate fields
        cache.getPositionSelections().forEach((position, values) -> {
            TextField field = deltaInputFields.get(position);
            if (field != null) {
                String valuesText = values.stream()
                    .map(String::valueOf)
                    .collect(Collectors.joining(","));
                field.setText(valuesText);
                
                // Add visual indicator (green border for cached)
                field.setStyle("-fx-border-color: #4CAF50; -fx-border-width: 2px;");
            }
        });
        
        logger.info("Loaded {} positions from cache", cache.getFilledPositions());
        showInfo("Loaded delta selections from cache");
    }
    
    @FXML
    private void handleSaveToCache() {
        // Extract current values from fields
        String mode = deltaModeComboBox.getValue();
        deltaCache.setMode(DeltaInputMode.valueOf(mode));
        
        int savedCount = 0;
        deltaInputFields.forEach((position, field) -> {
            String text = field.getText();
            if (text != null && !text.trim().isEmpty()) {
                try {
                    List<Integer> values = Arrays.stream(text.split(","))
                        .map(String::trim)
                        .filter(s -> !s.isEmpty())
                        .map(Integer::parseInt)
                        .collect(Collectors.toList());
                    
                    deltaCache.updatePosition(position, values);
                } catch (NumberFormatException e) {
                    logger.warn("Invalid values in position {}: {}", position, text);
                }
            }
        });
        
        showInfo("Saved delta selections to cache");
        logger.info("Saved delta selections to cache");
        
        // Update cache indicator
        checkAndLoadCache();
    }
    
    @FXML
    private void handleClearCacheFromGenerator() {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Clear Cache");
        confirm.setHeaderText("Clear all delta selections?");
        confirm.setContentText("This will remove all cached delta values.");
        
        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                deltaCache.clearAll();
                cacheIndicatorContainer.setVisible(false);
                cacheIndicatorContainer.setManaged(false);
                showInfo("Cache cleared");
                logger.info("Cache cleared from Delta Generator");
            }
        });
    }
}
