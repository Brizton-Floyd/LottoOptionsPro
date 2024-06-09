package com.example.lottooptionspro.controller;

import com.example.lottooptionspro.ScreenManager;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.layout.BorderPane;
import net.rgielen.fxweaver.core.FxmlView;
import org.springframework.stereotype.Component;

@Component
@FxmlView("/com.example.lottooptionspro/controller/dashboard.fxml")
public class DashBoardController {
    @FXML
    private Button text;
    private ScreenManager screenManager;

    public DashBoardController(ScreenManager screenManager) {
        this.screenManager = screenManager;
    }

    @FXML
    private void clickAction() {
//        screenManager.loadView(MainController.class, "SettingsView.fxml");
    }
}
