package com.example.lottooptionspro.controller;

import com.example.lottooptionspro.ScreenManager;
import com.example.lottooptionspro.models.LotteryState;
import com.example.lottooptionspro.service.MainControllerService;
import com.sun.tools.javac.Main;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import net.rgielen.fxweaver.core.FxWeaver;
import org.springframework.stereotype.Component;
import net.rgielen.fxweaver.core.FxmlView;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import reactor.core.publisher.Flux;

@Component
@FxmlView("/com.example.lottooptionspro/controller/main.fxml")
public class MainController {
    @FXML
    private StackPane mainContentArea;

    @FXML
    private Menu stateOneMenu;

    @FXML
    private Menu stateTwoMenu;

    @FXML
    private MenuBar menuBar;

    @FXML
    private Label selectedStateAndGame;

    private final ScreenManager screenManager;
    private final MainControllerService mainControllerService;

    public MainController(ScreenManager screenManager, MainControllerService mainControllerService) {
        this.screenManager = screenManager;
        this.mainControllerService = mainControllerService;
    }

    @FXML
    private void initialize() {
        Flux<LotteryState> stateFlux = mainControllerService.fetchStateGames();
        stateFlux.subscribe(
                state -> Platform.runLater(() -> {
                    // Update the UI with the state data
                    selectedStateAndGame.setText("State: " + state.getStateRegion() + ", Games: " + state.getStateLotteryGames().size());
                    // You can also update other parts of the UI with the state data
                }),
                error -> Platform.runLater(() -> {
                    // Handle the error
                    error.printStackTrace();
                    selectedStateAndGame.setText("Error: " + error.getMessage());
                }),
                () -> Platform.runLater(() -> {
                    // Handle the completion
                    System.out.println("Completed fetching state games");
                })
        );
    }

    @FXML
    private void showRandomNumberGenerator() {
        mainContentArea.getChildren().setAll(new Label("Random Number Generator UI"));
    }
    @FXML
    private void showDashboard() {
        this.screenManager.loadView(DashBoardController.class, mainContentArea);
    }

    @FXML
    private void showPatternAnalysis() {
        mainContentArea.getChildren().setAll(new Label("Pattern Analysis UI"));
    }

    @FXML
    private void showLotteryWheelingSystem() {
        mainContentArea.getChildren().setAll(new Label("Lottery Wheeling System UI"));
    }

    @FXML
    private void showTicketPrinting() {
        mainContentArea.getChildren().setAll(new Label("Ticket Printing UI"));
    }

    @FXML
    private void showStatisticalAnalysisTools() {
        mainContentArea.getChildren().setAll(new Label("Statistical Analysis Tools UI"));
    }

    @FXML
    private void showHistoricalDataAndTrends() {
        mainContentArea.getChildren().setAll(new Label("Historical Data and Trends UI"));
    }

    @FXML
    private void showUserAccountManagement() {
        mainContentArea.getChildren().setAll(new Label("User Account Management UI"));
    }

    @FXML
    private void showRealTimeNotifications() {
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

    @FXML
    private void selectGame(ActionEvent event) {
        MenuItem selectedItem = (MenuItem) event.getSource();
        String selectedGame = selectedItem.getText();
        Menu parentMenu = (Menu) selectedItem.getParentMenu();
        String selectedState = parentMenu.getText();
        selectedStateAndGame.setText(selectedState + ": " + selectedGame);
        // Perform additional actions based on the selected game
    }
}
