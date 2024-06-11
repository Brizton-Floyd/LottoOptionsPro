package com.example.lottooptionspro.util;

import javafx.scene.Node;
import javafx.scene.layout.StackPane;
import net.rgielen.fxweaver.core.FxWeaver;
import org.springframework.stereotype.Component;
import javafx.animation.FadeTransition;
import javafx.util.Duration;

@Component
public class ScreenManager {
    private final FxWeaver fxWeaver;

    public ScreenManager(FxWeaver fxWeaver) {
        this.fxWeaver = fxWeaver;
    }

    public void loadView(Class<?> controllerClass, StackPane contentArea) {
        FadeTransition fadeIn = new FadeTransition(Duration.millis(600), contentArea);
        fadeIn.setFromValue(0);
        fadeIn.setToValue(1);

        Node view = fxWeaver.loadView(controllerClass);
        contentArea.getChildren().setAll(view);
        fadeIn.play();
    }
}
