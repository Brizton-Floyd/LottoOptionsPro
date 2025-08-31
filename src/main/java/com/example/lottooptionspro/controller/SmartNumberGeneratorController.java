package com.example.lottooptionspro.controller;

import com.example.lottooptionspro.GameInformation;
import com.example.lottooptionspro.model.smart.*;
import com.example.lottooptionspro.presenter.SmartNumberGeneratorPresenter;
import com.example.lottooptionspro.presenter.SmartNumberGeneratorView;
import com.example.lottooptionspro.service.BetslipGenerationService;
import com.example.lottooptionspro.service.SmartNumberGenerationService;
import javafx.application.Platform;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import net.rgielen.fxweaver.core.FxControllerAndView;
import net.rgielen.fxweaver.core.FxWeaver;
import net.rgielen.fxweaver.core.FxmlView;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.io.File;
import java.text.DecimalFormat;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
@FxmlView("/com.example.lottooptionspro/controller/smartNumberGenerator.fxml")
public class SmartNumberGeneratorController implements GameInformation, SmartNumberGeneratorView {

    @FXML private VBox contentHolder;
    @FXML private Label gameContextLabel;
    @FXML private VBox tierSelectionBox;
    @FXML private Spinner<Integer> ticketCountSpinner;
    @FXML private TextField budgetField;
    @FXML private RadioButton patternBasedRadio;
    @FXML private RadioButton randomRadio;
    @FXML private RadioButton hybridRadio;
    @FXML private TitledPane advancedPreferencesPane;
    @FXML private TextField preferredNumbersField;
    @FXML private TextField excludeNumbersField;
    @FXML private Slider qualityScoreSlider;
    @FXML private Label qualityScoreLabel;
    @FXML private Slider coverageSlider;
    @FXML private Label coverageLabel;
    @FXML private CheckBox preventDuplicatesCheck;
    @FXML private CheckBox enableMultiBatchCheck;
    @FXML private CheckBox avoidConsecutiveCheck;
    @FXML private CheckBox balancedDistributionCheck;
    @FXML private Button generateButton;
    @FXML private Button cancelButton;
    @FXML private Label estimatedCostLabel;
    @FXML private VBox progressSection;
    @FXML private ProgressBar progressBar;
    @FXML private Label progressLabel;
    @FXML private Label sessionLabel;
    @FXML private Label qualityGradeLabel;
    @FXML private Label patternDistLabel;
    @FXML private Label timeElapsedLabel;
    @FXML private VBox resultsSection;
    @FXML private TableView<TicketDisplay> generatedTicketsTable;
    @FXML private TableColumn<TicketDisplay, Integer> ticketNumberColumn;
    @FXML private TableColumn<TicketDisplay, String> numbersColumn;
    @FXML private TableColumn<TicketDisplay, String> qualityColumn;
    @FXML private Label qualityGradeDisplay;
    @FXML private ProgressBar optimizationScoreBar;
    @FXML private Label optimizationScoreLabel;
    @FXML private Label improvementLabel;
    @FXML private ProgressBar hotPatternBar;
    @FXML private Label hotPatternLabel;
    @FXML private ProgressBar warmPatternBar;
    @FXML private Label warmPatternLabel;
    @FXML private ProgressBar coldPatternBar;
    @FXML private Label coldPatternLabel;
    @FXML private Label coveragePercentageLabel;
    @FXML private Label uniquenessScoreLabel;
    @FXML private Label generationTimeLabel;
    @FXML private Label successRateLabel;
    @FXML private Button generateBetslipsButton;
    @FXML private Button exportCsvButton;
    @FXML private Button saveTicketsButton;
    @FXML private ProgressIndicator loadingIndicator;

    private final FxWeaver fxWeaver;
    private final SmartNumberGeneratorPresenter presenter;
    private final BetslipGenerationService betslipGenerationService;
    
    private ToggleGroup tierToggleGroup;
    private ToggleGroup strategyToggleGroup;
    private String stateName;
    private String gameName;
    private LotteryConfiguration currentConfig;
    private TicketGenerationResult currentResult;

    public SmartNumberGeneratorController(SmartNumberGenerationService smartService, 
                                        FxWeaver fxWeaver, 
                                        BetslipGenerationService betslipGenerationService) {
        this.fxWeaver = fxWeaver;
        this.presenter = new SmartNumberGeneratorPresenter(this, smartService);
        this.betslipGenerationService = betslipGenerationService;
    }

    @FXML
    public void initialize() {
        setupStrategyRadioButtons();
        setupSpinners();
        setupSliders();
        setupTableColumns();
        setupEventHandlers();
    }

    private void setupStrategyRadioButtons() {
        strategyToggleGroup = new ToggleGroup();
        patternBasedRadio.setToggleGroup(strategyToggleGroup);
        randomRadio.setToggleGroup(strategyToggleGroup);
        hybridRadio.setToggleGroup(strategyToggleGroup);
        patternBasedRadio.setSelected(true);
    }

    private void setupSpinners() {
        ticketCountSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 1000, 20));
        ticketCountSpinner.valueProperty().addListener((obs, oldVal, newVal) -> updateEstimatedCost());
    }

    private void setupSliders() {
        qualityScoreSlider.valueProperty().addListener((obs, oldVal, newVal) -> 
            qualityScoreLabel.setText(String.format("%.0f%%", newVal.doubleValue())));
        
        coverageSlider.valueProperty().addListener((obs, oldVal, newVal) -> 
            coverageLabel.setText(String.format("%.0f%%", newVal.doubleValue())));
    }

    private void setupTableColumns() {
        ticketNumberColumn.setCellValueFactory(new PropertyValueFactory<>("ticketNumber"));
        numbersColumn.setCellValueFactory(new PropertyValueFactory<>("numbersDisplay"));
        qualityColumn.setCellValueFactory(new PropertyValueFactory<>("qualityDisplay"));
    }

    private void setupEventHandlers() {
        budgetField.textProperty().addListener((obs, oldVal, newVal) -> updateTicketCountFromBudget());
    }

    @Override
    public Mono<Void> setUpUi(String stateName, String gameName) {
        this.stateName = stateName;
        this.gameName = gameName;
        
        Platform.runLater(() -> {
            gameContextLabel.setText(String.format("%s - %s", stateName, gameName));
            showLoading(true);
        });
        
        return presenter.loadLotteryConfiguration(stateName, gameName)
                .doOnSuccess(config -> Platform.runLater(() -> {
                    this.currentConfig = config;
                    setupTierOptions(config);
                    updateEstimatedCost();
                    showLoading(false);
                }))
                .doOnError(error -> Platform.runLater(() -> {
                    showAlert("Error", "Failed to load lottery configuration: " + error.getMessage());
                    showLoading(false);
                }))
                .then();
    }

    private void setupTierOptions(LotteryConfiguration config) {
        tierToggleGroup = new ToggleGroup();
        tierSelectionBox.getChildren().clear();

        if (config.getPrizeStructure() != null) {
            config.getPrizeStructure().entrySet().stream()
                    .filter(entry -> entry.getValue().isActive())
                    .sorted((a, b) -> Integer.compare(b.getValue().getMatchCount(), a.getValue().getMatchCount()))
                    .forEach(entry -> {
                        String tierKey = entry.getKey();
                        LotteryConfiguration.PrizeTier tier = entry.getValue();
                        
                        String tierDisplay = String.format("%d-of-%d %s (%d tickets/batch)",
                                tier.getMatchCount(), 
                                config.getDrawSize(),
                                tier.getDescription().replace("Match " + tier.getMatchCount() + " numbers", "").trim(),
                                getOptimalTicketsForTier(tier.getMatchCount(), config.getDrawSize()));

                        RadioButton tierRadio = new RadioButton(tierDisplay);
                        tierRadio.setToggleGroup(tierToggleGroup);
                        tierRadio.setUserData(String.format("%d-of-%d", tier.getMatchCount(), config.getDrawSize()));
                        
                        if (tier.getMatchCount() == config.getDrawSize() - 1) {
                            tierRadio.setSelected(true);
                        }
                        
                        tierRadio.selectedProperty().addListener((obs, wasSelected, isSelected) -> {
                            if (isSelected) updateEstimatedCost();
                        });
                        
                        tierSelectionBox.getChildren().add(tierRadio);
                    });
        }
    }

    private int getOptimalTicketsForTier(int matchCount, int drawSize) {
        if (matchCount == drawSize) return 20;      // Jackpot
        if (matchCount == drawSize - 1) return 30; // Near-jackpot
        if (matchCount == drawSize - 2) return 50; // Secondary
        return 75; // Lower tiers
    }

    private void updateEstimatedCost() {
        if (tierToggleGroup == null || tierToggleGroup.getSelectedToggle() == null) return;
        
        RadioButton selectedTier = (RadioButton) tierToggleGroup.getSelectedToggle();
        String tierData = (String) selectedTier.getUserData();
        int ticketCount = ticketCountSpinner.getValue();
        
        double costPerTicket = getCostPerTicket(tierData);
        double totalCost = ticketCount * costPerTicket;
        
        DecimalFormat df = new DecimalFormat("#,##0.00");
        estimatedCostLabel.setText("Estimated Cost: $" + df.format(totalCost));
    }

    private double getCostPerTicket(String tierData) {
        if (tierData.contains("-of-")) {
            String[] parts = tierData.split("-of-");
            int matchCount = Integer.parseInt(parts[0]);
            int totalCount = Integer.parseInt(parts[1]);
            
            if (matchCount == totalCount) return 5.0;      // Jackpot
            if (matchCount == totalCount - 1) return 3.0;  // Near-jackpot  
            if (matchCount == totalCount - 2) return 2.0;  // Secondary
            return 1.0; // Lower tiers
        }
        return 2.0; // Default
    }

    private void updateTicketCountFromBudget() {
        try {
            double budget = Double.parseDouble(budgetField.getText());
            if (tierToggleGroup != null && tierToggleGroup.getSelectedToggle() != null) {
                RadioButton selectedTier = (RadioButton) tierToggleGroup.getSelectedToggle();
                String tierData = (String) selectedTier.getUserData();
                double costPerTicket = getCostPerTicket(tierData);
                
                int maxTickets = (int) Math.floor(budget / costPerTicket);
                ticketCountSpinner.getValueFactory().setValue(Math.max(1, maxTickets));
            }
        } catch (NumberFormatException e) {
            // Ignore invalid input
        }
    }

    @FXML
    private void generateSmartTickets() {
        presenter.generateSmartTickets();
    }

    @FXML
    private void cancelGeneration() {
        presenter.cancelGeneration();
    }

    @FXML
    private void generateBetslips() {
        if (currentResult == null || currentResult.getTickets() == null) {
            showAlert("Info", "No tickets available to generate betslips.");
            return;
        }

        boolean hasTemplate = betslipGenerationService.hasTemplateForGame(stateName, gameName);

        if (hasTemplate) {
            int[][] ticketArrays = currentResult.getTicketsAsIntArrays();
            List<int[]> numberSets = Arrays.asList(ticketArrays);

            betslipGenerationService.generatePdf(numberSets, stateName, gameName)
                    .doOnSubscribe(subscription -> Platform.runLater(() -> {
                        showLoading(true);
                        setContentDisabled(true);
                    }))
                    .doFinally(signalType -> Platform.runLater(() -> {
                        showLoading(false);
                        setContentDisabled(false);
                    }))
                    .subscribe(
                            this::showPreviewDialog,
                            error -> Platform.runLater(() -> {
                                showAlert("Error", "Failed to generate PDF: " + error.getMessage());
                                error.printStackTrace();
                            })
                    );
        } else {
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setTitle("Template Not Found");
            alert.setHeaderText("No betslip template found for this game.");
            alert.setContentText("Would you like to create one now?");

            Optional<ButtonType> result = alert.showAndWait();
            if (result.isPresent() && result.get() == ButtonType.OK) {
                fxWeaver.loadController(MainController.class).switchToBetslipTemplateEditor();
            }
        }
    }

    private void showPreviewDialog(BetslipGenerationService.PdfGenerationResult result) {
        Platform.runLater(() -> {
            FxControllerAndView<PdfPreviewController, Parent> controllerAndView = fxWeaver.load(PdfPreviewController.class);
            PdfPreviewController controller = controllerAndView.getController();
            controller.presenter.setData(result.images, result.template);

            Stage dialogStage = new Stage();
            dialogStage.initModality(Modality.WINDOW_MODAL);
            dialogStage.initOwner(contentHolder.getScene().getWindow());
            dialogStage.setTitle("PDF Preview");

            Scene scene = new Scene(controllerAndView.getView().get());
            dialogStage.setScene(scene);
            dialogStage.showAndWait();
        });
    }

    @FXML
    private void exportCsv() {
        presenter.exportCsv();
    }

    @FXML
    private void saveTickets() {
        presenter.saveTickets();
    }

    @Override
    public SmartGenerationRequest createGenerationRequest() {
        SmartGenerationRequest request = new SmartGenerationRequest();
        
        // Basic configuration
        request.setUserId("default-user");
        request.setNumberOfTickets(ticketCountSpinner.getValue());
        request.setLotteryConfigId(presenter.deriveConfigId(stateName, gameName));
        request.setLotteryState(stateName);
        request.setLotteryGame(gameName);

        // Budget
        try {
            if (!budgetField.getText().trim().isEmpty()) {
                request.setBudget(Double.parseDouble(budgetField.getText()));
            }
        } catch (NumberFormatException e) {
            // Budget is optional
        }

        // Target Tier
        if (tierToggleGroup.getSelectedToggle() != null) {
            RadioButton selectedTier = (RadioButton) tierToggleGroup.getSelectedToggle();
            request.setTargetTier((String) selectedTier.getUserData());
        }

        // Strategy
        if (patternBasedRadio.isSelected()) {
            request.setGenerationStrategy("PATTERN_BASED");
        } else if (randomRadio.isSelected()) {
            request.setGenerationStrategy("RANDOM");
        } else {
            request.setGenerationStrategy("HYBRID");
        }

        // User Preferences
        UserPreferences prefs = new UserPreferences();
        prefs.setAvoidConsecutive(avoidConsecutiveCheck.isSelected());
        prefs.setMinQualityScore(qualityScoreSlider.getValue());
        prefs.setTargetCoveragePercentage(coverageSlider.getValue());
        prefs.setPreferBalancedDistribution(balancedDistributionCheck.isSelected());
        prefs.setEnableMultiBatch(enableMultiBatchCheck.isSelected());
        prefs.setPreventDuplicates(preventDuplicatesCheck.isSelected());

        // Parse preferred numbers
        if (!preferredNumbersField.getText().trim().isEmpty()) {
            try {
                List<Integer> preferred = Arrays.stream(preferredNumbersField.getText().split(","))
                        .map(String::trim)
                        .map(Integer::parseInt)
                        .collect(Collectors.toList());
                prefs.setPreferredNumbers(preferred);
            } catch (NumberFormatException e) {
                // Invalid format, skip
            }
        }

        // Parse excluded numbers
        if (!excludeNumbersField.getText().trim().isEmpty()) {
            try {
                List<Integer> excluded = Arrays.stream(excludeNumbersField.getText().split(","))
                        .map(String::trim)
                        .map(Integer::parseInt)
                        .collect(Collectors.toList());
                prefs.setExcludeNumbers(excluded);
            } catch (NumberFormatException e) {
                // Invalid format, skip
            }
        }

        request.setPreferences(prefs);
        return request;
    }

    @Override
    public void showLoading(boolean show) {
        loadingIndicator.setVisible(show);
    }

    @Override
    public void setContentDisabled(boolean disabled) {
        contentHolder.setDisable(disabled);
    }

    @Override
    public void showGenerationProgress(boolean show) {
        progressSection.setVisible(show);
    }

    @Override
    public void updateProgress(double progress, String message) {
        progressBar.setProgress(progress);
        progressLabel.setText(message);
        
        // Update cancel button state based on progress
        if (progress >= 0 && progress < 1.0) {
            cancelButton.setDisable(false);
        } else {
            cancelButton.setDisable(true);
        }
    }

    @Override
    public void updateSessionInfo(String sessionId) {
        sessionLabel.setText("Session: " + sessionId);
    }

    @Override
    public void updateQualityMetrics(QualityMetrics metrics) {
        if (metrics != null) {
            // Update real-time quality display in progress section
            qualityGradeLabel.setText(String.format("Quality: %s (%.1f%%)", 
                    metrics.getQualityGrade(), metrics.getOptimizationScore()));
            
            patternDistLabel.setText(String.format("HOT: %.0f%% | WARM: %.0f%%", 
                    metrics.getHotPatternPercentage(), metrics.getWarmPatternPercentage()));
            
            // If results section is visible, also update the detailed metrics
            if (resultsSection.isVisible()) {
                updateQualityMetricsDisplay(metrics);
            }
        }
    }

    @Override
    public void updateTimeElapsed(double seconds) {
        timeElapsedLabel.setText(String.format("Time: %.1fs", seconds));
    }

    @Override
    public void showResults(TicketGenerationResult result) {
        this.currentResult = result;
        resultsSection.setVisible(true);
        
        // Update comprehensive quality metrics
        updateQualityMetricsDisplay(result.getQualityMetrics());
        
        if (result.getGenerationSummary() != null) {
            generationTimeLabel.setText(String.format("%.1fs", 
                    result.getGenerationSummary().getElapsedTimeSeconds()));
            successRateLabel.setText(String.format("%.1f%%", 
                    result.getGenerationSummary().getSuccessRate()));
        }

        // Update tickets table
        updateTicketsTable(result.getTickets());
        
        // Enable action buttons
        generateBetslipsButton.setDisable(false);
        exportCsvButton.setDisable(false);
        saveTicketsButton.setDisable(false);
    }

    private void updateQualityMetricsDisplay(QualityMetrics metrics) {
        if (metrics == null) {
            resetQualityMetricsDisplay();
            return;
        }

        // Quality Grade Display
        qualityGradeDisplay.setText(metrics.getQualityGrade());
        qualityGradeDisplay.getStyleClass().removeAll("grade-a", "grade-b", "grade-c", "grade-d", "grade-f");
        qualityGradeDisplay.getStyleClass().add("grade-" + metrics.getQualityGrade().toLowerCase());

        // Optimization Score Bar and Label
        double optimizationScore = metrics.getOptimizationScore();
        optimizationScoreBar.setProgress(optimizationScore / 100.0);
        optimizationScoreLabel.setText(String.format("%.1f%%", optimizationScore));

        // Pattern Distribution Bars
        double hotPercentage = metrics.getHotPatternPercentage();
        double warmPercentage = metrics.getWarmPatternPercentage();
        double coldPercentage = metrics.getColdPatternPercentage();

        hotPatternBar.setProgress(hotPercentage / 100.0);
        hotPatternLabel.setText(String.format("%.0f%%", hotPercentage));

        warmPatternBar.setProgress(warmPercentage / 100.0);
        warmPatternLabel.setText(String.format("%.0f%%", warmPercentage));

        coldPatternBar.setProgress(coldPercentage / 100.0);
        coldPatternLabel.setText(String.format("%.0f%%", coldPercentage));

        // Additional Metrics
        coveragePercentageLabel.setText(String.format("%.1f%%", metrics.getCoveragePercentage()));
        uniquenessScoreLabel.setText(String.format("%.1f", metrics.getUniquenessScore()));

        // Improvement Metrics
        if (metrics.getImprovementMetrics() != null) {
            double improvementPercentage = metrics.getImprovementMetrics().getImprovementPercentage();
            improvementLabel.setText(String.format("%.0f%%", improvementPercentage));
            
            // Add visual styling based on improvement level
            improvementLabel.getStyleClass().removeAll("improvement-excellent", "improvement-good", "improvement-poor");
            if (improvementPercentage > 1000) {
                improvementLabel.getStyleClass().add("improvement-excellent");
            } else if (improvementPercentage > 100) {
                improvementLabel.getStyleClass().add("improvement-good");
            } else {
                improvementLabel.getStyleClass().add("improvement-poor");
            }
        }
    }

    private void resetQualityMetricsDisplay() {
        qualityGradeDisplay.setText("-");
        optimizationScoreBar.setProgress(0);
        optimizationScoreLabel.setText("- %");
        
        hotPatternBar.setProgress(0);
        hotPatternLabel.setText("0%");
        warmPatternBar.setProgress(0);
        warmPatternLabel.setText("0%");
        coldPatternBar.setProgress(0);
        coldPatternLabel.setText("0%");
        
        coveragePercentageLabel.setText("-");
        uniquenessScoreLabel.setText("-");
        improvementLabel.setText("- %");
    }

    private void updateTicketsTable(List<List<Integer>> tickets) {
        ObservableList<TicketDisplay> ticketDisplays = FXCollections.observableArrayList();
        
        for (int i = 0; i < tickets.size(); i++) {
            List<Integer> ticket = tickets.get(i);
            String numbersStr = ticket.stream()
                    .map(String::valueOf)
                    .collect(Collectors.joining(", "));
            
            ticketDisplays.add(new TicketDisplay(i + 1, numbersStr, "A"));
        }
        
        generatedTicketsTable.setItems(ticketDisplays);
    }

    @Override
    public void enableGenerationControls(boolean enabled) {
        generateButton.setDisable(!enabled);
        cancelButton.setDisable(enabled);
    }

    @Override
    public void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    @Override
    public File showSaveDialog(String initialDirectory, String initialFileName) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Save Smart Generated Tickets");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("JSON Files", "*.json"));
        File directory = new File(initialDirectory);
        if (!directory.exists()) {
            directory.mkdirs();
        }
        fileChooser.setInitialDirectory(directory);
        fileChooser.setInitialFileName(initialFileName);
        return fileChooser.showSaveDialog(contentHolder.getScene().getWindow());
    }

    public static class TicketDisplay {
        private final SimpleIntegerProperty ticketNumber;
        private final SimpleStringProperty numbersDisplay;
        private final SimpleStringProperty qualityDisplay;

        public TicketDisplay(int ticketNumber, String numbersDisplay, String qualityDisplay) {
            this.ticketNumber = new SimpleIntegerProperty(ticketNumber);
            this.numbersDisplay = new SimpleStringProperty(numbersDisplay);
            this.qualityDisplay = new SimpleStringProperty(qualityDisplay);
        }

        public int getTicketNumber() { return ticketNumber.get(); }
        public String getNumbersDisplay() { return numbersDisplay.get(); }
        public String getQualityDisplay() { return qualityDisplay.get(); }
    }
}