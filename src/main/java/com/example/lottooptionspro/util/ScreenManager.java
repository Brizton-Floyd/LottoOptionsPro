package com.example.lottooptionspro.util;

import com.example.lottooptionspro.GameInformation;
import javafx.animation.FadeTransition;
import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.layout.StackPane;
import javafx.util.Duration;
import net.rgielen.fxweaver.core.FxControllerAndView;
import net.rgielen.fxweaver.core.FxWeaver;
import org.springframework.stereotype.Component;

@Component
public class ScreenManager {
    private final FxWeaver fxWeaver;
    private StackPane contentArea;

    public ScreenManager(FxWeaver fxWeaver) {
        this.fxWeaver = fxWeaver;
    }

    public void setContentArea(StackPane contentArea) {
        this.contentArea = contentArea;
    }

    public <T extends GameInformation> void loadView(Class<T> controllerClass, StackPane contentArea, String stateName, String gameName, ProgressIndicator progressIndicator) {
        contentArea.getChildren().setAll(progressIndicator);
        FadeTransition fadeIn = new FadeTransition(Duration.millis(600), contentArea);
        fadeIn.setFromValue(0);
        fadeIn.setToValue(1);

        FxControllerAndView<T, Node> controllerAndView = fxWeaver.load(controllerClass);
        T controller = controllerAndView.getController();
        // Subscribe to the Mono and load the view only after setUpUi completes
        controller.setUpUi(stateName, gameName)
                .doOnSuccess(result -> {
                    Platform.runLater(() -> {
                        progressIndicator.setVisible(false);
                        Node view = controllerAndView.getView().get();
                        contentArea.getChildren().setAll(view);
                        fadeIn.play();

                    });
                })
                .doOnError(error -> {
                    // Handle error appropriately, e.g., show an error message
                    System.err.println("Error during setUpUi: " + error.getMessage());
                })
                .subscribe();
    }
}
