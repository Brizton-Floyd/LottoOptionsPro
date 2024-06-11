package com.example.lottooptionspro.controller;

import com.example.lottooptionspro.util.ScreenManager;
import com.example.lottooptionspro.models.LotteryState;
import com.example.lottooptionspro.service.MainControllerService;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import org.springframework.stereotype.Component;
import net.rgielen.fxweaver.core.FxmlView;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import reactor.core.publisher.Flux;

import java.util.Comparator;

@Component
@FxmlView("/com.example.lottooptionspro/controller/main.fxml")
public class MainController {
    @FXML
    private StackPane mainContentArea;

    @FXML
    private MenuBar menuBar;

    @FXML
    private Menu lotteryState;

    @FXML
    private Label selectedStateAndGame;

    private final ScreenManager screenManager;
    private final MainControllerService mainControllerService;
    private ToggleButton lastClickedButton = null;
    private ProgressIndicator progressIndicator = new ProgressIndicator();

    public MainController(ScreenManager screenManager, MainControllerService mainControllerService) {
        this.screenManager = screenManager;
        this.mainControllerService = mainControllerService;
    }

    @FXML
    private void initialize() {
        progressIndicator.setVisible(true);
        mainContentArea.getChildren().add(progressIndicator);
        setupLotteryStatesAndGamesMenuOptions();
    }


    @FXML
    private void showRandomNumberGenerator(ActionEvent actionEvent) {
        reEnableDisableButton(actionEvent);
        mainContentArea.getChildren().setAll(new Label("Random Number Generator UI"));
    }
    @FXML
    private void showDashboard(ActionEvent actionEvent) {
        reEnableDisableButton(actionEvent);
        this.screenManager.loadView(DashBoardController.class, mainContentArea);
    }

    @FXML
    private void showPatternAnalysis(ActionEvent actionEvent) {
        reEnableDisableButton(actionEvent);
        mainContentArea.getChildren().setAll(new Label("Pattern Analysis UI"));
    }

    @FXML
    private void showLotteryWheelingSystem(ActionEvent actionEvent) {
        reEnableDisableButton(actionEvent);
        mainContentArea.getChildren().setAll(new Label("Lottery Wheeling System UI"));
    }

    @FXML
    private void showTicketPrinting(ActionEvent actionEvent) {
        reEnableDisableButton(actionEvent);
        mainContentArea.getChildren().setAll(new Label("Ticket Printing UI"));
    }

    @FXML
    private void showStatisticalAnalysisTools(ActionEvent actionEvent) {
        reEnableDisableButton(actionEvent);
        mainContentArea.getChildren().setAll(new Label("Statistical Analysis Tools UI"));
    }

    @FXML
    private void showHistoricalDataAndTrends(ActionEvent actionEvent) {
        reEnableDisableButton(actionEvent);
        mainContentArea.getChildren().setAll(new Label("Historical Data and Trends UI"));
    }

    @FXML
    private void showUserAccountManagement(ActionEvent actionEvent) {
        reEnableDisableButton(actionEvent);
        mainContentArea.getChildren().setAll(new Label("User Account Management UI"));
    }

    @FXML
    private void showRealTimeNotifications(ActionEvent actionEvent) {
        reEnableDisableButton(actionEvent);
        mainContentArea.getChildren().setAll(new Label("Real-Time Notifications UI"));
    }

    @FXML
    private void handleExit() {
        System.exit(0);
    }

    @FXML
    private void handleAbout() {
        // Show about dialog
    }

    private void reEnableDisableButton(ActionEvent event) {
        ToggleButton clickedButton = (ToggleButton) event.getSource();
        if (lastClickedButton != null) {
            lastClickedButton.setDisable(false);
        }
        clickedButton.setDisable(true);
        lastClickedButton = clickedButton;
    }

    private void setupLotteryStatesAndGamesMenuOptions() {
        Flux<LotteryState> stateFlux = mainControllerService.fetchStateGames();
        stateFlux.subscribe(
                state -> Platform.runLater(() -> {

                    // Update the UI with the state data
                    selectedStateAndGame.setText("Make Game Selection");
                    Menu stateMenu = new Menu(state.getStateRegion());
                    state.getStateLotteryGames().forEach(lotteryGame -> {
                        MenuItem item = new MenuItem(lotteryGame.getFullName());
                        item.setOnAction((actionEvent) -> {
                            selectedStateAndGame.setText(state.getStateRegion() + ": " + lotteryGame.getFullName());
                        });
                        stateMenu.getItems().add(item);
                    });
                    stateMenu.getItems()
                            .sort((Comparator.comparing(MenuItem::getText)));
                    lotteryState.getItems().addAll(stateMenu);
                }),
                error -> Platform.runLater(() -> {
                    // Handle the error
                    error.printStackTrace();
                    selectedStateAndGame.setText("Error: " + error.getMessage());
                }),
                () -> Platform.runLater(() -> {
                    // Handle the completion
                    progressIndicator.setVisible(false);
                    System.out.println("Completed fetching state games");
                })
        );
    }

}
