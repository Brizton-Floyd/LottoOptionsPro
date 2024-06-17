package com.example.lottooptionspro.util;

import com.example.lottooptionspro.GameInformation;
import javafx.scene.Node;
import javafx.scene.layout.StackPane;
import net.rgielen.fxweaver.core.FxControllerAndView;
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

    public <T extends GameInformation> void loadView(Class<T> controllerClass, StackPane contentArea, String stateName, String gameName) {
        FadeTransition fadeIn = new FadeTransition(Duration.millis(600), contentArea);
        fadeIn.setFromValue(0);
        fadeIn.setToValue(1);

        FxControllerAndView<T, Node> controllerAndView = fxWeaver.load(controllerClass);
        T controller = controllerAndView.getController();
        controller.setGameInformation(stateName, gameName);

        Node view = controllerAndView.getView().get();
        contentArea.getChildren().setAll(view);
        fadeIn.play();
    }
}
