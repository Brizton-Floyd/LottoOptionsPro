package com.example.lottooptionspro.controller;

import com.example.lottooptionspro.ScreenManager;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.BorderPane;
import net.rgielen.fxweaver.core.FxmlView;
import org.springframework.stereotype.Component;

@Component
@FxmlView("/com.example.lottooptionspro/controller/dashboard.fxml")
public class DashBoardController {
    @FXML
    private TableView<MyData> dynamicTable;

    private ScreenManager screenManager;

    public DashBoardController(ScreenManager screenManager) {
        this.screenManager = screenManager;
    }

    @FXML
    public void initialize() {
        // Example data
        String[] columnNames = {"Column1", "Column2", "Column3"};

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


        // Add data to the table (example)
        dynamicTable.getItems().add(new MyData("Data1", "Data2", "Data3"));
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
