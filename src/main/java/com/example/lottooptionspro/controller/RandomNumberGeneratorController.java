package com.example.lottooptionspro.controller;

import com.example.lottooptionspro.GameInformation;
import com.example.lottooptionspro.service.RandomNumberGeneratorService;
import com.floyd.model.generatednumbers.GeneratedNumberData;
import com.floyd.model.generatednumbers.PrizeLevelResult;
import com.floyd.model.request.RandomNumberGeneratorRequest;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import net.rgielen.fxweaver.core.FxWeaver;
import net.rgielen.fxweaver.core.FxmlView;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.io.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Component
@FxmlView("/com.example.lottooptionspro/controller/randomNumberGenerator.fxml")
public class RandomNumberGeneratorController implements GameInformation {

    @FXML private ComboBox<String> rngComboBox;
    @FXML private TextField numberSetPerPatternField;
    @FXML private TextField targetedPrizeLevelField;
    @FXML private TextField drawDaysPerWeekField;
    @FXML private ProgressIndicator progressIndicator;
    @FXML private TableView<int[]> generatedNumbersTable;
    @FXML private TableColumn<int[], String> numberSetColumn;
    @FXML private TableView<PrizeLevelResult> prizeLevelResultsTable;
    @FXML private TableColumn<PrizeLevelResult, Integer> correctNumbersColumn;
    @FXML private TableColumn<PrizeLevelResult, Integer> hitsColumn;
    @FXML private TableColumn<PrizeLevelResult, Integer> gamesOutColumn;
    @FXML private VBox contentHolder;
    @FXML private Label totalTicketsLabel;
    @FXML private Label estimatedDaysLabel;

    private final FxWeaver fxWeaver;
    private GeneratedNumberData currentData;
    private final RandomNumberGeneratorService randomNumberGeneratorService;
    private String gameName, stateName;

    public RandomNumberGeneratorController(RandomNumberGeneratorService randomNumberGeneratorService, FxWeaver fxWeaver) {
        this.randomNumberGeneratorService = randomNumberGeneratorService;
        this.fxWeaver = fxWeaver;
    }

    @FXML
    public void initialize() {
        rngComboBox.getItems().addAll("Random 1", "SecureRandom", "ThreadLocalRandom");

        numberSetColumn.setCellValueFactory(cellData ->
                new SimpleStringProperty(Arrays.toString(cellData.getValue())));

        correctNumbersColumn.setCellValueFactory(new PropertyValueFactory<>("correctNumbers"));
        hitsColumn.setCellValueFactory(new PropertyValueFactory<>("hits"));
        gamesOutColumn.setCellValueFactory(new PropertyValueFactory<>("gamesOut"));
    }

    @FXML
    private void generateNumbers() {

        progressIndicator.setVisible(true);
        contentHolder.setDisable(true);
        Task<GeneratedNumberData> task = new Task<>() {
            @Override
            protected GeneratedNumberData call() throws Exception {
                // Simulating API call
                return randomNumberGeneratorService.generateNumbers(createRequest()).block();
            }
        };

        task.setOnSucceeded(e -> {
            progressIndicator.setVisible(false);
            contentHolder.setDisable(false);
            currentData = task.getValue();
            updateUI();
        });

        task.setOnFailed(e -> {
            progressIndicator.setVisible(false);
            contentHolder.setDisable(false);
            // Handle error
        });

        new Thread(task).start();
    }

    private void updateUI() {
        generatedNumbersTable.setItems(FXCollections.observableArrayList(currentData.getGeneratedNumbers()));
        prizeLevelResultsTable.setItems(FXCollections.observableArrayList(currentData.getPrizeLevelResults()));

        totalTicketsLabel.setText(String.valueOf(currentData.getTotalTickets()));
        estimatedDaysLabel.setText(String.format("%.2f", currentData.getEstimatedElapsedDaysForTargetedPrizeLevelWin()));
    }

    @FXML
    private void generateBetslips() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Open Chosen Numbers File");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Chosen Number Files", "*.ser"));
        File file = fileChooser.showOpenDialog(null);
        if (file != null) {
            try (FileInputStream fis = new FileInputStream(file);
                 ObjectInputStream ois = new ObjectInputStream(fis)) {
                GeneratedNumberData loadedData = (GeneratedNumberData) ois.readObject();
                List<List<int[]>> partitionedNumbers = processLoadedData(loadedData);
                openBetslipsWindow(partitionedNumbers);
            } catch (IOException | ClassNotFoundException e) {
                showAlert("Error", "Cannot load coordinates: " + e.getMessage());
            }
        }
    }

    private List<List<int[]>> processLoadedData(GeneratedNumberData loadedData) {
        List<int[]> generatedNumbers = loadedData.getGeneratedNumbers();
        return partitionList(generatedNumbers, 5);
    }

    private List<List<int[]>> partitionList(List<int[]> list, int size) {
        List<List<int[]>> partitions = new ArrayList<>();
        for (int i = 0; i < list.size(); i += size) {
            partitions.add(list.subList(i, Math.min(i + size, list.size())));
        }
        return partitions;
    }

    private void openBetslipsWindow(List<List<int[]>> partitionedNumbers) {
        LotteryBetSlipController betslipsController = fxWeaver.loadController(LotteryBetSlipController.class);
        betslipsController.setData(partitionedNumbers, this.stateName, this.gameName);
        betslipsController.show();
    }

    @FXML
    private void saveNumbers() {
        if (currentData != null) {
            // Define the directory path
            String directoryPath = "Chosen Number Files/" + stateName;
            File directory = new File(directoryPath);

            // Ensure the directory exists
            if (!directory.exists()) {
                boolean dirCreated = directory.mkdirs();
                if (!dirCreated) {
                    showAlert("Error", "Failed to create directory: " + directoryPath);
                    return;
                }
            }

            // Get the current date and time
            LocalDateTime now = LocalDateTime.now();
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MM-dd-yyyy");
            String formattedNow = now.format(formatter);

            // Configure the FileChooser
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Save Chosen Numbers");
            fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Chosen Number Files", "*.ser"));
            fileChooser.setInitialDirectory(directory);

            // Set the initial file name with the current date
            fileChooser.setInitialFileName(gameName + "_chosen_numbers_" + formattedNow + ".ser");

            // Show the save dialog
            File file = fileChooser.showSaveDialog(null);
            if (file != null) {
                try (FileOutputStream fos = new FileOutputStream(file);
                     ObjectOutputStream oos = new ObjectOutputStream(fos)) {
                    oos.writeObject(currentData);
                    showAlert("Success", "Coordinates saved successfully.");
                } catch (IOException e) {
                    showAlert("Error", "Cannot save coordinates: " + e.getMessage());
                    e.printStackTrace(); // This will print the stack trace for debugging
                }
            }
        }
    }

    @Override
    public Mono<Void> setUpUi(String stateName, String gameName) {
        this.gameName = gameName;
        this.stateName = stateName;
        return Mono.empty();
    }
    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
    private RandomNumberGeneratorRequest createRequest() {
        Map<String, String> generatorMapper = new HashMap<>();
        generatorMapper.put("Random 1", "LOW_ODD_LOW_EVEN_HIGH_ODD_HIGH_EVEN");

        // Create and return the request object based on user input
        RandomNumberGeneratorRequest request = new RandomNumberGeneratorRequest();
        request.setLotteryGame(this.gameName);
        request.setLotteryState(this.stateName);
        request.setNumberGenerator(generatorMapper.get(rngComboBox.getValue()));
        request.setNumberSetsPerPattern(Integer.parseInt(numberSetPerPatternField.getText()));
        request.setDrawDaysPerWeek(Integer.parseInt(drawDaysPerWeekField.getText()));
        request.setTargetedPrizeLevel(Integer.parseInt(targetedPrizeLevelField.getText()));

        // Set properties of the request object
        return request;
    }
}
