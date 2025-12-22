package com.example.lottooptionspro.controller;

import com.example.lottooptionspro.model.range.GameConfiguration;
import com.example.lottooptionspro.model.strategyengine.*;
import com.example.lottooptionspro.model.wheel.GuaranteeLevel;
import com.example.lottooptionspro.model.wheel.WheelConfiguration;
import com.example.lottooptionspro.model.wheel.WheelGenerationResponse;
import com.example.lottooptionspro.presenter.StrategyEnginePresenter;
import com.example.lottooptionspro.service.AbbreviatedWheelService;
import com.example.lottooptionspro.service.BetslipGenerationService;
import com.example.lottooptionspro.service.PatternBasedWheelGenerator;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Stage;
import net.rgielen.fxweaver.core.FxControllerAndView;
import net.rgielen.fxweaver.core.FxWeaver;
import net.rgielen.fxweaver.core.FxmlView;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Component
@FxmlView("/com.example.lottooptionspro/controller/StrategyEngineView.fxml")
public class StrategyEngineController implements StrategyEnginePresenter.StrategyEngineView, ContextAware {

    private static final Logger logger = LoggerFactory.getLogger(StrategyEngineController.class);

    @FXML private AnchorPane rootContainer;
    @FXML private Label stateGameLabel;
    @FXML private Label lastUpdatedLabel;
    
    @FXML private ComboBox<Integer> poolSizeCombo;
    @FXML private TextField setCountField;
    @FXML private ToggleButton aggressiveButton;
    @FXML private ToggleButton balancedButton;
    @FXML private ToggleButton conservativeButton;
    @FXML private Button generateButton;
    
    @FXML private HBox engineConstantsPanel;
    @FXML private Label averageSkipLabel;
    @FXML private Label longShotThresholdLabel;
    @FXML private Label coldRuleLimitLabel;
    @FXML private Label drawsAnalyzedLabel;
    @FXML private Label hitRateLabel;
    @FXML private Label avgCoverageLabel;
    
    @FXML private TabPane resultsTabPane;
    @FXML private FlowPane tier1AnchorsPane;
    @FXML private VBox patternsPane;
    @FXML private VBox generatedSetsPane;
    @FXML private TableView<ExclusionReport> exclusionTable;
    @FXML private TableColumn<ExclusionReport, Integer> excludedNumberColumn;
    @FXML private TableColumn<ExclusionReport, String> reasonColumn;
    @FXML private TableColumn<ExclusionReport, Integer> currentSkipColumn;
    @FXML private TableColumn<ExclusionReport, Integer> limitColumn;
    
    @FXML private ListView<String> setsToWheelList;
    @FXML private ComboBox<GuaranteeLevel> guaranteeLevelCombo;
    @FXML private Label estimatedLinesLabel;
    @FXML private Button generateWheelButton;
    @FXML private VBox wheeledCombinationsPane;
    @FXML private Button generateBetslipsButton;
    
    @FXML private StackPane loadingPane;
    @FXML private ProgressIndicator progressIndicator;

    private final StrategyEnginePresenter presenter;
    private final AbbreviatedWheelService wheelService;
    private final BetslipGenerationService betslipService;
    private final PatternBasedWheelGenerator patternWheelGenerator;
    private final FxWeaver fxWeaver;
    
    private ToggleGroup strategyBiasGroup;
    private String currentState;
    private String currentGame;
    private int currentPickSize = 6;
    private StrategyEngineResponse currentResponse;
    private List<int[]> currentWheeledCombinations;

    public StrategyEngineController(StrategyEnginePresenter presenter, 
                                   AbbreviatedWheelService wheelService,
                                   BetslipGenerationService betslipService,
                                   PatternBasedWheelGenerator patternWheelGenerator,
                                   FxWeaver fxWeaver) {
        this.presenter = presenter;
        this.wheelService = wheelService;
        this.betslipService = betslipService;
        this.patternWheelGenerator = patternWheelGenerator;
        this.fxWeaver = fxWeaver;
    }

    @FXML
    public void initialize() {
        presenter.setView(this);
        
        strategyBiasGroup = new ToggleGroup();
        aggressiveButton.setToggleGroup(strategyBiasGroup);
        balancedButton.setToggleGroup(strategyBiasGroup);
        conservativeButton.setToggleGroup(strategyBiasGroup);
        aggressiveButton.setSelected(true);
        
        poolSizeCombo.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                updateGuaranteesForPoolSize(newVal);
            }
        });
        
        setupExclusionTable();
        setupWheelListeners();
        
        setCountField.setText("4");
        
        guaranteeLevelCombo.setDisable(true);
        generateWheelButton.setDisable(true);
    }

    @Override
    public void initializeWithContext(String stateName, String gameName) {
        GameConfiguration gameConfig = new GameConfiguration(stateName, gameName);
        int pickSize = gameConfig.getNumbersDrawn();
        setGameContext(stateName, gameName, pickSize);
        logger.info("Loaded game configuration: {} - {} with pick size {} (bonus numbers: {})", 
                   stateName, gameName, pickSize, gameConfig.getBonusNumbers());
    }

    public void setGameContext(String state, String game, int pickSize) {
        this.currentState = state;
        this.currentGame = game;
        this.currentPickSize = pickSize;
        stateGameLabel.setText(state.toUpperCase() + " - " + game);
        
        refreshGuaranteeLevels();
        
        logger.info("Game context set: {} - {} (Pick-{})", state, game, pickSize);
    }

    @FXML
    private void handleGenerate() {
        try {
            Integer poolSize = poolSizeCombo.getValue();
            if (poolSize == null) {
                showError("Please select a pool size");
                return;
            }
            int setCount = Integer.parseInt(setCountField.getText().trim());
            
            if (poolSize < 4 || poolSize > 27) {
                showError("Pool size must be between 4 and 27");
                return;
            }
            
            if (setCount < 1 || setCount > 10) {
                showError("Set count must be between 1 and 10");
                return;
            }
            
            String strategyBias = getSelectedStrategyBias();
            
            showLoading(true);
            lastUpdatedLabel.setText("Generating strategy...");
            
            presenter.generateStrategy(currentGame, currentState, poolSize, setCount, strategyBias)
                .doOnSuccess(response -> Platform.runLater(() -> {
                    currentResponse = response;
                    displayResults(response);
                    lastUpdatedLabel.setText("Generated: Just now");
                }))
                .doOnError(error -> Platform.runLater(() -> {
                    showError("Failed to generate strategy: " + error.getMessage());
                    logger.error("Strategy generation failed", error);
                }))
                .doFinally(signalType -> Platform.runLater(() -> showLoading(false)))
                .subscribe();
                
        } catch (NumberFormatException e) {
            showError("Please enter valid numbers for pool size and set count");
        }
    }

    private void displayResults(StrategyEngineResponse response) {
        if (currentState != null && currentGame != null) {
            GameConfiguration gameConfig = new GameConfiguration(currentState, currentGame);
            int actualPickSize = gameConfig.getNumbersDrawn();
            if (actualPickSize != currentPickSize) {
                logger.warn("Pick size mismatch detected. Current: {}, Actual from GameConfiguration: {}. Updating guarantee levels.",
                           currentPickSize, actualPickSize);
                currentPickSize = actualPickSize;
                refreshGuaranteeLevels();
            }
        }
        
        displayEngineConstants(
            response.getEngineConstants().getAverageSkip(),
            response.getEngineConstants().getLongShotThreshold(),
            response.getEngineConstants().getColdRuleLimit()
        );
        
        displayHistoricalContext(
            response.getHistoricalPerformance().getDrawsAnalyzed(),
            response.getHistoricalPerformance().getPerformanceMetrics().getHitRate(),
            response.getHistoricalPerformance().getPerformanceMetrics().getAverageCoverage()
        );
        
        displayTier1Anchors(
            response.getTier1Anchors().getNumbers(),
            response.getTier1Anchors().getPatterns()
        );
        
        displayGeneratedSets(response.getGeneratedSets());
        displayExclusionReport(response.getExclusionReport());
        
        engineConstantsPanel.setVisible(true);
        engineConstantsPanel.setManaged(true);
        resultsTabPane.setVisible(true);
        resultsTabPane.setManaged(true);
        
        populateSetsToWheelList(response.getGeneratedSets());
    }
    
    private void refreshGuaranteeLevels() {
        List<Integer> availablePoolSizes = patternWheelGenerator.getAvailablePoolSizes(currentPickSize);
        poolSizeCombo.getItems().clear();
        poolSizeCombo.getItems().setAll(availablePoolSizes);
        
        if (!availablePoolSizes.isEmpty()) {
            poolSizeCombo.getSelectionModel().select(availablePoolSizes.size() > 5 ? 5 : 0);
        }
        
        logger.info("Refreshed pool sizes for Pick-{}: {} options available", 
                   currentPickSize, availablePoolSizes.size());
    }
    
    private void updateGuaranteesForPoolSize(int poolSize) {
        List<String> availableKeys = patternWheelGenerator.getAvailableGuarantees(poolSize, currentPickSize);
        
        List<GuaranteeLevel> matchingLevels = new ArrayList<>();
        for (String key : availableKeys) {
            String[] parts = key.split("-");
            if (parts.length >= 4) {
                int t = Integer.parseInt(parts[2]);
                int m = Integer.parseInt(parts[3]);
                
                for (GuaranteeLevel level : GuaranteeLevel.values()) {
                    if (level.getPickSize() == currentPickSize && 
                        level.getRequiredHits() == t && 
                        level.getGuaranteedMatches() == m) {
                        matchingLevels.add(level);
                        break;
                    }
                }
            }
        }
        
        guaranteeLevelCombo.getItems().clear();
        guaranteeLevelCombo.getItems().setAll(matchingLevels);
        
        if (!matchingLevels.isEmpty()) {
            guaranteeLevelCombo.getSelectionModel().select(0);
            guaranteeLevelCombo.setDisable(false);
            generateWheelButton.setDisable(false);
            
            GuaranteeLevel selected = matchingLevels.get(0);
            int lineCount = patternWheelGenerator.getWheelLineCount(
                poolSize, currentPickSize, selected.getRequiredHits(), selected.getGuaranteedMatches());
            estimatedLinesLabel.setText(String.valueOf(lineCount));
        } else {
            guaranteeLevelCombo.setDisable(true);
            generateWheelButton.setDisable(true);
            estimatedLinesLabel.setText("-");
        }
        
        logger.info("Updated guarantees for {}-{}: {} options available", 
                   poolSize, currentPickSize, matchingLevels.size());
    }

    @Override
    public void displayEngineConstants(double averageSkip, int longShotThreshold, int coldRuleLimit) {
        averageSkipLabel.setText(String.format("%.1f", averageSkip));
        longShotThresholdLabel.setText(String.valueOf(longShotThreshold));
        coldRuleLimitLabel.setText(String.valueOf(coldRuleLimit));
    }

    @Override
    public void displayHistoricalContext(int drawsAnalyzed, double hitRate, double avgCoverage) {
        drawsAnalyzedLabel.setText(String.valueOf(drawsAnalyzed));
        hitRateLabel.setText(String.format("%.1f%%", hitRate));
        avgCoverageLabel.setText(String.format("%.2f", avgCoverage));
    }

    @Override
    public void displayTier1Anchors(List<Integer> numbers, Map<String, String> patterns) {
        tier1AnchorsPane.getChildren().clear();
        
        for (Integer number : numbers) {
            Label numberLabel = new Label(String.valueOf(number));
            numberLabel.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white; " +
                               "-fx-padding: 8 12; -fx-background-radius: 5; -fx-font-weight: bold;");
            tier1AnchorsPane.getChildren().add(numberLabel);
        }
        
        patternsPane.getChildren().clear();
        for (Map.Entry<String, String> entry : patterns.entrySet()) {
            HBox patternRow = new HBox(10);
            patternRow.setPadding(new Insets(5));
            
            Label numberLabel = new Label(entry.getKey());
            numberLabel.setStyle("-fx-font-weight: bold; -fx-min-width: 40;");
            
            Label patternLabel = new Label(entry.getValue());
            patternLabel.setWrapText(true);
            
            String style = getPatternStyle(entry.getValue());
            patternRow.setStyle(style);
            
            patternRow.getChildren().addAll(numberLabel, patternLabel);
            patternsPane.getChildren().add(patternRow);
        }
    }

    @Override
    public void displayGeneratedSets(List<GeneratedSet> sets) {
        generatedSetsPane.getChildren().clear();
        
        for (GeneratedSet set : sets) {
            VBox setCard = createSetCard(set);
            generatedSetsPane.getChildren().add(setCard);
        }
    }

    @Override
    public void displayExclusionReport(List<ExclusionReport> exclusions) {
        exclusionTable.getItems().setAll(exclusions);
    }

    @Override
    public void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    @Override
    public void showLoading(boolean show) {
        loadingPane.setVisible(show);
        loadingPane.setManaged(show);
        generateButton.setDisable(show);
    }

    private VBox createSetCard(GeneratedSet set) {
        VBox card = new VBox(10);
        card.setStyle("-fx-background-color: white; -fx-border-color: #ddd; " +
                     "-fx-border-radius: 5; -fx-background-radius: 5; -fx-padding: 15;");
        
        Label headerLabel = new Label("Set " + set.getSetId() + " - Diversity: " + 
                                     String.format("%.2f", set.getDiversityScore()));
        headerLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold;");
        
        FlowPane numbersPane = new FlowPane(5, 5);
        for (Integer number : set.getNumbers()) {
            Label numLabel = new Label(String.valueOf(number));
            
            boolean isTier1 = set.getTierBreakdown().getTier1Anchors().contains(number);
            boolean isTier2 = set.getTierBreakdown().getTier2Rotators().contains(number);
            
            if (isTier1) {
                numLabel.setStyle("-fx-background-color: gold; -fx-padding: 5 10; " +
                                "-fx-background-radius: 3; -fx-font-weight: bold;");
            } else if (isTier2) {
                numLabel.setStyle("-fx-background-color: lightblue; -fx-padding: 5 10; " +
                                "-fx-background-radius: 3;");
            } else {
                numLabel.setStyle("-fx-background-color: lightgray; -fx-padding: 5 10; " +
                                "-fx-background-radius: 3;");
            }
            
            numbersPane.getChildren().add(numLabel);
        }
        
        GridPane statsGrid = new GridPane();
        statsGrid.setHgap(15);
        statsGrid.setVgap(5);
        
        TrapStatistics trapStats = set.getSetPerformance().getTrapStatistics();
        int row = 0;
        
        if (trapStats.getPerfectMatch() != null) {
            addStatRow(statsGrid, row++, String.format("%d/%d:", currentPickSize, currentPickSize), 
                      formatMatchStatistic(trapStats.getPerfectMatch()));
        }
        
        if (trapStats.getMatchMinus1() != null) {
            addStatRow(statsGrid, row++, String.format("%d/%d:", currentPickSize - 1, currentPickSize), 
                      formatMatchStatistic(trapStats.getMatchMinus1()));
        }
        
        if (trapStats.getMatchMinus2() != null) {
            addStatRow(statsGrid, row++, String.format("%d/%d:", currentPickSize - 2, currentPickSize), 
                      formatMatchStatistic(trapStats.getMatchMinus2()));
        }
        
        if (trapStats.getMatchMinus3() != null) {
            addStatRow(statsGrid, row++, String.format("%d/%d:", currentPickSize - 3, currentPickSize), 
                      formatMatchStatistic(trapStats.getMatchMinus3()));
        }
        
        addStatRow(statsGrid, row, "Avg Coverage:", String.format("%.2f", set.getSetPerformance().getAverageCoverage()));
        
        card.getChildren().addAll(headerLabel, numbersPane, new Separator(), statsGrid);
        return card;
    }

    private String formatMatchStatistic(MatchStatistic stat) {
        StringBuilder sb = new StringBuilder();
        sb.append(stat.getCount()).append(" hits");
        
        if (stat.getDrawsSinceLastOccurrence() != null) {
            sb.append(" (").append(stat.getDrawsSinceLastOccurrence()).append(" draws ago");
            if (stat.getLastOccurrenceDate() != null) {
                sb.append(", ").append(stat.getLastOccurrenceDate());
            }
            sb.append(")");
        }
        
        return sb.toString();
    }

    private void addStatRow(GridPane grid, int row, String label, String value) {
        Label labelNode = new Label(label);
        labelNode.setStyle("-fx-font-weight: bold;");
        Label valueNode = new Label(value);
        grid.add(labelNode, 0, row);
        grid.add(valueNode, 1, row);
    }

    private String getPatternStyle(String pattern) {
        if (pattern.contains("Trend Reversal")) {
            return "-fx-background-color: #FFF9C4; -fx-border-color: #FBC02D; -fx-border-radius: 3;";
        } else if (pattern.contains("Double Bottom")) {
            return "-fx-background-color: #E3F2FD; -fx-border-color: #2196F3; -fx-border-radius: 3;";
        } else if (pattern.contains("Cascade")) {
            return "-fx-background-color: #F3E5F5; -fx-border-color: #9C27B0; -fx-border-radius: 3;";
        }
        return "-fx-background-color: #F5F5F5; -fx-border-radius: 3;";
    }

    private void setupExclusionTable() {
        excludedNumberColumn.setCellValueFactory(cellData -> 
            new javafx.beans.property.SimpleObjectProperty<>(cellData.getValue().getNumber()));
        reasonColumn.setCellValueFactory(cellData -> 
            new javafx.beans.property.SimpleStringProperty(cellData.getValue().getReason()));
        currentSkipColumn.setCellValueFactory(cellData -> 
            new javafx.beans.property.SimpleObjectProperty<>(cellData.getValue().getCurrentSkip()));
        limitColumn.setCellValueFactory(cellData -> 
            new javafx.beans.property.SimpleObjectProperty<>(cellData.getValue().getLimit()));
    }

    private void setupWheelListeners() {
        setsToWheelList.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        
        setsToWheelList.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            updateEstimatedLines();
            updateGenerateWheelButtonState();
        });
        
        guaranteeLevelCombo.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            updateEstimatedLines();
            updateGenerateWheelButtonState();
        });
    }
    
    private void updateGenerateWheelButtonState() {
        boolean hasSelection = !setsToWheelList.getSelectionModel().getSelectedItems().isEmpty();
        boolean hasGuarantee = guaranteeLevelCombo.getSelectionModel().getSelectedItem() != null;
        generateWheelButton.setDisable(!hasSelection || !hasGuarantee);
    }

    private void populateSetsToWheelList(List<GeneratedSet> sets) {
        setsToWheelList.getItems().clear();
        for (GeneratedSet set : sets) {
            setsToWheelList.getItems().add("Set " + set.getSetId() + " (" + set.getNumbers().size() + " numbers)");
        }
        setsToWheelList.getSelectionModel().clearSelection();
        updateGenerateWheelButtonState();
    }

    private void updateEstimatedLines() {
        if (setsToWheelList.getSelectionModel().getSelectedItems().isEmpty() || 
            guaranteeLevelCombo.getSelectionModel().getSelectedItem() == null) {
            estimatedLinesLabel.setText("0");
            return;
        }
        
        try {
            List<Integer> selectedIndices = setsToWheelList.getSelectionModel().getSelectedIndices();
            if (!selectedIndices.isEmpty() && currentResponse != null) {
                GuaranteeLevel guarantee = guaranteeLevelCombo.getSelectionModel().getSelectedItem();
                
                long totalLines = 0;
                for (int index : selectedIndices) {
                    GeneratedSet selectedSet = currentResponse.getGeneratedSets().get(index);
                    long lines = wheelService.estimateLineCount(
                        selectedSet.getNumbers().size(),
                        currentPickSize,
                        guarantee
                    );
                    totalLines += lines;
                }
                
                estimatedLinesLabel.setText(String.valueOf(totalLines));
            }
        } catch (Exception e) {
            logger.error("Error estimating lines", e);
            estimatedLinesLabel.setText("Error");
        }
    }

    @FXML
    private void handleSelectAllSets() {
        setsToWheelList.getSelectionModel().selectAll();
    }
    
    @FXML
    private void handleClearSetSelection() {
        setsToWheelList.getSelectionModel().clearSelection();
    }

    @FXML
    private void handleGenerateWheel() {
        if (setsToWheelList.getSelectionModel().getSelectedItems().isEmpty()) {
            showError("Please select at least one set to wheel");
            return;
        }
        
        if (guaranteeLevelCombo.getSelectionModel().getSelectedItem() == null) {
            showError("Please select a guarantee level");
            return;
        }
        
        GuaranteeLevel guarantee = guaranteeLevelCombo.getSelectionModel().getSelectedItem();
        
        if (guarantee.getPickSize() != currentPickSize) {
            showError(String.format("Selected guarantee level (%s) does not match current game (Pick-%d). " +
                                   "Please select a valid guarantee level for this game.",
                                   guarantee.getDisplayName(), currentPickSize));
            logger.error("Guarantee level mismatch: {} requires Pick-{}, but current game is Pick-{}",
                        guarantee.getDisplayName(), guarantee.getPickSize(), currentPickSize);
            return;
        }
        
        List<Integer> selectedIndices = setsToWheelList.getSelectionModel().getSelectedIndices();
        
        showLoading(true);
        
        try {
            List<int[]> allCombinations = new ArrayList<>();
            int totalLines = 0;
            
            for (int index : selectedIndices) {
                GeneratedSet selectedSet = currentResponse.getGeneratedSets().get(index);
                
                WheelConfiguration config = new WheelConfiguration(
                    selectedSet.getNumbers().size(),
                    currentPickSize,
                    selectedSet.getNumbers(),
                    guarantee
                );
                
                WheelGenerationResponse wheelResponse = wheelService.generateWheel(config);
                allCombinations.addAll(wheelResponse.getCombinations());
                totalLines += wheelResponse.getTotalLines();
                
                logger.info("Wheeled Set {}: {} combinations", selectedSet.getSetId(), wheelResponse.getTotalLines());
            }
            
            currentWheeledCombinations = allCombinations;
            
            WheelGenerationResponse combinedResponse = new WheelGenerationResponse(
                allCombinations,
                totalLines,
                guarantee,
                null
            );
            
            Platform.runLater(() -> {
                displayWheeledCombinations(combinedResponse);
                generateBetslipsButton.setDisable(false);
                showLoading(false);
            });
        } catch (Exception e) {
            Platform.runLater(() -> {
                showError("Failed to generate wheel: " + e.getMessage());
                showLoading(false);
            });
        }
    }

    private void displayWheeledCombinations(WheelGenerationResponse response) {
        wheeledCombinationsPane.getChildren().clear();
        
        VBox summaryBox = new VBox(5);
        summaryBox.setStyle("-fx-background-color: #E8F5E9; -fx-padding: 10; -fx-background-radius: 5;");
        
        Label summaryLabel = new Label(String.format("✓ Generated %d combinations (%s guarantee)",
            response.getTotalLines(), response.getGuarantee().getDisplayName()));
        summaryLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #2E7D32;");
        
        Label instructionLabel = new Label("Click 'Generate Betslips' above to create printable bet slips");
        instructionLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #555;");
        
        summaryBox.getChildren().addAll(summaryLabel, instructionLabel);
        wheeledCombinationsPane.getChildren().add(summaryBox);
        
        Label spacer = new Label(" ");
        wheeledCombinationsPane.getChildren().add(spacer);
        
        for (int i = 0; i < response.getCombinations().size(); i++) {
            int[] combo = response.getCombinations().get(i);
            String comboStr = java.util.Arrays.stream(combo)
                .mapToObj(n -> String.format("%02d", n))
                .collect(Collectors.joining("-"));
            
            Label comboLabel = new Label((i + 1) + ". " + comboStr);
            comboLabel.setStyle("-fx-font-family: monospace;");
            wheeledCombinationsPane.getChildren().add(comboLabel);
        }
        
        logger.info("Displayed {} wheeled combinations. Generate Betslips button enabled.", response.getTotalLines());
    }

    @FXML
    private void handleGenerateBetslips() {
        if (currentWheeledCombinations == null || currentWheeledCombinations.isEmpty()) {
            showError("No wheeled combinations available");
            return;
        }
        
        showLoading(true);
        
        betslipService.generatePdf(currentWheeledCombinations, currentState, currentGame)
            .doOnSuccess(result -> Platform.runLater(() -> {
                showPreviewDialog(result);
                showLoading(false);
            }))
            .doOnError(error -> Platform.runLater(() -> {
                showError("Failed to generate betslips: " + error.getMessage());
                logger.error("Betslip generation failed", error);
                showLoading(false);
            }))
            .subscribe();
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
                
                logger.info("Betslip preview dialog shown with {} images", result.images.size());
            } catch (Exception e) {
                logger.error("Error showing preview dialog", e);
                showError("Error showing preview: " + e.getMessage());
            }
        });
    }

    private String getSelectedStrategyBias() {
        if (aggressiveButton.isSelected()) return "aggressive";
        if (balancedButton.isSelected()) return "balanced";
        if (conservativeButton.isSelected()) return "conservative";
        return "aggressive";
    }
}
