package com.example.lottooptionspro.controller;

import com.example.lottooptionspro.GameInformation;
import com.example.lottooptionspro.service.DashboardService;
import com.example.lottooptionspro.util.ScreenManager;
import com.floyd.model.dashboard.DrawResultPattern;
import com.floyd.model.dashboard.LotteryNumber;
import com.floyd.model.dashboard.PatternAnalysisResult;
import com.floyd.model.response.DashboardResponse;
import javafx.application.Platform;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import net.rgielen.fxweaver.core.FxmlView;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.core.publisher.SignalType;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.util.Map;
import java.util.Stack;

import java.util.List;

@Component
@FxmlView("/com.example.lottooptionspro/controller/dashboard.fxml")
public class DashBoardController implements GameInformation  {
    @FXML
    private TableView<ObservableList<Object>> dynamicTable;

    @FXML
    private Button addBtn, removeBtn;

    @FXML
    private HBox dynamicPanesContainer;

    private ScreenManager screenManager;

    private DashboardService dashboardService;

    private final ObservableList<ObservableList<Object>> data = FXCollections.observableArrayList();
    private final Stack<ObservableList<Object>> removedItems = new Stack<>();
    private int totalElementsInList;
    private String gameName;

    public DashBoardController(ScreenManager screenManager, DashboardService dashboardService) {
        this.screenManager = screenManager;
        this.dashboardService = dashboardService;
    }

    @Override
    public Mono<Void> setUpUi(String stateName, String gameName) {
        this.gameName = gameName;
        return dashboardService.getDashboardData(stateName.toUpperCase(), gameName)
                .flatMap(this::updateUiWithDashboardData)
                .doOnError(this::handleDashboardDataError)
                .doFinally(this::handleDashboardDataCompletion)
                .then();
    }

    private Mono<Void> updateUiWithDashboardData(DashboardResponse dashboardResponse) {
        return Mono.fromRunnable(() -> Platform.runLater(() -> {
            setUpDrawPatternTable(dashboardResponse.getDrawResultPatterns());
            setUpProbabilityPanes(dashboardResponse);
        }));
    }

    private void setUpProbabilityPanes(DashboardResponse dashboardResponse) {
//        dynamicPanesContainer.setStyle("-fx-background-color: blue;");
        String[] patterns = new String[] {"oddEvenPatterns", "highLowPatterns"};
        for (int i = 0; i < patterns.length; i++) {
            Pane pane = createPanesWithData(dashboardResponse, patterns[i]);
            pane.getStyleClass().add("chart-pane");
            dynamicPanesContainer.getChildren().add(pane);
            HBox.setHgrow(pane, Priority.ALWAYS);
        }
//        int numChildren = dynamicPanesContainer.getChildren().size();
//        Pane node = (Pane)dynamicPanesContainer.getChildren().get(0);
//        Pane nodeTwo = (Pane)dynamicPanesContainer.getChildren().get(0);
//        node.prefWidthProperty().bind(dynamicPanesContainer.widthProperty().divide(numChildren));
//        nodeTwo.prefWidthProperty().bind(dynamicPanesContainer.widthProperty().divide(numChildren));
    }

    private void handleDashboardDataError(Throwable error) {
        error.printStackTrace();
    }

    private void handleDashboardDataCompletion(SignalType signalType) {
//        System.out.println("Dashboard data retrieval completed.");
    }

    private Pane createPanesWithData(DashboardResponse dashboardResponse, String pattern) {
        // Data
        String mainHeader = "Probability Estimation Compared to the Actual Results of the " + gameName + " Lotto " + dashboardResponse.getGameFormat();
        String subHeader = dashboardResponse.getHistoryBeginAndEndDates()[0] + " to " +
                dashboardResponse.getHistoryBeginAndEndDates ()[1] + "(" + dashboardResponse.getTotalDraws()+ " Draws)";

        // Create Labels
        Label mainHeaderLabel = new Label(mainHeader);
        Label subHeaderLabel = new Label(subHeader);

        // Style the labels (optional)
        mainHeaderLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-alignment: center; -fx-background-color: pink;");
        subHeaderLabel.setStyle("-fx-font-size: 12px; -fx-font-style: italic; -fx-text-alignment: center; -fx-background-color: pink;");

        // Create a VBox to hold the labels
        VBox vbox = new VBox(5); // 10 is the spacing between elements
        vbox.getChildren().addAll(mainHeaderLabel, subHeaderLabel, createProbabilityTable(dashboardResponse, pattern));
        vbox.setAlignment(Pos.CENTER);
        vbox.setFillWidth(true);

        vbox.setStyle("-fx-background-color: yellow;");


        AnchorPane.setTopAnchor(vbox, 0.0);
        AnchorPane.setBottomAnchor(vbox, 0.0);
        AnchorPane.setLeftAnchor(vbox, 0.0);
        AnchorPane.setRightAnchor(vbox, 0.0);

        AnchorPane pane = new AnchorPane(vbox);
        return pane;
    }

    private Node createProbabilityTable(DashboardResponse dashboardResponse, String pattern) {
        TableView<PatternAnalysisResult> tableView = new TableView<>();
        tableView.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        try {
            Class<? extends DashboardResponse> clazz = dashboardResponse.getClass();
            Field declaredField = clazz.getDeclaredField(pattern);
            declaredField.setAccessible(true);
            Map<String, PatternAnalysisResult> patternData = (Map<String, PatternAnalysisResult>) declaredField.get(dashboardResponse);

            ObservableList<PatternAnalysisResult> data = FXCollections.observableArrayList();
            for (Map.Entry<String, PatternAnalysisResult> entry : patternData.entrySet()) {
                PatternAnalysisResult patternObject = entry.getValue();
                data.add(patternObject);
            }

            PatternAnalysisResult value = patternData.values().iterator().next();

            TableColumn<PatternAnalysisResult, Object> patternColumn = new TableColumn<>("Combinatorial Patterns");
            patternColumn.setCellValueFactory(new PropertyValueFactory<>("pattern"));

            TableColumn<PatternAnalysisResult, Double> probabilityColumn = new TableColumn<>("Probability");
            probabilityColumn.setCellValueFactory(new PropertyValueFactory<>("probability"));

            TableColumn<PatternAnalysisResult, Double> estimatedHitFrequencyColumn = new TableColumn<>("Estimated Hit Frequency");
            estimatedHitFrequencyColumn.setCellValueFactory(new PropertyValueFactory<>("estimatedHitFrequency"));

            TableColumn<PatternAnalysisResult, Integer> frequencyColumn = new TableColumn<>("Frequency");
            frequencyColumn.setCellValueFactory(new PropertyValueFactory<>("frequency"));

            TableColumn<PatternAnalysisResult, Integer> gamesSinceLastAppearanceColumn = new TableColumn<>("Games Since Last Appearance");
            gamesSinceLastAppearanceColumn.setCellValueFactory(new PropertyValueFactory<>("gamesSinceLastAppearance"));

            tableView.getColumns().addAll(patternColumn, frequencyColumn, probabilityColumn, estimatedHitFrequencyColumn, gamesSinceLastAppearanceColumn);
            tableView.setItems(data);

        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }

        return tableView;
    }

    private void setUpDrawPatternTable(List<DrawResultPattern> drawResultPatterns) {

        for (DrawResultPattern pattern : drawResultPatterns) {
            ObservableList<Object> row = FXCollections.observableArrayList();
            row.add(pattern.getDrawDate());
            for (LotteryNumber lotteryNumber : pattern.getLotteryNumbers()) {
                row.add(lotteryNumber.getGamesOut());
            }
            data.add(row);
        }

        TableColumn<ObservableList<Object>, String> drawDateColumn = new TableColumn<>("Draw Date");
        drawDateColumn.setCellValueFactory(param -> new SimpleStringProperty(param.getValue().get(0).toString()));
        dynamicTable.getColumns().add(drawDateColumn);

        int min = Integer.MAX_VALUE;
        int max = Integer.MIN_VALUE;
        for (LotteryNumber lotteryNumber : drawResultPatterns.get(0).getLotteryNumbers()) {
            min = Math.min(min, lotteryNumber.getNumber());
            max = Math.max(max, lotteryNumber.getNumber());
        }

        if (max == 9) {
            dynamicTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        }

        int idx = 1;
        for (int i = min; i <= max; i++, idx++) {
            TableColumn<ObservableList<Object>, Integer> column = new TableColumn<>("" + (i));
            final int colIndex = idx; // Need a final variable for the lambda
            column.setSortable(false);
            column.setReorderable(false);
            column.setCellValueFactory(param -> {
                ObservableList<Object> values = param.getValue();
                if (colIndex >= 0 && colIndex < values.size()) {
                    Object value = values.get(colIndex);
                    if (value != null) {
                        try {
                            return new SimpleIntegerProperty(Integer.parseInt(value.toString())).asObject();
                        } catch (NumberFormatException e) {
                            // Handle the exception, e.g., log the error or return a default value

                        }
                    }
                }
                // Return a default value if the index is out of bounds or the value is null
                return null;
            });
            // Custom cell factory
            column.setCellFactory(col -> {
                return new TableCell<ObservableList<Object>, Integer>() {
                    @Override
                    protected void updateItem(Integer item, boolean empty) {
                        super.updateItem(item, empty);
                        if (item == null || empty) {
                            setText(null);
                            setStyle("");
                        } else {
                            if (item == 0) {
                                setText("#");
                                setStyle("-fx-text-fill: orange;"); // Set text color to yellow
                            } else {
                                setText(item.toString());
                                setStyle("");
                            }
                        }
                    }
                };
            });

            dynamicTable.getColumns().add(column);
        }

        this.totalElementsInList = data.size();
        dynamicTable.setItems(data);

        // Scroll to the last row
        dynamicTable.scrollTo(data.size());
        addBtn.setDisable(true);
    }

    @FXML
    public void handleAddRow(ActionEvent event) {
        if (!removedItems.isEmpty()) {
            // Add back the last removed item
            data.add(removedItems.pop());
            totalElementsInList++;
            removeBtn.setDisable(false); // Enable the remove button if it was disabled
        }
        // Disable the add button if there are no more items to add back
        if (removedItems.isEmpty()) {
            addBtn.setDisable(true);
        }
        // Scroll to the last row
        dynamicTable.scrollTo(data.size());
    }

    @FXML
    public void handleRemoveRow(ActionEvent event) {
        if (totalElementsInList > 0) {
            // Remove the last item from the data list and push it onto the stack
            removedItems.push(data.remove(totalElementsInList - 1));
            totalElementsInList--;
            addBtn.setDisable(false); // Enable the add button if it was disabled
        }
        // Disable the remove button if there are no more items to remove
        if (totalElementsInList == 0) {
            removeBtn.setDisable(true);
        }
        // Scroll to the last row
        dynamicTable.scrollTo(data.size());
    }
}
