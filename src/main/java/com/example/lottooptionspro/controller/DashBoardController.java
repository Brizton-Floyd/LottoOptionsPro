package com.example.lottooptionspro.controller;

import com.example.lottooptionspro.GameInformation;
import com.example.lottooptionspro.service.DashboardService;
import com.example.lottooptionspro.util.ScreenManager;
import com.floyd.model.dashboard.DrawResultPattern;
import com.floyd.model.dashboard.LotteryNumber;
import com.floyd.model.response.DashboardResponse;
import javafx.application.Platform;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Button;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import net.rgielen.fxweaver.core.FxmlView;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.core.publisher.SignalType;
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

    public DashBoardController(ScreenManager screenManager, DashboardService dashboardService) {
        this.screenManager = screenManager;
        this.dashboardService = dashboardService;
    }

    @Override
    public Mono<Void> setUpUi(String stateName, String gameName) {
        return dashboardService.getDashboardData(stateName, gameName)
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
        System.out.println();

        for (int i = 0; i < 6; i++) {
            Pane pane = createBarChartPane();
            pane.getStyleClass().add("chart-pane");
            dynamicPanesContainer.getChildren().add(pane);
        }
    }

    private void handleDashboardDataError(Throwable error) {
        error.printStackTrace();
    }

    private void handleDashboardDataCompletion(SignalType signalType) {
//        System.out.println("Dashboard data retrieval completed.");
    }

    private Pane createBarChartPane() {
        CategoryAxis xAxis = new CategoryAxis();
        NumberAxis yAxis = new NumberAxis();
        BarChart<String, Number> barChart = new BarChart<>(xAxis, yAxis);

        XYChart.Series<String, Number> series1 = new XYChart.Series<>();
        series1.setName("Data Series 1");
        series1.getData().add(new XYChart.Data<>("Category 1", 50));
        series1.getData().add(new XYChart.Data<>("Category 2", 80));
        series1.getData().add(new XYChart.Data<>("Category 3", 30));

        barChart.getData().add(series1);

        Pane pane = new Pane(barChart);
        return pane;
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

        int idx = 0;
        for (int i = min; i <= max; i++, idx++) {
            TableColumn<ObservableList<Object>, Integer> column = new TableColumn<>("" + (i));
            final int colIndex = idx + 1; // Need a final variable for the lambda
            column.setSortable(false);
            column.setReorderable(false);
            column.setCellValueFactory(param -> new SimpleIntegerProperty(Integer.parseInt(param.getValue().get(colIndex).toString())).asObject());

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
