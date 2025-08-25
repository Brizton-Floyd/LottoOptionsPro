package com.example.lottooptionspro.controller;

import com.example.lottooptionspro.GameInformation;
import com.example.lottooptionspro.presenter.RandomNumberGeneratorPresenter;
import com.example.lottooptionspro.presenter.RandomNumberGeneratorView;
import com.example.lottooptionspro.service.BetslipGenerationService;
import com.example.lottooptionspro.service.RandomNumberGeneratorService;
import com.floyd.model.generatednumbers.PrizeLevelResult;
import javafx.application.Platform;
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
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

@Component
@FxmlView("/com.example.lottooptionspro/controller/randomNumberGenerator.fxml")
public class RandomNumberGeneratorController implements GameInformation, RandomNumberGeneratorView {

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
    @FXML private TableColumn<PrizeLevelResult, Double> expectedElapsedDaysBeforeWinColumn;
    @FXML private VBox contentHolder;
    @FXML private Label totalTicketsLabel;
    @FXML private Label estimatedDaysLabel;
    @FXML private Button generateBetslipsButton;

    private final FxWeaver fxWeaver;
    private final RandomNumberGeneratorPresenter presenter;
    private final BetslipGenerationService betslipGenerationService;

    private String stateName;
    private String gameName;

    public RandomNumberGeneratorController(RandomNumberGeneratorService randomNumberGeneratorService, FxWeaver fxWeaver, BetslipGenerationService betslipGenerationService) {
        this.fxWeaver = fxWeaver;
        this.presenter = new RandomNumberGeneratorPresenter(this, randomNumberGeneratorService);
        this.betslipGenerationService = betslipGenerationService;
    }

    @FXML
    public void initialize() {
        rngComboBox.getItems().addAll("Random 1", "SecureRandom", "ThreadLocalRandom");
        numberSetColumn.setCellValueFactory(cellData -> new SimpleStringProperty(Arrays.toString(cellData.getValue())));
        correctNumbersColumn.setCellValueFactory(new PropertyValueFactory<>("correctNumbers"));
        hitsColumn.setCellValueFactory(new PropertyValueFactory<>("hits"));
        gamesOutColumn.setCellValueFactory(new PropertyValueFactory<>("gamesOut"));
        expectedElapsedDaysBeforeWinColumn.setCellValueFactory(new PropertyValueFactory<>("expectedElapsedDaysBeforeWin"));
    }

    @FXML
    private void generateNumbers() {
        presenter.generateNumbers();
    }

    @FXML
    private void generateBetslips() {
        boolean hasTemplate = betslipGenerationService.hasTemplateForGame(this.stateName, this.gameName);

        if (hasTemplate) {
            ObservableList<int[]> numberSets = generatedNumbersTable.getItems();
            if (numberSets == null || numberSets.isEmpty()) {
                showAlert("Info", "Please generate numbers first.");
                return;
            }

            betslipGenerationService.generatePdf(numberSets, stateName, gameName)
                    .doOnSubscribe(subscription -> Platform.runLater(() -> {
                        showProgress(true);
                        setContentDisabled(true);
                    }))
                    .doFinally(signalType -> Platform.runLater(() -> {
                        showProgress(false);
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
    private void saveNumbers() {
        presenter.saveNumbers();
    }

    @Override
    public Mono<Void> setUpUi(String stateName, String gameName) {
        this.stateName = stateName;
        this.gameName = gameName;
        presenter.setGameInfo(stateName, gameName);
        return Mono.empty();
    }

    @Override
    public String getSelectedRng() {
        return rngComboBox.getValue();
    }

    @Override
    public int getNumberSetPerPattern() {
        try {
            return Integer.parseInt(numberSetPerPatternField.getText());
        } catch (NumberFormatException e) {
            showAlert("Error", "Invalid input for Number Set Per Pattern.");
            return 0;
        }
    }

    @Override
    public int getTargetedPrizeLevel() {
        try {
            return Integer.parseInt(targetedPrizeLevelField.getText());
        } catch (NumberFormatException e) {
            showAlert("Error", "Invalid input for Targeted Prize Level.");
            return 0;
        }
    }

    @Override
    public int getDrawDaysPerWeek() {
        try {
            return Integer.parseInt(drawDaysPerWeekField.getText());
        } catch (NumberFormatException e) {
            showAlert("Error", "Invalid input for Draw Days Per Week.");
            return 0;
        }
    }

    @Override
    public void showProgress(boolean show) {
        progressIndicator.setVisible(show);
    }

    @Override
    public void setContentDisabled(boolean disabled) {
        contentHolder.setDisable(disabled);
    }

    @Override
    public void updateGeneratedNumbers(List<int[]> numbers) {
        generatedNumbersTable.setItems(FXCollections.observableArrayList(numbers));
        generateBetslipsButton.setDisable(numbers == null || numbers.isEmpty());
    }

    @Override
    public void updatePrizeLevelResults(List<PrizeLevelResult> results) {
        prizeLevelResultsTable.setItems(FXCollections.observableArrayList(results));
    }

    @Override
    public void updateTotalTickets(String total) {
        totalTicketsLabel.setText(total);
    }

    @Override
    public void updateEstimatedDays(String days) {
        estimatedDaysLabel.setText(days);
    }

    @Override
    public void openBetslipsWindow(List<List<int[]>> partitionedNumbers, String stateName, String gameName) {
        // Deprecated
    }

    @Override
    public File showSaveDialog(String initialDirectory, String initialFileName) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Save Chosen Numbers");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Chosen Number Files", "*.ser"));
        File directory = new File(initialDirectory);
        if (!directory.exists()) {
            directory.mkdirs();
        }
        fileChooser.setInitialDirectory(directory);
        fileChooser.setInitialFileName(initialFileName);
        return fileChooser.showSaveDialog(null);
    }

    @Override
    public File showOpenDialog(String initialDirectory) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Open Chosen Numbers File");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Chosen Number Files", "*.ser"));
        File directory = new File(initialDirectory);
        if (!directory.exists()) {
            directory.mkdirs();
        }
        fileChooser.setInitialDirectory(directory);
        return fileChooser.showOpenDialog(null);
    }

    @Override
    public File showSavePdfDialog(String initialFileName) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Save Betslips PDF");
        fileChooser.setInitialFileName(initialFileName);
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF Files", "*.pdf"));
        return fileChooser.showSaveDialog(null);
    }

    @Override
    public void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
