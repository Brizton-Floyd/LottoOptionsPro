package com.example.lottooptionspro.controller;

import com.example.lottooptionspro.model.wheel.GuaranteeLevel;
import com.example.lottooptionspro.model.wheel.WheelTable;
import com.example.lottooptionspro.service.WheelGenerationService;
import com.example.lottooptionspro.service.WheelTableService;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import net.rgielen.fxweaver.core.FxmlView;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.FileWriter;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

@Component
@FxmlView("/com.example.lottooptionspro/controller/WheelBuilderView.fxml")
public class WheelBuilderController {
    
    private static final Logger logger = LoggerFactory.getLogger(WheelBuilderController.class);
    
    @FXML private Label statusLabel;
    @FXML private TextField poolSizeField;
    @FXML private ComboBox<Integer> pickSizeCombo;
    @FXML private ComboBox<GuaranteeLevel> guaranteeLevelCombo;
    
    @FXML private TableView<WheelTable> existingWheelsTable;
    @FXML private TableColumn<WheelTable, Integer> poolSizeColumn;
    @FXML private TableColumn<WheelTable, Integer> pickSizeColumn;
    @FXML private TableColumn<WheelTable, String> guaranteeColumn;
    @FXML private TableColumn<WheelTable, Integer> linesColumn;
    @FXML private TableColumn<WheelTable, String> sourceColumn;
    @FXML private TableColumn<WheelTable, Boolean> verifiedColumn;
    
    @FXML private VBox progressPanel;
    @FXML private Label progressMessageLabel;
    @FXML private ProgressBar progressBar;
    @FXML private Label subsetsCoveredLabel;
    @FXML private Label linesGeneratedLabel;
    @FXML private Label elapsedTimeLabel;
    
    @FXML private VBox resultsPanel;
    @FXML private Label totalLinesLabel;
    @FXML private Label generationTimeLabel;
    @FXML private Label verifiedLabel;
    @FXML private TextArea patternsPreviewArea;
    
    private final WheelGenerationService wheelGenerationService;
    private final WheelTableService wheelTableService;
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    
    private WheelTable generatedWheel;
    private final AtomicBoolean cancelled = new AtomicBoolean(false);
    
    public WheelBuilderController(WheelGenerationService wheelGenerationService, WheelTableService wheelTableService) {
        this.wheelGenerationService = wheelGenerationService;
        this.wheelTableService = wheelTableService;
    }
    
    @FXML
    private void initialize() {
        pickSizeCombo.getItems().addAll(4, 5, 6);
        pickSizeCombo.getSelectionModel().select(1); // Default to Pick-5
        pickSizeCombo.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            updateGuaranteeLevels();
            loadExistingWheels();
        });
        
        setupExistingWheelsTable();
        loadExistingWheels();
        
        updateGuaranteeLevels();
        poolSizeField.setText("15");
    }
    
    private void setupExistingWheelsTable() {
        poolSizeColumn.setCellValueFactory(cellData -> 
            new javafx.beans.property.SimpleObjectProperty<>(cellData.getValue().getPoolSize()));
        pickSizeColumn.setCellValueFactory(cellData -> 
            new javafx.beans.property.SimpleObjectProperty<>(cellData.getValue().getPickSize()));
        guaranteeColumn.setCellValueFactory(cellData -> {
            GuaranteeLevel level = cellData.getValue().getGuaranteeLevel();
            String displayName = level != null ? level.getDisplayName() : "Unknown";
            return new javafx.beans.property.SimpleStringProperty(displayName);
        });
        linesColumn.setCellValueFactory(cellData -> 
            new javafx.beans.property.SimpleObjectProperty<>(cellData.getValue().getTotalLines()));
        sourceColumn.setCellValueFactory(cellData -> 
            new javafx.beans.property.SimpleStringProperty(cellData.getValue().getSource()));
        verifiedColumn.setCellValueFactory(cellData -> 
            new javafx.beans.property.SimpleObjectProperty<>(cellData.getValue().isVerified()));
    }
    
    private void loadExistingWheels() {
        try {
            existingWheelsTable.getItems().clear();
            existingWheelsTable.refresh();
            
            Integer selectedPickSize = pickSizeCombo.getValue();
            if (selectedPickSize == null) {
                return;
            }
            
            poolSizeColumn.setSortType(javafx.scene.control.TableColumn.SortType.DESCENDING);
            existingWheelsTable.getSortOrder().clear();
            existingWheelsTable.getSortOrder().add(poolSizeColumn);
            
            java.nio.file.Path wheelsRoot = java.nio.file.Paths.get("wheels").toAbsolutePath();
            if (!java.nio.file.Files.exists(wheelsRoot)) {
                logger.warn("Wheels directory not found: {}", wheelsRoot);
                return;
            }
            
            java.util.List<WheelTable> wheelTables = new java.util.ArrayList<>();
            
            java.nio.file.Files.walk(wheelsRoot)
                .filter(java.nio.file.Files::isRegularFile)
                .filter(p -> p.toString().endsWith(".txt"))
                .forEach(wheelFile -> {
                    try {
                        String path = wheelFile.toString();
                        String pickSizeStr = path.contains("pick5") ? "5" : path.contains("pick6") ? "6" : path.contains("pick4") ? "4" : "5";
                        int pickSize = Integer.parseInt(pickSizeStr);
                        
                        if (pickSize != selectedPickSize) {
                            return;
                        }
                        
                        String poolSizeStr = wheelFile.getParent().getFileName().toString();
                        int poolSize = Integer.parseInt(poolSizeStr);
                        
                        String filename = wheelFile.getFileName().toString();
                        String[] parts = filename.replace(".txt", "").split("_if_");
                        int m = Integer.parseInt(parts[0]);
                        int t = Integer.parseInt(parts[1]);
                        
                        java.util.List<String> lines = java.nio.file.Files.readAllLines(wheelFile);
                        
                        WheelTable table = new WheelTable();
                        table.setPoolSize(poolSize);
                        table.setPickSize(pickSize);
                        table.setTotalLines(lines.size());
                        table.setGuaranteeLevel(GuaranteeLevel.fromMandT(m, t, pickSize));
                        table.setSource("File: " + wheelFile.getFileName());
                        table.setVerified(true);
                        
                        wheelTables.add(table);
                        
                    } catch (Exception e) {
                        logger.error("Failed to load wheel: {}", wheelFile, e);
                    }
                });
            
            existingWheelsTable.getItems().addAll(wheelTables);
            
            logger.info("Loaded {} existing wheels for Pick-{} from {}", 
                       existingWheelsTable.getItems().size(), selectedPickSize, wheelsRoot);
            
            existingWheelsTable.sort();
            existingWheelsTable.refresh();
            
        } catch (Exception e) {
            logger.error("Failed to load existing wheels", e);
        }
    }
    
    @FXML
    private void handleReloadWheels() {
        loadExistingWheels();
        statusLabel.setText("Wheels reloaded");
    }
    
    private void updateGuaranteeLevels() {
        Integer pickSize = pickSizeCombo.getValue();
        if (pickSize == null) return;
        
        GuaranteeLevel[] levels = GuaranteeLevel.getAvailableGuaranteesForPickSize(pickSize);
        guaranteeLevelCombo.getItems().clear();
        guaranteeLevelCombo.getItems().addAll(levels);
        if (levels.length > 0) {
            guaranteeLevelCombo.getSelectionModel().select(0);
        }
    }
    
    @FXML
    private void handleGenerateWheel() {
        try {
            int poolSize = Integer.parseInt(poolSizeField.getText().trim());
            Integer pickSize = pickSizeCombo.getValue();
            GuaranteeLevel guaranteeLevel = guaranteeLevelCombo.getValue();
            
            if (pickSize == null || guaranteeLevel == null) {
                showError("Please select pick size and guarantee level");
                return;
            }
            
            if (poolSize < 4 || poolSize > 27) {
                showError("Pool size must be between 4 and 27");
                return;
            }
            
            if (poolSize < pickSize) {
                showError("Pool size must be at least equal to pick size");
                return;
            }
            
            cancelled.set(false);
            resultsPanel.setVisible(false);
            resultsPanel.setManaged(false);
            progressPanel.setVisible(true);
            progressPanel.setManaged(true);
            statusLabel.setText("Generating...");
            
            long startTime = System.currentTimeMillis();
            
            Thread generationThread = new Thread(() -> {
                try {
                    wheelGenerationService.generateWheelWithProgress(
                        poolSize, 
                        pickSize, 
                        guaranteeLevel,
                        this::updateProgress,
                        cancelled
                    ).thenAccept(wheel -> {
                        if (!cancelled.get()) {
                            long elapsedTime = System.currentTimeMillis() - startTime;
                            Platform.runLater(() -> {
                                generatedWheel = wheel;
                                displayResults(wheel, elapsedTime);
                            });
                        }
                    }).exceptionally(error -> {
                        Platform.runLater(() -> {
                            showError("Generation failed: " + error.getMessage());
                            resetUI();
                        });
                        return null;
                    });
                    
                    Thread timeThread = new Thread(() -> {
                        while (!cancelled.get() && generatedWheel == null) {
                            try {
                                Thread.sleep(1000);
                                long elapsed = (System.currentTimeMillis() - startTime) / 1000;
                                Platform.runLater(() -> elapsedTimeLabel.setText(elapsed + "s"));
                            } catch (InterruptedException e) {
                                break;
                            }
                        }
                    });
                    timeThread.setDaemon(true);
                    timeThread.start();
                    
                } catch (Exception e) {
                    logger.error("Wheel generation error", e);
                    Platform.runLater(() -> {
                        showError("Generation error: " + e.getMessage());
                        resetUI();
                    });
                }
            });
            
            generationThread.setDaemon(true);
            generationThread.start();
            
        } catch (NumberFormatException e) {
            showError("Please enter a valid pool size");
        }
    }
    
    private void updateProgress(String message, double progress, int subsetsCovered, int totalSubsets, int linesGenerated) {
        Platform.runLater(() -> {
            progressMessageLabel.setText(message);
            progressBar.setProgress(progress);
            subsetsCoveredLabel.setText(subsetsCovered + " / " + totalSubsets);
            linesGeneratedLabel.setText(String.valueOf(linesGenerated));
        });
    }
    
    private void displayResults(WheelTable wheel, long elapsedTime) {
        progressPanel.setVisible(false);
        progressPanel.setManaged(false);
        resultsPanel.setVisible(true);
        resultsPanel.setManaged(true);
        
        totalLinesLabel.setText(String.valueOf(wheel.getTotalLines()));
        generationTimeLabel.setText((elapsedTime / 1000.0) + "s");
        verifiedLabel.setText(wheel.isVerified() ? "Yes" : "No");
        verifiedLabel.setStyle(wheel.isVerified() ? 
            "-fx-text-fill: #4CAF50; -fx-font-weight: bold;" : 
            "-fx-text-fill: #F44336; -fx-font-weight: bold;");
        
        List<int[]> patterns = wheel.getPatterns();
        int previewCount = Math.min(20, patterns.size());
        StringBuilder preview = new StringBuilder();
        for (int i = 0; i < previewCount; i++) {
            preview.append(Arrays.toString(patterns.get(i))).append("\n");
        }
        if (patterns.size() > 20) {
            preview.append("\n... and ").append(patterns.size() - 20).append(" more patterns");
        }
        patternsPreviewArea.setText(preview.toString());
        
        statusLabel.setText("Generation Complete");
        statusLabel.setStyle("-fx-text-fill: #4CAF50;");
        
        logger.info("Wheel generated: {}-{}-{} with {} lines in {}ms", 
                   wheel.getPoolSize(), wheel.getPickSize(), wheel.getGuaranteeLevel(), 
                   wheel.getTotalLines(), elapsedTime);
    }
    
    @FXML
    private void handleSaveWheel() {
        showInfo("Wheels are automatically saved during generation to the wheels directory.");
    }
    
    @FXML
    private void handleCancel() {
        cancelled.set(true);
        resetUI();
        statusLabel.setText("Cancelled");
        statusLabel.setStyle("-fx-text-fill: #F44336;");
    }
    
    @FXML
    private void handleClear() {
        poolSizeField.clear();
        pickSizeCombo.getSelectionModel().select(1);
        generatedWheel = null;
        resetUI();
        statusLabel.setText("Ready");
        statusLabel.setStyle("");
    }
    
    private void resetUI() {
        progressPanel.setVisible(false);
        progressPanel.setManaged(false);
        resultsPanel.setVisible(false);
        resultsPanel.setManaged(false);
        progressBar.setProgress(0);
        subsetsCoveredLabel.setText("0 / 0");
        linesGeneratedLabel.setText("0");
        elapsedTimeLabel.setText("0s");
    }
    
    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
    
    private void showInfo(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Success");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
