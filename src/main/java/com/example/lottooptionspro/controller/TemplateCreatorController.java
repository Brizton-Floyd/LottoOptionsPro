package com.example.lottooptionspro.controller;

import com.example.lottooptionspro.models.BetslipTemplate;
import com.example.lottooptionspro.models.Coordinate;
import com.example.lottooptionspro.presenter.TemplateCreatorPresenter;
import com.example.lottooptionspro.presenter.TemplateCreatorView;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import net.rgielen.fxweaver.core.FxmlView;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.Optional;

@Component
@FxmlView("/com.example.lottooptionspro/controller/TemplateCreatorView.fxml")
public class TemplateCreatorController implements TemplateCreatorView {

    private TemplateCreatorPresenter presenter;

    @FXML
    private TextField gameNameField, jurisdictionField, globalOptionNameField, nextNumberField;
    @FXML
    private Label globalOptionNameLabel;
    @FXML
    private ImageView betslipImageView;
    @FXML
    private Pane drawingPane;
    @FXML
    private ComboBox<String> mappingModeComboBox, panelComboBox;
    @FXML
    private Spinner<Integer> markWidthSpinner, markHeightSpinner;

    private Node pressTarget;
    private boolean wasDragged = false;
    private double dragOffsetX, dragOffsetY;

    @FXML
    public void initialize() {
        this.presenter = new TemplateCreatorPresenter(new BetslipTemplate(), this);
        drawingPane.setOnMousePressed(this::paneMousePressed);
        drawingPane.setOnMouseDragged(this::paneMouseDragged);
        drawingPane.setOnMouseReleased(this::paneMouseReleased);
        populateComboBoxes();
        configureSpinners();
        addListeners();
        presenter.onPanelOrModeChanged();
    }

    private void populateComboBoxes() {
        mappingModeComboBox.setItems(FXCollections.observableArrayList("Main Number", "Bonus Number", "Quick Pick", "Global Option"));
        mappingModeComboBox.getSelectionModel().selectFirst();
        panelComboBox.setItems(FXCollections.observableArrayList("A", "B", "C", "D", "E"));
        panelComboBox.getSelectionModel().selectFirst();
    }

    private void configureSpinners() {
        markWidthSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 100, 20));
        markHeightSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 100, 20));
    }

    private void addListeners() {
        mappingModeComboBox.getSelectionModel().selectedItemProperty().addListener((obs, o, n) -> {
            updateVisibleControls(n);
            presenter.onPanelOrModeChanged();
        });
        panelComboBox.getSelectionModel().selectedItemProperty().addListener((obs, o, n) -> presenter.onPanelOrModeChanged());
        markWidthSpinner.valueProperty().addListener((obs, o, n) -> presenter.updateMarkSize(n, markHeightSpinner.getValue()));
        markHeightSpinner.valueProperty().addListener((obs, o, n) -> presenter.updateMarkSize(markWidthSpinner.getValue(), n));
    }

    private void updateVisibleControls(String mode) {
        boolean isGlobal = "Global Option".equals(mode);
        boolean isNumber = "Main Number".equals(mode) || "Bonus Number".equals(mode);
        globalOptionNameField.setVisible(isGlobal);
        globalOptionNameLabel.setVisible(isGlobal);
        nextNumberField.setVisible(isNumber);
    }

    private void paneMousePressed(MouseEvent event) {
        pressTarget = (Node) event.getTarget();
        wasDragged = false;
        if (pressTarget instanceof Rectangle) {
            Rectangle rect = (Rectangle) pressTarget;
            dragOffsetX = event.getX() - rect.getX();
            dragOffsetY = event.getY() - rect.getY();
            rect.setCursor(Cursor.MOVE);
            presenter.startCoordinateMove((Coordinate) rect.getUserData());
        }
        event.consume();
    }

    private void paneMouseDragged(MouseEvent event) {
        if (pressTarget instanceof Rectangle) {
            wasDragged = true;
            Rectangle rect = (Rectangle) pressTarget;
            rect.setX(event.getX() - dragOffsetX);
            rect.setY(event.getY() - dragOffsetY);
        }
        event.consume();
    }

    private void paneMouseReleased(MouseEvent event) {
        if (pressTarget instanceof Rectangle) {
            Rectangle rect = (Rectangle) pressTarget;
            if (wasDragged) {
                Coordinate coord = (Coordinate) rect.getUserData();
                int newX = (int) (rect.getX() + rect.getWidth() / 2);
                int newY = (int) (rect.getY() + rect.getHeight() / 2);
                presenter.finishCoordinateMove(coord, newX, newY);
            }
            rect.setCursor(Cursor.DEFAULT);
        } else if (pressTarget == drawingPane && !wasDragged) {
            presenter.onPaneClicked(event.getX(), event.getY());
        }
        pressTarget = null;
        wasDragged = false;
        event.consume();
    }

    @FXML
    private void saveTemplate() {
        presenter.saveTemplate();
    }

    @FXML
    private void saveTemplateAs() {
        presenter.saveTemplateAs();
    }

    @FXML
    private void loadTemplate() {
        presenter.loadTemplate();
    }

    @FXML
    private void previewTemplate() {
        presenter.previewTemplate();
    }

    @Override
    public File showSaveDialog(String initialFileName) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Save Template As");
        fileChooser.setInitialFileName(initialFileName);
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("JSON Files", "*.json"));
        return fileChooser.showSaveDialog(getStage());
    }

    @Override
    public Optional<String> askForPreviewNumbers() {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Live Preview");
        dialog.setHeaderText("Enter numbers to preview their plot locations.");
        dialog.setContentText("Numbers (e.g., 5 10 15, 8):");
        return dialog.showAndWait();
    }

    @Override
    public void drawRectangle(Coordinate coordinate, int width, int height) {
        double x = coordinate.getX() - (double) width / 2;
        double y = coordinate.getY() - (double) height / 2;
        Rectangle rect = new Rectangle(x, y, width, height);
        rect.setUserData(coordinate);
        rect.setFill(Color.TRANSPARENT);
        rect.setStroke(Color.BLACK);
        rect.setStrokeWidth(1);
        drawingPane.getChildren().add(rect);
    }

    @Override
    public void drawPreviewRectangle(Coordinate coordinate, int width, int height) {
        double x = coordinate.getX() - (double) width / 2;
        double y = coordinate.getY() - (double) height / 2;
        Rectangle rect = new Rectangle(x, y, width, height);
        rect.getStyleClass().add("preview-mark");
        rect.setFill(Color.BLACK);
        drawingPane.getChildren().add(rect);
    }

    @Override
    public void clearPreviewRectangles() {
        drawingPane.getChildren().removeIf(node -> node.getStyleClass().contains("preview-mark"));
    }

    @FXML
    private void clearLastMarking() { presenter.removeLastMarking(); }

    @FXML
    private void loadImage() { presenter.loadImage(); }

    @Override
    public void showView() { /* Handled by ScreenManager */ }

    @Override
    public File showOpenImageDialog() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Open Betslip Image");
        fileChooser.getExtensionFilters().addAll(new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg"));
        return fileChooser.showOpenDialog(getStage());
    }

    @Override
    public File showOpenTemplateDialog() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Open Template File");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("JSON Files", "*.json"));
        return fileChooser.showOpenDialog(getStage());
    }

    @Override
    public void displayImage(String imagePath) {
        betslipImageView.setImage(new javafx.scene.image.Image(imagePath));
    }

    @Override
    public void clearAllRectangles() {
        drawingPane.getChildren().removeIf(node -> node instanceof Rectangle);
    }

    @Override
    public String getGameName() { return gameNameField.getText(); }

    @Override
    public String getJurisdiction() { return jurisdictionField.getText(); }

    @Override
    public void setGameName(String name) { gameNameField.setText(name); }

    @Override
    public void setJurisdiction(String name) { jurisdictionField.setText(name); }

    @Override
    public String getNextNumber() { return nextNumberField.getText(); }

    @Override
    public void setNextNumber(String number) { nextNumberField.setText(number); }

    @Override
    public String getSelectedMappingMode() { return mappingModeComboBox.getValue(); }

    @Override
    public String getSelectedPanel() { return panelComboBox.getValue(); }

    @Override
    public String getGlobalOptionName() { return globalOptionNameField.getText(); }

    @Override
    public void showError(String message) { new Alert(Alert.AlertType.ERROR, message).showAndWait(); }

    @Override
    public void showSuccess(String message) { new Alert(Alert.AlertType.INFORMATION, message).showAndWait(); }

    private Stage getStage() { return (Stage) gameNameField.getScene().getWindow(); }
}
