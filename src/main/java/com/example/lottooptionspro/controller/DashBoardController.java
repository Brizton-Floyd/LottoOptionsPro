package com.example.lottooptionspro.controller;

import com.example.lottooptionspro.GameInformation;
import com.example.lottooptionspro.util.ScreenManager;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import net.rgielen.fxweaver.core.FxmlView;
import org.springframework.stereotype.Component;

@Component
@FxmlView("/com.example.lottooptionspro/controller/dashboard.fxml")
public class DashBoardController implements GameInformation  {
    @FXML
    private TableView<MyData> dynamicTable;

    @FXML
    private HBox dynamicPanesContainer;

    private ScreenManager screenManager;

    public DashBoardController(ScreenManager screenManager) {
        this.screenManager = screenManager;
    }

    @FXML
    public void initialize() {
        System.out.println("Im in initializer method");
        setUpDrawPatternTable();

        for (int i = 0; i < 6; i++) {
            Pane pane = createBarChartPane();
            pane.getStyleClass().add("chart-pane");
            dynamicPanesContainer.getChildren().add(pane);
        }
//        dynamicTable.getItems().add(new MyData("Data1", "Data2", "Data3"));
    }

    @Override
    public void setGameInformation(String stateName, String gameName) {
        System.out.println(stateName);
        System.out.println(gameName);
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
    private void setUpDrawPatternTable() {
        // Example data
        String[] columnNames = {"Column1", "Column2", "Column3"};
        ObservableList<MyData> items = FXCollections.observableArrayList();
        dynamicTable.setItems(items);

        // Create columns dynamically
        for (int i = 0; i < 6; i++) {
            for (String columnName : columnNames) {
                TableColumn<MyData, String> column = new TableColumn<>(columnName);
                column.setSortable(false);
                column.setReorderable(false);
                column.setCellValueFactory(new PropertyValueFactory<>(columnName.toLowerCase()));
                dynamicTable.getColumns().add(column);
            }
        }


        // Add listener to items list
        items.addListener((ListChangeListener<MyData>) change -> {
            while (change.next()) {
                if (change.wasAdded()) {
                    dynamicTable.scrollTo(items.size());
                }
            }
        });

        // Add data to the table (example)
        for (int i = 0; i < 100; i++) {
            items.add(new MyData("Data1", "Data2", "Data3"));
        }
    }

    @FXML
    public void handleAddRow(ActionEvent event) {

    }

    @FXML
    public void handleRemoveRow(ActionEvent event) {

    }
    // Example data class
    public static class MyData {
        private String column1;
        private String column2;
        private String column3;

        public MyData(String column1, String column2, String column3) {
            this.column1 = column1;
            this.column2 = column2;
            this.column3 = column3;
        }

        public String getColumn1() {
            return column1;
        }

        public void setColumn1(String column1) {
            this.column1 = column1;
        }

        public String getColumn2() {
            return column2;
        }

        public void setColumn2(String column2) {
            this.column2 = column2;
        }

        public String getColumn3() {
            return column3;
        }

        public void setColumn3(String column3) {
            this.column3 = column3;
        }
    }
}
