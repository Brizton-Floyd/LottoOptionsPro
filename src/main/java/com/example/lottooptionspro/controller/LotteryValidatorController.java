package com.example.lottooptionspro.controller;

import com.example.lottooptionspro.GameInformation;
import com.example.lottooptionspro.presenter.LotteryValidatorPresenter;
import com.example.lottooptionspro.presenter.LotteryValidatorView;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.scene.text.Text;
import javafx.stage.FileChooser;
import net.rgielen.fxweaver.core.FxmlView;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Component
@FxmlView("/com.example.lottooptionspro/controller/LotteryValidator.fxml")
public class LotteryValidatorController implements GameInformation, LotteryValidatorView {

    @FXML
    private HBox winningNumbersContainer;

    @FXML
    private TableView<List<Integer>> ticketTable;
    @FXML
    private TableColumn<List<Integer>, String> ticketColumn;
    @FXML
    private TableColumn<List<Integer>, Integer> ticketNumberColumn;

    @FXML
    private TableView<LotteryValidatorPresenter.PrizeLevelResult> prizeTable;
    @FXML
    private TableColumn<LotteryValidatorPresenter.PrizeLevelResult, Integer> correctNumbersColumn;
    @FXML
    private TableColumn<LotteryValidatorPresenter.PrizeLevelResult, Integer> hitsColumn;

    private List<TextField> winningNumberFields = new ArrayList<>();
    private final LotteryValidatorPresenter presenter;

    public LotteryValidatorController() {
        this.presenter = new LotteryValidatorPresenter(this);
    }

    @FXML
    private void initialize() {
        ticketNumberColumn.setCellValueFactory(data ->
                new SimpleObjectProperty<>(ticketTable.getItems().indexOf(data.getValue()) + 1));

        ticketColumn.setCellValueFactory(data -> {
            List<Integer> ticket = data.getValue();
            return new SimpleStringProperty(formatTicket(ticket));
        });

        ticketColumn.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    String[] numbers = item.split(" ");
                    HBox hbox = new HBox(5);
                    for (String number : numbers) {
                        Text text = new Text(number.replace("-", ""));
                        if (number.startsWith("-")) {
                            text.setStyle("-fx-font-weight: bold; -fx-fill: red;");
                        }
                        hbox.getChildren().add(text);
                    }
                    setGraphic(hbox);
                }
            }
        });

        correctNumbersColumn.setCellValueFactory(new PropertyValueFactory<>("correctNumbers"));
        hitsColumn.setCellValueFactory(new PropertyValueFactory<>("hits"));
    }

    @FXML
    public void validateNumbers() {
        presenter.validateNumbers();
    }

    @FXML
    public void loadGameFile() {
        presenter.loadGameFile();
    }

    @Override
    public Mono<Void> setUpUi(String stateName, String gameName) {
        // This view is independent of the game selection for now
        return Mono.empty();
    }

    @Override
    public void createWinningNumberFields(int count) {
        winningNumbersContainer.getChildren().clear();
        winningNumberFields.clear();
        for (int i = 0; i < count; i++) {
            TextField field = new TextField();
            field.setPromptText("Num " + (i + 1));
            field.setPrefWidth(70);
            winningNumberFields.add(field);
            winningNumbersContainer.getChildren().add(field);
        }
    }

    @Override
    public List<String> getWinningNumbers() {
        return winningNumberFields.stream()
                .map(TextField::getText)
                .collect(Collectors.toList());
    }

    @Override
    public void updateTicketTable(List<List<Integer>> tickets) {
        ticketTable.getItems().setAll(tickets);
    }

    @Override
    public void updatePrizeTable(List<LotteryValidatorPresenter.PrizeLevelResult> results) {
        prizeTable.getItems().setAll(results);
    }

    @Override
    public File showOpenFileDialog() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Open Chosen Numbers File");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Chosen Number Files", "*.ser"));
        return fileChooser.showOpenDialog(null);
    }

    @Override
    public void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private String formatTicket(List<Integer> ticket) {
        return ticket.stream()
                .map(num -> (num < 0 ? "-" + Math.abs(num) : String.valueOf(num)))
                .collect(Collectors.joining(" "));
    }
}
