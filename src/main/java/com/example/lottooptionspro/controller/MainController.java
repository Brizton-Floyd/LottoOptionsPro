package com.example.lottooptionspro.controller;

import com.example.lottooptionspro.models.LotteryGame;
import com.example.lottooptionspro.util.ScreenManager;
import com.example.lottooptionspro.models.LotteryState;
import com.example.lottooptionspro.service.MainControllerService;
import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import org.apache.commons.text.WordUtils;
import org.springframework.stereotype.Component;
import net.rgielen.fxweaver.core.FxmlView;
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
        mainContentArea.getChildren().add(createLoadingOverlay()); // Show initial loading screen
        setupLotteryStatesAndGamesMenuOptions();
    }

    private Node createLoadingOverlay() {
        ProgressIndicator progressIndicator = new ProgressIndicator();
        Pane background = new Pane();
        background.setStyle("-fx-background-color: rgba(0, 0, 0, 0.4);");
        StackPane loadingOverlay = new StackPane(background, progressIndicator);
        loadingOverlay.setAlignment(Pos.CENTER);
        return loadingOverlay;
    }

    public void switchToBetslipTemplateEditor() {
        ToggleButton templateEditorButton = toggleButtonMap.get("Betslip Template Editor");
        if (templateEditorButton != null) {
            templateEditorButton.fire();
        }
    }

    @FXML
    private void showRandomNumberGenerator(ActionEvent actionEvent) {
        reEnableDisableButton(actionEvent);
        this.screenManager.loadView(RandomNumberGeneratorController.class, mainContentArea, stateName, gameName, createLoadingOverlay());
    }
    @FXML
    private void showDashboard(ActionEvent actionEvent) {
        reEnableDisableButton(actionEvent);
        this.screenManager.loadView(DashBoardController.class, mainContentArea, stateName, gameName, createLoadingOverlay());
    }

    @FXML
    private void showTemplateCreator(ActionEvent actionEvent) {
        reEnableDisableButton(actionEvent);
        this.screenManager.loadView(TemplateCreatorController.class, mainContentArea, createLoadingOverlay());
    }

    @FXML
    private void showWinCheck(ActionEvent actionEvent) {
        reEnableDisableButton(actionEvent);
        this.screenManager.loadView(LotteryValidatorController.class, mainContentArea, stateName, gameName, createLoadingOverlay());
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

    @FXML
    private void showTemplateCreatorHelp() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Betslip Template Editor Guide");
        alert.setHeaderText("How to Use the Betslip Template Editor");

        String helpContent = "This tool allows you to create and edit a JSON template that maps the coordinates of numbers and options on a lottery betslip image.\n\n" +
                "Step 1: Load Your Files\n" +
                "- Load Image: Click this to open a betslip image file (PNG or JPG). This is the first step.\n" +
                "- Load Template: Click this to open a previously saved JSON template file to continue editing.\n\n" +
                "Step 2: Define the Template\n" +
                "- Game Name & Jurisdiction: Fill these fields out first. They are used to suggest a filename when you save.\n\n" +
                "Step 3: Map the Coordinates\n" +
                "1. Select Panel: Choose the panel you are currently mapping (e.g., Panel A).\n" +
                "2. Select Mapping Mode:\n" +
                "    - Main/Bonus Number: Select this to map the numbered boxes. The \"Next Number\" field will auto-increment for you. You can type in a new starting number at any time.\n" +
                "    - Quick Pick: Select this to map the \"QP\" or \"Quick Pick\" box for the current panel.\n" +
                "    - Global Option: Select this for ticket-wide options like \"Power Play\" or \"Cash Value\". You must type a unique name for each one.\n" +
                "3. Click to Map: Click on the image to place a mark.\n\n" +
                "Step 4: Fine-Tune and Verify\n" +
                "- Move a Mark: Click and drag any existing transparent rectangle to adjust its position.\n" +
                "- Resize Marks: Use the \"Mark Width\" and \"Mark Height\" spinners to change the size of all marks in real-time.\n" +
                "- Undo: Click \"Clear Last Marking\" to remove the last action you took (either creating or moving a mark).\n" +
                "- Preview: Click this to test your work. A dialog will ask for numbers. Enter them (e.g., 5 10 15, 8) to see solid black marks appear at the mapped locations for the currently selected panel.\n\n" +
                "Step 5: Save Your Work\n" +
                "- Save: The first time you save, this will ask for a file location. After that, it will quickly overwrite the same file with your changes.\n" +
                "- Save As...: This will always ask for a new file name, allowing you to create copies.";

        TextArea textArea = new TextArea(helpContent);
        textArea.setEditable(false);
        textArea.setWrapText(true);

        textArea.setMaxWidth(Double.MAX_VALUE);
        textArea.setMaxHeight(Double.MAX_VALUE);
        GridPane.setVgrow(textArea, Priority.ALWAYS);
        GridPane.setHgrow(textArea, Priority.ALWAYS);

        GridPane expandableContent = new GridPane();
        expandableContent.setMaxWidth(Double.MAX_VALUE);
        expandableContent.add(textArea, 0, 0);

        alert.getDialogPane().setExpandableContent(expandableContent);
        alert.getDialogPane().setExpanded(true);

        alert.showAndWait();
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
                this::handleStateData,
                this::handleError,
                this::handleCompletion
        );
    }

    private void handleStateData(LotteryState state) {
        Platform.runLater(() -> {
            selectedStateAndGame.setText("Make Game Selection");
            Menu stateMenu = createStateMenu(state);
            lotteryState.getItems().addAll(stateMenu);
            setupToggleButtons();
        });
    }

    private Menu createStateMenu(LotteryState state) {
        Menu stateMenu = new Menu(WordUtils.capitalizeFully(state.getStateRegion()));
        state.getStateLotteryGames().forEach(lotteryGame -> {
            MenuItem item = createGameMenuItem(state, lotteryGame);
            stateMenu.getItems().add(item);
        });
        stateMenu.getItems().sort(Comparator.comparing(MenuItem::getText));
        return stateMenu;
    }

    private MenuItem createGameMenuItem(LotteryState state, LotteryGame lotteryGame) {
        MenuItem item = new MenuItem(lotteryGame.getFullName());
        item.setOnAction(event -> handleGameSelection(state, lotteryGame));
        return item;
    }

    private void handleGameSelection(LotteryState state, LotteryGame lotteryGame) {
        selectedStateAndGame.setText(WordUtils.capitalizeFully(state.getStateRegion()) + ": " + lotteryGame.getFullName());
        stateName = state.getStateRegion();
        gameName = lotteryGame.getFullName();
        ToggleButton toggleButton = toggleButtonMap.get(activeToggleButton);
        toggleButton.setDisable(false);
        toggleButtonMap.get(activeToggleButton).fire();
    }

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

    private void handleError(Throwable error) {
        Platform.runLater(() -> {
            error.printStackTrace();
            selectedStateAndGame.setText("Error: " + error.getMessage());
        });
    }

    private void handleCompletion() {
        Platform.runLater(() -> {
            // The loading overlay is now removed automatically by the ScreenManager when a view loads.
            // We just need to update the text.
            selectedStateAndGame.setText(WordUtils.capitalizeFully(stateName) + ": " + gameName);
            System.out.println("Completed fetching state games");
        });
    }
}
