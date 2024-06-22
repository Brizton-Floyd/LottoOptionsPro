package com.example.lottooptionspro.controller;

import com.example.lottooptionspro.GameInformation;
import com.example.lottooptionspro.util.ImageUtils;
import com.example.lottooptionspro.util.LotteryBetslipProcessor;
import javafx.beans.binding.Bindings;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import net.rgielen.fxweaver.core.FxmlView;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import javax.imageio.ImageIO;
import java.io.File;
import java.io.IOException;

@Component
@FxmlView("/com.example.lottooptionspro/controller/LotteryBetslipView.fxml")
public class LotteryBetslipController implements GameInformation {

    @FXML private ImageView imageView;
    @FXML private ScrollPane scrollPane;
    @FXML private Slider mainBallHorizontalSlider;
    @FXML private Slider bonusBallHorizontalSlider;
    @FXML private Slider verticalSlider;
    @FXML private Slider sizeSlider;
    @FXML private Slider offsetXSlider;
    @FXML private Slider offsetYSlider;
    @FXML private TextField stateField;
    @FXML private TextField gameField;
    @FXML private TextField panelCountField;
    @FXML private TextField mainBallRowsField;
    @FXML private TextField bonusBallRowsField;
    @FXML private TextField mainBallColumnsField;
    @FXML private TextField bonusBallColumnsField;
    @FXML private TextField mainBallHorizontalSpacingField;
    @FXML private TextField bonusBallHorizontalSpacingField;
    @FXML private TextField verticalSpacingField;
    @FXML private TextField markingSizeField;
    @FXML private TextField offsetXField;
    @FXML private TextField offsetYField;
    @FXML private TextField xOffsetsField;
    @FXML private TextField yOffsetsField;
    @FXML private TextField bonusXOffsetsField;
    @FXML private TextField bonusYOffsetsField;
    @FXML private CheckBox bonusGameCheckBox;
    @FXML private VBox bonusFields;
    @FXML private Button loadImage;
    @FXML private HBox root;
    private LotteryBetslipProcessor processor;

    public void initialize() {
        // Bind the preferred width of the ScrollPane to 40% of the HBox's width
        scrollPane.prefWidthProperty().bind(root.widthProperty().multiply(0.4));

        // Ensure the VBox takes the remaining 60% of the width
        HBox.setHgrow(scrollPane, Priority.NEVER);

        bindSliderToTextField(mainBallHorizontalSlider, mainBallHorizontalSpacingField);
        bindSliderToTextField(bonusBallHorizontalSlider, bonusBallHorizontalSpacingField);
        bindSliderToTextField(verticalSlider, verticalSpacingField);
        bindSliderToTextField(sizeSlider, markingSizeField);
        bindSliderToTextField(offsetXSlider, offsetXField);
        bindSliderToTextField(offsetYSlider, offsetYField);

        mainBallHorizontalSlider.valueProperty().addListener((obs, oldVal, newVal) -> updateProcessor());
        bonusBallHorizontalSlider.valueProperty().addListener((obs, oldVal, newVal) -> updateProcessor());
        verticalSlider.valueProperty().addListener((obs, oldVal, newVal) -> updateProcessor());
        sizeSlider.valueProperty().addListener((obs, oldVal, newVal) -> updateProcessor());
        offsetXSlider.valueProperty().addListener((obs, oldVal, newVal) -> updateProcessor());
        offsetYSlider.valueProperty().addListener((obs, oldVal, newVal) -> updateProcessor());

        // Add listeners to xOffsetsField, yOffsetsField, bonusXOffsetsField, and bonusYOffsetsField
        xOffsetsField.textProperty().addListener((observable, oldValue, newValue) -> validateAndUpdateProcessor());
        yOffsetsField.textProperty().addListener((observable, oldValue, newValue) -> validateAndUpdateProcessor());
        bonusXOffsetsField.textProperty().addListener((observable, oldValue, newValue) -> validateAndUpdateProcessor());
        bonusYOffsetsField.textProperty().addListener((observable, oldValue, newValue) -> validateAndUpdateProcessor());


        // Add zooming functionality for mouse scroll
        scrollPane.addEventFilter(ScrollEvent.SCROLL, event -> {
            if (event.isControlDown()) {
                double zoomFactor = event.getDeltaY() > 0 ? 1.1 : 0.9;
                zoom(zoomFactor);
                event.consume();
            }
        });

        // Add key press event handler for zooming
        scrollPane.setOnKeyPressed(event -> {
            if (event.isControlDown()) {
                if (event.getCode() == KeyCode.PLUS || event.getCode() == KeyCode.ADD) {
                    zoom(1.1); // Zoom in
                } else if (event.getCode() == KeyCode.MINUS || event.getCode() == KeyCode.SUBTRACT) {
                    zoom(0.9); // Zoom out
                }
            }
        });

        // Bind the loadImage button's disable property to the required fields
        loadImage.disableProperty().bind(
                stateField.textProperty().isEmpty()
                        .or(gameField.textProperty().isEmpty())
                        .or(panelCountField.textProperty().isEmpty())
                        .or(mainBallRowsField.textProperty().isEmpty())
                        .or(xOffsetsField.textProperty().isEmpty())
                        .or(yOffsetsField.textProperty().isEmpty())
                        .or(Bindings.createBooleanBinding(() -> {
                            if (bonusGameCheckBox.isSelected()) {
                                return bonusBallRowsField.textProperty().isEmpty().get()
                                        || bonusBallColumnsField.textProperty().isEmpty().get()
                                        || bonusXOffsetsField.textProperty().isEmpty().get()
                                        || bonusYOffsetsField.textProperty().isEmpty().get();
                            }
                            return false;
                        }, bonusGameCheckBox.selectedProperty(), bonusBallRowsField.textProperty(), bonusBallColumnsField.textProperty(), bonusXOffsetsField.textProperty(), bonusYOffsetsField.textProperty()))
                        .or(Bindings.createBooleanBinding(() -> {
                            try {
                                int panelCount = Integer.parseInt(panelCountField.getText());
                                return parseOffsets(xOffsetsField.getText(), panelCount) == null
                                        || parseOffsets(yOffsetsField.getText(), panelCount) == null
                                        || (bonusGameCheckBox.isSelected() && (parseOffsets(bonusXOffsetsField.getText(), panelCount) == null || parseOffsets(bonusYOffsetsField.getText(), panelCount) == null));
                            } catch (NumberFormatException e) {
                                return true;
                            }
                        }, panelCountField.textProperty(), xOffsetsField.textProperty(), yOffsetsField.textProperty(), bonusXOffsetsField.textProperty(), bonusYOffsetsField.textProperty()))
        );
    }

    private void validateAndUpdateProcessor() {
        try {
            int panelCount = Integer.parseInt(panelCountField.getText());
            if (parseOffsets(xOffsetsField.getText(), panelCount) != null &&
                    parseOffsets(yOffsetsField.getText(), panelCount) != null &&
                    (!bonusGameCheckBox.isSelected() ||
                            (parseOffsets(bonusXOffsetsField.getText(), panelCount) != null &&
                                    parseOffsets(bonusYOffsetsField.getText(), panelCount) != null))) {
                updateProcessor();
            }
        } catch (NumberFormatException e) {
            // Handle the exception if needed
        }
    }

    private void bindSliderToTextField(Slider slider, TextField textField) {
        slider.valueProperty().addListener((observable, oldValue, newValue) -> {
            textField.setText(String.format("%.0f", newValue));
        });

        textField.setOnAction(event -> {
            try {
                double value = Double.parseDouble(textField.getText());
                slider.setValue(value);
            } catch (NumberFormatException e) {
                textField.setText(String.format("%.0f", slider.getValue()));
            }
        });
    }

    private void zoom(double factor) {
        double currentScale = imageView.getScaleX();
        double newScale = currentScale * factor;

        // Limit the zoom level if needed
        newScale = Math.max(0.1, Math.min(newScale, 10.0));

        imageView.setScaleX(newScale);
        imageView.setScaleY(newScale);
    }

    @FXML
    private void loadImage() {
        updateProcessor();
    }

    private void updateProcessor() {
        String state = stateField.getText();
        String game = gameField.getText();
        String panelCountText = panelCountField.getText();
        String mainBallRowsText = mainBallRowsField.getText();
        String bonusBallRowsText = bonusBallRowsField.getText();
        String mainBallColumnsText = mainBallColumnsField.getText();
        String bonusBallColumnsText = bonusBallColumnsField.getText();
        String xOffsetsText = xOffsetsField.getText();
        String yOffsetsText = yOffsetsField.getText();
        String bonusXOffsetsText = bonusXOffsetsField.getText();
        String bonusYOffsetsText = bonusYOffsetsField.getText();

        if (state.isEmpty() || game.isEmpty() || panelCountText.isEmpty() ||
                mainBallRowsText.isEmpty() || mainBallColumnsText.isEmpty() ||
                xOffsetsText.isEmpty() || yOffsetsText.isEmpty() ||
                (bonusGameCheckBox.isSelected() && (bonusBallRowsText.isEmpty() || bonusBallColumnsText.isEmpty() || bonusXOffsetsText.isEmpty() || bonusYOffsetsText.isEmpty()))) {
            showAlert("Error", "All fields must be filled out.");
            return;
        }

        int panelCount, mainBallRows, bonusBallRows = 0, mainBallColumns, bonusBallColumns = 0;
        try {
            panelCount = Integer.parseInt(panelCountText);
            mainBallRows = Integer.parseInt(mainBallRowsText);
            mainBallColumns = Integer.parseInt(mainBallColumnsText);
            if (bonusGameCheckBox.isSelected()) {
                bonusBallRows = Integer.parseInt(bonusBallRowsText);
                bonusBallColumns = Integer.parseInt(bonusBallColumnsText);
            }
        } catch (NumberFormatException e) {
            showAlert("Error", "Panel count, rows, and columns must be valid integers.");
            return;
        }

        int[] xOffsets = parseOffsets(xOffsetsText, panelCount);
        int[] yOffsets = parseOffsets(yOffsetsText, panelCount);
        int[] bonusXOffsets = bonusGameCheckBox.isSelected() ? parseOffsets(bonusXOffsetsText, panelCount) : new int[panelCount];
        int[] bonusYOffsets = bonusGameCheckBox.isSelected() ? parseOffsets(bonusYOffsetsText, panelCount) : new int[panelCount];

        if (xOffsets == null || yOffsets == null || (bonusGameCheckBox.isSelected() && (bonusXOffsets == null || bonusYOffsets == null))) {
            showAlert("Error", "Offsets must be valid integers separated by commas.");
            return;
        }

        String imagePath = "src/main/resources/images/" + state + "/" + game + ".jpg";

        try {
            processor = new LotteryBetslipProcessor(imagePath, panelCount, mainBallRows, bonusBallRows, mainBallColumns, bonusBallColumns, xOffsets, yOffsets, bonusXOffsets, bonusYOffsets);
            processor.setSpacing((int) mainBallHorizontalSlider.getValue(), (int) bonusBallHorizontalSlider.getValue(), (int) verticalSlider.getValue());
            processor.setMarkingProperties((int) sizeSlider.getValue());
            updateImage();
        } catch (IOException e) {
            showAlert("Error", "Cannot load image: " + e.getMessage());
        }
    }

    private int[] parseOffsets(String offsetsText, int panelCount) {
        String[] parts = offsetsText.split(",");
        if (parts.length != panelCount) {
            return null;
        }
        int[] offsets = new int[panelCount];
        try {
            for (int i = 0; i < panelCount; i++) {
                offsets[i] = Integer.parseInt(parts[i].trim());
            }
        } catch (NumberFormatException e) {
            return null;
        }
        return offsets;
    }

    private void updateImage() {
        if (processor != null) {
            imageView.setImage(ImageUtils.convertToFxImage(processor.plotMarkings()));
        }
    }

    @FXML
    private void saveImage() {
        if (processor != null) {
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Save Coordinates");
            fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Serialized Files", "*.ser"));
            File file = fileChooser.showSaveDialog(null);
            if (file != null) {
                try {
                    processor.saveCoordinatesToFile(file.getAbsolutePath());
                    showAlert("Success", "Coordinates saved successfully.");
                } catch (IOException e) {
                    showAlert("Error", "Cannot save coordinates: " + e.getMessage());
                }
            }
        }
    }

    @FXML
    private void toggleBonusFields() {
        boolean isSelected = bonusGameCheckBox.isSelected();
        bonusFields.setVisible(isSelected);
        bonusFields.setManaged(isSelected);
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    @Override
    public Mono<Void> setUpUi(String stateName, String gameName) {
        return Mono.empty();
    }
}
