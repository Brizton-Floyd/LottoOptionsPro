package com.example.lottooptionspro.util;

import com.example.lottooptionspro.GameInformation;
import javafx.animation.FadeTransition;
import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.layout.StackPane;
import javafx.util.Duration;
import net.rgielen.fxweaver.core.FxControllerAndView;
import net.rgielen.fxweaver.core.FxWeaver;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

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

    public <T> void loadView(Class<T> controllerClass, StackPane contentArea, Node loadingIndicator) {
        contentArea.getChildren().setAll(loadingIndicator);
        FadeTransition fadeIn = new FadeTransition(Duration.millis(600), contentArea);
        fadeIn.setFromValue(0);
        fadeIn.setToValue(1);

        FxControllerAndView<T, Node> controllerAndView = fxWeaver.load(controllerClass);

        Platform.runLater(() -> {
            Node view = controllerAndView.getView().get();
            contentArea.getChildren().setAll(view);
            fadeIn.play();
        });
    }

    public <T> void loadView(Class<T> controllerClass, StackPane contentArea, 
                            String stateName, String gameName, Node loadingIndicator) {
        contentArea.getChildren().setAll(loadingIndicator);
        FadeTransition fadeIn = new FadeTransition(Duration.millis(600), contentArea);
        fadeIn.setFromValue(0);
        fadeIn.setToValue(1);

        FxControllerAndView<T, Node> controllerAndView = fxWeaver.load(controllerClass);
        T controller = controllerAndView.getController();

        // Handle GameInformation controllers (reactive with Mono)
        if (controller instanceof GameInformation) {
            ((GameInformation) controller).setUpUi(stateName, gameName)
                    .doOnSuccess(result -> {
                        Platform.runLater(() -> {
                            Node view = controllerAndView.getView().get();
                            contentArea.getChildren().setAll(view);
                            fadeIn.play();
                        });
                    })
                    .doOnError(error -> {
                        System.err.println("Error during setUpUi: " + error.getMessage());
                    })
                    .subscribe();
        } 
        // Handle ContextAware controllers (synchronous)
        else if (controller instanceof com.example.lottooptionspro.controller.ContextAware) {
            ((com.example.lottooptionspro.controller.ContextAware) controller).initializeWithContext(stateName, gameName);
            Platform.runLater(() -> {
                Node view = controllerAndView.getView().get();
                contentArea.getChildren().setAll(view);
                fadeIn.play();
            });
        } 
        // Fallback: just load the view without context initialization
        else {
            Platform.runLater(() -> {
                Node view = controllerAndView.getView().get();
                contentArea.getChildren().setAll(view);
                fadeIn.play();
            });
        }
    }
}
