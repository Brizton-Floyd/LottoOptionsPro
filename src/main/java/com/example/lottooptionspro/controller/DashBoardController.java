package com.example.lottooptionspro.controller;

import com.example.lottooptionspro.GameInformation;
import com.example.lottooptionspro.presenter.DashboardPresenter;
import com.example.lottooptionspro.presenter.DashboardView;
import com.floyd.model.dashboard.DrawResultPattern;
import com.floyd.model.dashboard.LotteryNumber;
import com.floyd.model.dashboard.PatternAnalysisResult;
import com.floyd.model.response.DashboardResponse;
import jakarta.annotation.PostConstruct;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import net.rgielen.fxweaver.core.FxmlView;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;
import java.util.Stack;

@Component
@FxmlView("/com.example.lottooptionspro/controller/dashboard.fxml")
public class DashBoardController implements GameInformation, DashboardView {
    @FXML
    private TableView<ObservableList<Object>> dateColumnTable;
    @FXML
    private TableView<ObservableList<Object>> numberColumnsTable;
    @FXML
    private Button addBtn, removeBtn;
    @FXML
    private HBox dynamicPanesContainer;
    @FXML
    private HBox legendContainer;

    private final DashboardPresenter presenter;
    private final ObservableList<ObservableList<Object>> data = FXCollections.observableArrayList();
    private final Stack<ObservableList<Object>> removedItems = new Stack<>();
    private int totalElementsInList;

    @Autowired
    public DashBoardController(DashboardPresenter presenter) {
        this.presenter = presenter;
    }

    @PostConstruct
    public void init() {
        presenter.setView(this);
    }

    @Override
    public Mono<Void> setUpUi(String stateName, String gameName) {
        return presenter.loadDashboardData(stateName, gameName);
    }

    @Override
    public void setUpDrawPatternTable(List<DrawResultPattern> drawResultPatterns) {
        data.clear();
        dateColumnTable.getColumns().clear();
        numberColumnsTable.getColumns().clear();

        drawResultPatterns.forEach(pattern -> {
            ObservableList<Object> row = FXCollections.observableArrayList();
            row.add(pattern.getDrawDate());
            pattern.getLotteryNumbers().forEach(lotteryNumber -> row.add(lotteryNumber.getGamesOut()));
            data.add(row);
        });

        TableColumn<ObservableList<Object>, String> drawDateColumn = new TableColumn<>("Draw Date");
        drawDateColumn.setCellValueFactory(param -> new SimpleStringProperty(param.getValue().get(0).toString()));
        drawDateColumn.setSortable(false);
        drawDateColumn.setReorderable(false);
        dateColumnTable.getColumns().add(drawDateColumn);

        numberColumnsTable.setColumnResizePolicy(TableView.UNCONSTRAINED_RESIZE_POLICY);

        int min = drawResultPatterns.get(0).getLotteryNumbers().stream().mapToInt(LotteryNumber::getNumber).min().orElse(1);
        int max = drawResultPatterns.get(0).getLotteryNumbers().stream().mapToInt(LotteryNumber::getNumber).max().orElse(1);

        for (int i = min; i <= max; i++) {
            final int colIndex = (i - min) + 1;
            TableColumn<ObservableList<Object>, Integer> column = new TableColumn<>("" + i);
            column.setSortable(false);
            column.setReorderable(false);
            column.setCellValueFactory(param -> {
                ObservableList<Object> values = param.getValue();
                if (colIndex < values.size() && values.get(colIndex) != null) {
                    return new SimpleIntegerProperty(Integer.parseInt(values.get(colIndex).toString())).asObject();
                }
                return null;
            });

            column.setCellFactory(col -> new TableCell<>() {
                @Override
                protected void updateItem(Integer item, boolean empty) {
                    super.updateItem(item, empty);
                    if (item == null || empty) {
                        setText(null);
                        setStyle("");
                    } else {
                        setText(item == 0 ? "#" : item.toString());
                        setStyle(item == 0 ? "-fx-text-fill: orange; -fx-font-weight: bold;" : "");
                    }
                }
            });
            numberColumnsTable.getColumns().add(column);
        }

        totalElementsInList = data.size();
        dateColumnTable.setItems(data);
        numberColumnsTable.setItems(data);

        synchronizeScrollbars();

        Platform.runLater(() -> {
            numberColumnsTable.scrollTo(data.size());
            dateColumnTable.scrollTo(data.size());
        });
        addBtn.setDisable(true);
    }

    @Override
    public void setUpProbabilityPanes(DashboardResponse dashboardResponse) {
        dynamicPanesContainer.getChildren().clear();

        VBox probabilityPanel = new VBox(10);
        probabilityPanel.getStyleClass().add("chart-pane");
        HBox.setHgrow(probabilityPanel, Priority.ALWAYS);
        probabilityPanel.setAlignment(Pos.TOP_CENTER);

        String mainHeaderText = "Probability Estimation";
        String subHeaderText = dashboardResponse.getHistoryBeginAndEndDates()[0] + " to " +
                dashboardResponse.getHistoryBeginAndEndDates()[1] + " (" + dashboardResponse.getTotalDraws() + " Draws)";
        Label mainHeaderLabel = new Label(mainHeaderText);
        mainHeaderLabel.getStyleClass().add("chart-header-main");

        Label subHeaderLabel = new Label(subHeaderText);
        subHeaderLabel.getStyleClass().add("chart-header-sub");

        ComboBox<String> patternSelector = new ComboBox<>();
        patternSelector.getItems().addAll("Odd/Even Patterns", "High/Low Patterns");
        patternSelector.setMaxWidth(Double.MAX_VALUE);

        StackPane tableContainer = new StackPane();
        VBox.setVgrow(tableContainer, Priority.ALWAYS);

        patternSelector.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal == null) return;

            String patternKey = "oddEvenPatterns";
            if (newVal.equals("High/Low Patterns")) {
                patternKey = "highLowPatterns";
            }

            Node probabilityTable = createProbabilityTable(dashboardResponse, patternKey);
            tableContainer.getChildren().setAll(probabilityTable);
        });

        probabilityPanel.getChildren().addAll(mainHeaderLabel, subHeaderLabel, patternSelector, tableContainer);
        dynamicPanesContainer.getChildren().add(probabilityPanel);
        patternSelector.getSelectionModel().selectFirst();
    }

    private Node createProbabilityTable(DashboardResponse dashboardResponse, String pattern) {
        TableView<PatternAnalysisResult> tableView = new TableView<>();
        tableView.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        try {
            Field declaredField = dashboardResponse.getClass().getDeclaredField(pattern);
            declaredField.setAccessible(true);
            Map<String, PatternAnalysisResult> patternData = (Map<String, PatternAnalysisResult>) declaredField.get(dashboardResponse);

            ObservableList<PatternAnalysisResult> tableData = FXCollections.observableArrayList(patternData.values());

            tableView.getColumns().add(createColumn("Combinatorial Patterns", "pattern"));
            tableView.getColumns().add(createColumn("Frequency", "frequency"));
            tableView.getColumns().add(createColumn("Probability", "probability"));
            tableView.getColumns().add(createColumn("Estimated Hit Frequency", "estimatedHitFrequency"));
            tableView.getColumns().add(createColumn("Games Since Last Appearance", "gamesSinceLastAppearance"));

            tableView.setItems(tableData);

            // Dynamically set the table height based on its content to "shrink-wrap" it.
            tableView.setFixedCellSize(30);
            tableView.prefHeightProperty().bind(
                    tableView.fixedCellSizeProperty().multiply(Bindings.size(tableView.getItems()).add(1.05))
            );
            tableView.minHeightProperty().bind(tableView.prefHeightProperty());
            tableView.maxHeightProperty().bind(tableView.prefHeightProperty());

        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }

        return tableView;
    }

    private <S, T> TableColumn<S, T> createColumn(String title, String property) {
        TableColumn<S, T> column = new TableColumn<>(title);
        column.setCellValueFactory(new PropertyValueFactory<>(property));
        return column;
    }

    @Override
    public void setUpLegend() {
        legendContainer.getChildren().clear();

        Label hitSymbol = new Label("#");
        hitSymbol.setStyle("-fx-text-fill: orange; -fx-font-weight: bold; -fx-font-size: 14px;");

        Label hitText = new Label("= Number Hit");
        hitText.getStyleClass().add("legend-text");

        Label gamesOut = new Label("1, 2, 3...");
        gamesOut.getStyleClass().add("legend-text");
        gamesOut.setStyle("-fx-padding: 0 0 0 10px;");

        Label gamesOutText = new Label("= Games Out Since Last Hit");
        gamesOutText.getStyleClass().add("legend-text");

        legendContainer.getChildren().addAll(hitSymbol, hitText, gamesOut, gamesOutText);
    }

    @Override
    public void showDataError(Throwable error) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Error");
            alert.setHeaderText("Failed to load dashboard data");
            alert.setContentText(error.getMessage());
            alert.showAndWait();
        });
    }

    private void synchronizeScrollbars() {
        numberColumnsTable.skinProperty().addListener((obs, oldSkin, newSkin) -> {
            if (newSkin != null) {
                bindScrollBars();
                ScrollBar hBar = findHorizontalScrollBar(numberColumnsTable);
                if (hBar != null) {
                    Runnable updatePadding = () -> {
                        double height = hBar.isVisible() ? hBar.getHeight() : 0;
                        dateColumnTable.setPadding(new Insets(0, 0, height, 0));
                    };
                    hBar.visibleProperty().addListener(o -> Platform.runLater(updatePadding));
                    hBar.heightProperty().addListener(o -> Platform.runLater(updatePadding));
                    Platform.runLater(updatePadding);
                }
            }
        });
        dateColumnTable.skinProperty().addListener((obs, oldSkin, newSkin) -> {
            if (newSkin != null) {
                bindScrollBars();
            }
        });
    }

    private void bindScrollBars() {
        ScrollBar dateVBar = findVerticalScrollBar(dateColumnTable);
        ScrollBar numberVBar = findVerticalScrollBar(numberColumnsTable);
        if (dateVBar != null && numberVBar != null) {
            dateVBar.valueProperty().bindBidirectional(numberVBar.valueProperty());
            dateVBar.setOpacity(0);
        }
    }

    private ScrollBar findVerticalScrollBar(TableView<?> tableView) {
        for (Node node : tableView.lookupAll(".scroll-bar")) {
            if (node instanceof ScrollBar && ((ScrollBar) node).getOrientation() == Orientation.VERTICAL) {
                return (ScrollBar) node;
            }
        }
        return null;
    }

    private ScrollBar findHorizontalScrollBar(TableView<?> tableView) {
        for (Node node : tableView.lookupAll(".scroll-bar")) {
            if (node instanceof ScrollBar && ((ScrollBar) node).getOrientation() == Orientation.HORIZONTAL) {
                return (ScrollBar) node;
            }
        }
        return null;
    }

    @FXML
    public void handleAddRow(ActionEvent event) {
        if (!removedItems.isEmpty()) {
            data.add(removedItems.pop());
            totalElementsInList++;
            removeBtn.setDisable(false);
        }
        if (removedItems.isEmpty()) {
            addBtn.setDisable(true);
        }
        Platform.runLater(() -> {
            numberColumnsTable.scrollTo(data.size());
            dateColumnTable.scrollTo(data.size());
        });
    }

    @FXML
    public void handleRemoveRow(ActionEvent event) {
        if (totalElementsInList > 0) {
            removedItems.push(data.remove(totalElementsInList - 1));
            totalElementsInList--;
            addBtn.setDisable(false);
        }
        if (totalElementsInList == 0) {
            removeBtn.setDisable(true);
        }
        Platform.runLater(() -> {
            numberColumnsTable.scrollTo(data.size());
            dateColumnTable.scrollTo(data.size());
        });
    }
}
