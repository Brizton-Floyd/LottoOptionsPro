package com.example.lottooptionspro.controller;


import com.example.lottooptionspro.GameInformation;
import com.floyd.model.generatednumbers.GeneratedNumberData;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import javafx.stage.FileChooser;
import net.rgielen.fxweaver.core.FxmlView;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.*;
import java.util.stream.Collectors;

@Component
@FxmlView("/com.example.lottooptionspro/controller/LotteryValidator.fxml")
public class LotteryValidatorController implements GameInformation {

    @FXML
    private HBox winningNumbersContainer;

    @FXML
    private TableView<List<Integer>> ticketTable;
    @FXML
    private TableColumn<List<Integer>, String> ticketColumn;
    @FXML
    private TableColumn<List<Integer>, Integer> ticketNumberColumn;

    @FXML
    private TableView<PrizeLevelResult> prizeTable;
    @FXML
    private TableColumn<PrizeLevelResult, Integer> correctNumbersColumn;
    @FXML
    private TableColumn<PrizeLevelResult, Integer> hitsColumn;

    private List<TextField> winningNumberFields;
    private List<List<Integer>> generatedNumbers;

    private List<PrizeLevelResult> prizeLevelResults;

    private void initialize() {
        ticketColumn.setCellFactory(column -> new TableCell<List<Integer>, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    String[] numbers = item.split(" ");
                    HBox hbox = new HBox(5); // 5 is the spacing between numbers
                    for (String number : numbers) {
                        Text text = new Text(number.replace("-", ""));
                        if (number.startsWith("-")) {
                            text.setStyle("-fx-font-weight: bold;");
                        }
                        hbox.getChildren().add(text);
                    }
                    setGraphic(hbox);
                }
            }
        });
        // Set up the ticket number column
        ticketNumberColumn.setCellValueFactory(data ->
                new SimpleObjectProperty<>(ticketTable.getItems().indexOf(data.getValue()) + 1));

        ticketColumn.setCellValueFactory(data -> {
            List<Integer> ticket = data.getValue();
            return new SimpleStringProperty(formatTicket(ticket));
        });

        int drawPositions = generatedNumbers.get(0).size();
        createWinningNumberFields(drawPositions);

        correctNumbersColumn.setCellValueFactory(new PropertyValueFactory<>("correctNumbers"));
        hitsColumn.setCellValueFactory(new PropertyValueFactory<>("hits"));

        ticketTable.getItems().setAll(generatedNumbers);
        prizeTable.getItems().setAll(prizeLevelResults);
    }

    private void createWinningNumberFields(int count) {
        winningNumberFields = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            TextField field = new TextField();
            field.setPromptText("Number " + (i + 1));
            field.setPrefWidth(70);
            winningNumberFields.add(field);
            winningNumbersContainer.getChildren().add(field);
        }
    }

    private String formatTicket(List<Integer> ticket) {
        return ticket.stream()
                .map(num -> (num < 0 ? "-" + Math.abs(num) : String.valueOf(num)))
                .collect(Collectors.joining(" "));
    }

    @FXML
    public void validateNumbers() {
        List<Integer> winningNumbers = new ArrayList<>();
        for (TextField field : winningNumberFields) {
            winningNumbers.add(Integer.parseInt(field.getText()));
        }

        ticketTable.getItems().clear();
        for (List<Integer> ticket : generatedNumbers) {
            List<Integer> displayTicket = new ArrayList<>();
            for (Integer number : ticket) {
                if (winningNumbers.contains(number)) {
                    displayTicket.add(-number); // Use negative to indicate bold
                } else {
                    displayTicket.add(number);
                }
            }
            ticketTable.getItems().add(displayTicket);
        }

        updatePrizeLevelResults(winningNumbers);
    }

    private void updatePrizeLevelResults(List<Integer> winningNumbers) {
        Map<Integer, Integer> hitCounts = new HashMap<>();
        for (List<Integer> ticket : generatedNumbers) {
            int matches = (int) ticket.stream().filter(winningNumbers::contains).count();
            hitCounts.put(matches, hitCounts.getOrDefault(matches, 0) + 1);
        }

        prizeTable.getItems().clear();
        for (PrizeLevelResult result : prizeLevelResults) {
            int hits = hitCounts.getOrDefault(result.getCorrectNumbers(), 0);
            prizeTable.getItems().add(new PrizeLevelResult(result.getCorrectNumbers(), hits));
        }
    }

    public void loadGameFile() {
        winningNumberFields.clear();
        winningNumbersContainer.getChildren().clear();
        loadGeneratedData();
        initialize();
    }
    @Override
    public Mono<Void> setUpUi(String stateName, String gameName) {
        loadGeneratedData();
        initialize();
        return Mono.empty();
    }

    private void loadGeneratedData() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Open Chosen Numbers File");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Chosen Number Files", "*.ser"));
        File file = fileChooser.showOpenDialog(null);
        if (file != null) {
            try (FileInputStream fis = new FileInputStream(file);
                 ObjectInputStream ois = new ObjectInputStream(fis)) {
                GeneratedNumberData loadedData = (GeneratedNumberData) ois.readObject();
                this.generatedNumbers = loadedData.getGeneratedNumbers().stream()
                        .map(array -> Arrays.stream(array)
                                .boxed()
                                .collect(Collectors.toList()))
                        .collect(Collectors.toList());
                this.prizeLevelResults = loadedData.getPrizeLevelResults().stream()
                        .map(data -> new PrizeLevelResult(data.correctNumbers, 0))
                        .collect(Collectors.toList());
            } catch (IOException | ClassNotFoundException e) {
                showAlert("Error", "Cannot load coordinates: " + e.getMessage());
            }
        }
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    public static class PrizeLevelResult {
        private final int correctNumbers;
        private final int hits;

        public PrizeLevelResult(int correctNumbers, int hits) {
            this.correctNumbers = correctNumbers;
            this.hits = hits;
        }

        public int getCorrectNumbers() { return correctNumbers; }
        public int getHits() { return hits; }
    }
}