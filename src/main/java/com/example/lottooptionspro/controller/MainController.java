package com.example.lottooptionspro.controller;

import com.example.lottooptionspro.models.LotteryGame;
import com.example.lottooptionspro.util.ScreenManager;
import com.example.lottooptionspro.models.LotteryState;
import com.example.lottooptionspro.service.MainControllerService;
import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import org.apache.commons.text.WordUtils;
import org.springframework.stereotype.Component;
import net.rgielen.fxweaver.core.FxmlView;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import reactor.core.publisher.Flux;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

@Component
@FxmlView("/com.example.lottooptionspro/controller/main.fxml")
public class MainController {
    @FXML
    private StackPane mainContentArea;

    @FXML
    private ToggleGroup toggleGroup;

    @FXML
    private MenuBar menuBar;

    @FXML
    private Menu lotteryState;

    @FXML
    private Label selectedStateAndGame;

    private Map<String, ToggleButton> toggleButtonMap = new HashMap<>();
    private final ScreenManager screenManager;
    private final MainControllerService mainControllerService;
    private ToggleButton lastClickedButton = null;
    private ProgressIndicator progressIndicator = new ProgressIndicator();
    private String stateName = "TEXAS";
    private String gameName = "Cash Five";
    private String activeToggleButton;

    public MainController(ScreenManager screenManager, MainControllerService mainControllerService) {
        this.screenManager = screenManager;
        this.mainControllerService = mainControllerService;
    }

    @FXML
    private void initialize() {
        screenManager.setContentArea(mainContentArea);
        progressIndicator.setVisible(true);
        mainContentArea.getChildren().add(progressIndicator);
        setupLotteryStatesAndGamesMenuOptions();
    }


    @FXML
    private void showRandomNumberGenerator(ActionEvent actionEvent) {
        progressIndicator.setVisible(true);
        reEnableDisableButton(actionEvent);
        this.screenManager.loadView(RandomNumberGeneratorController.class, mainContentArea, stateName, gameName, progressIndicator);
    }
    @FXML
    private void showDashboard(ActionEvent actionEvent) {
        progressIndicator.setVisible(true);
        reEnableDisableButton(actionEvent);
        this.screenManager.loadView(DashBoardController.class, mainContentArea, stateName, gameName, progressIndicator);
    }

    @FXML
    private void showBetlipPrccessor(ActionEvent actionEvent) {
        progressIndicator.setVisible(true);
        reEnableDisableButton(actionEvent);
        this.screenManager.loadView(LotteryBetslipCoordinateController.class, mainContentArea, stateName, gameName, progressIndicator);
    }

    @FXML
    private void showPreBetSlipProcessor(ActionEvent actionEvent) {
        progressIndicator.setVisible(true);
        reEnableDisableButton(actionEvent);
        this.screenManager.loadView(PreProcessBetSlipController.class, mainContentArea, stateName, gameName, progressIndicator);
    }

    @FXML
    private void showWinCheck(ActionEvent actionEvent) {
        progressIndicator.setVisible(true);
        reEnableDisableButton(actionEvent);
        this.screenManager.loadView(LotteryValidatorController.class, mainContentArea, stateName, gameName, progressIndicator);
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
        Platform.exit();
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

    /**
     * Sets up the lottery states and games menu options.
     * Fetches the state games from the main controller service and updates the UI accordingly.
     */
    private void setupLotteryStatesAndGamesMenuOptions() {
        Flux<LotteryState> stateFlux = mainControllerService.fetchStateGames();
        stateFlux.subscribe(
                this::handleStateData,
                this::handleError,
                this::handleCompletion
        );
    }

    /**
     * Handles the state data received from the main controller service.
     * Updates the UI with the state and game information.
     *
     * @param state The lottery state data.
     */
    private void handleStateData(LotteryState state) {
        Platform.runLater(() -> {
            selectedStateAndGame.setText("Make Game Selection");
            Menu stateMenu = createStateMenu(state);
            lotteryState.getItems().addAll(stateMenu);
            setupToggleButtons();
        });
    }

    /**
     * Creates a menu for the given lottery state.
     *
     * @param state The lottery state data.
     * @return The menu containing the state's lottery games.
     */
    private Menu createStateMenu(LotteryState state) {
        Menu stateMenu = new Menu(WordUtils.capitalizeFully(state.getStateRegion()));
        state.getStateLotteryGames().forEach(lotteryGame -> {
            MenuItem item = createGameMenuItem(state, lotteryGame);
            stateMenu.getItems().add(item);
        });
        stateMenu.getItems().sort(Comparator.comparing(MenuItem::getText));
        return stateMenu;
    }

    /**
     * Creates a menu item for the given lottery game.
     *
     * @param state       The lottery state data.
     * @param lotteryGame The lottery game data.
     * @return The menu item representing the lottery game.
     */
    private MenuItem createGameMenuItem(LotteryState state, LotteryGame lotteryGame) {
        MenuItem item = new MenuItem(lotteryGame.getFullName());
        item.setOnAction(event -> handleGameSelection(state, lotteryGame));
        return item;
    }

    /**
     * Handles the selection of a lottery game.
     *
     * @param state       The selected lottery state.
     * @param lotteryGame The selected lottery game.
     */
    private void handleGameSelection(LotteryState state, LotteryGame lotteryGame) {
        selectedStateAndGame.setText(WordUtils.capitalizeFully(state.getStateRegion()) + ": " + lotteryGame.getFullName());
        stateName = state.getStateRegion();
        gameName = lotteryGame.getFullName();
        ToggleButton toggleButton = toggleButtonMap.get(activeToggleButton);
        toggleButton.setDisable(false);
        toggleButtonMap.get(activeToggleButton).fire();
    }

    /**
     * Sets up the toggle buttons for the different views.
     */
    private void setupToggleButtons() {
        ObservableList<Toggle> toggles = toggleGroup.getToggles();
        toggles.forEach(toggle -> {
            ToggleButton toggleButton = (ToggleButton) toggle;
            toggleButtonMap.put(toggleButton.getText(), toggleButton);
            if (toggleButton.getText().equals("Dashboard")) {
                activeToggleButton = toggleButton.getText();
                toggleButton.fire();
            }
        });
    }

    /**
     * Handles any errors that occur during the state games fetching process.
     *
     * @param error The error that occurred.
     */
    private void handleError(Throwable error) {
        Platform.runLater(() -> {
            error.printStackTrace();
            selectedStateAndGame.setText("Error: " + error.getMessage());
        });
    }

    /**
     * Handles the completion of the state games fetching process.
     */
    private void handleCompletion() {
        Platform.runLater(() -> {
            progressIndicator.setVisible(false);
            selectedStateAndGame.setText(WordUtils.capitalizeFully(stateName) + ": " + gameName);
            System.out.println("Completed fetching state games");
        });
    }
}
