package com.example.lottooptionspro.controller;

import com.example.lottooptionspro.GameInformation;
import com.example.lottooptionspro.util.ScreenManager;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import net.rgielen.fxweaver.core.FxmlView;
import org.springframework.stereotype.Component;

@Component
@FxmlView("/com.example.lottooptionspro/controller/open.fxml")
public class OpenViewController implements GameInformation {
    @FXML
    private Button text;
    private ScreenManager screenManager;

    public OpenViewController(ScreenManager screenManager) {
        this.screenManager = screenManager;
    }

    @FXML
    private void clickAction() {
//        screenManager.loadView(MainController.class, "SettingsView.fxml");
    }

    @Override
    public void setGameInformation(String stateName, String gameName) {
    }
}
