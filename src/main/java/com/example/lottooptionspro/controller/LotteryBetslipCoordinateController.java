package com.example.lottooptionspro.controller;

import com.example.lottooptionspro.GameInformation;
import com.example.lottooptionspro.presenter.LotteryBetslipCoordinatePresenter;
import com.example.lottooptionspro.presenter.LotteryBetslipCoordinateView;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import net.rgielen.fxweaver.core.FxmlView;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.io.File;

@Component
@FxmlView("/com.example.lottooptionspro/controller/LotteryBetslipCoordinateView.fxml")
public class LotteryBetslipCoordinateController implements GameInformation, LotteryBetslipCoordinateView {

    @FXML private ImageView imageView;
    @FXML private ScrollPane scrollPane;
    @FXML private Slider mainBallHorizontalSlider, bonusBallHorizontalSlider, verticalSlider, sizeSlider;
    @FXML private TextField stateField, gameField, panelCountField, mainBallRowsField, bonusBallRowsField, mainBallColumnsField, bonusBallColumnsField;
    @FXML private TextField mainBallHorizontalSpacingField, bonusBallHorizontalSpacingField, verticalSpacingField, markingSizeField;
    @FXML private TextField xOffsetsField, yOffsetsField, bonusXOffsetsField, bonusYOffsetsField;
    @FXML private CheckBox bonusGameCheckBox, jackpotOptionCheckBox, verticalOrientationCheckBox, bottomToTopCheckBox;
    @FXML private VBox bonusFields, jackpotOptionFields;
    @FXML private Button loadImage, saveCoordinates;
    @FXML private SplitPane root;
    @FXML private TextField jackpotOptionXField, jackpotOptionYField;

    private final LotteryBetslipCoordinatePresenter presenter;

    public LotteryBetslipCoordinateController() {
        this.presenter = new LotteryBetslipCoordinatePresenter(this);
    }

    public void initialize() {
        setupBindingsAndListeners();
    }

    private void setupBindingsAndListeners() {
        Platform.runLater(() -> root.setDividerPositions(0.4));

        mainBallHorizontalSlider.setMin(0.0); mainBallHorizontalSlider.setMax(100.0); mainBallHorizontalSlider.setValue(0.0); mainBallHorizontalSlider.setBlockIncrement(0.1);
        verticalSlider.setMin(0.0); verticalSlider.setMax(100.0); verticalSlider.setValue(0.0); verticalSlider.setBlockIncrement(0.1);
        sizeSlider.setMin(0.0); sizeSlider.setMax(100.0); sizeSlider.setValue(0.0); sizeSlider.setBlockIncrement(0.1);

        bindSliderToTextField(mainBallHorizontalSlider, mainBallHorizontalSpacingField);
        bindSliderToTextField(bonusBallHorizontalSlider, bonusBallHorizontalSpacingField);
        bindSliderToTextField(verticalSlider, verticalSpacingField);
        bindSliderToTextField(sizeSlider, markingSizeField);

        mainBallHorizontalSlider.valueProperty().addListener((obs, oldVal, newVal) -> presenter.updateProcessor());
        bonusBallHorizontalSlider.valueProperty().addListener((obs, oldVal, newVal) -> presenter.updateProcessor());
        verticalSlider.valueProperty().addListener((obs, oldVal, newVal) -> presenter.updateProcessor());
        sizeSlider.valueProperty().addListener((obs, oldVal, newVal) -> presenter.updateProcessor());

        xOffsetsField.textProperty().addListener((obs, old, n) -> presenter.updateProcessor());
        yOffsetsField.textProperty().addListener((obs, old, n) -> presenter.updateProcessor());
        bonusXOffsetsField.textProperty().addListener((obs, old, n) -> presenter.updateProcessor());
        bonusYOffsetsField.textProperty().addListener((obs, old, n) -> presenter.updateProcessor());
        jackpotOptionXField.textProperty().addListener((obs, old, n) -> presenter.updateProcessor());
        jackpotOptionYField.textProperty().addListener((obs, old, n) -> presenter.updateProcessor());

        scrollPane.addEventFilter(ScrollEvent.SCROLL, event -> {
            if (event.isControlDown()) {
                zoom(event.getDeltaY() > 0 ? 1.1 : 0.9);
                event.consume();
            }
        });

        scrollPane.setOnKeyPressed(event -> {
            if (event.isControlDown()) {
                if (event.getCode() == KeyCode.PLUS || event.getCode() == KeyCode.ADD) zoom(1.1);
                else if (event.getCode() == KeyCode.MINUS || event.getCode() == KeyCode.SUBTRACT) zoom(0.9);
            }
        });

        jackpotOptionCheckBox.setOnAction(event -> toggleJackpotOptionFields());
        verticalOrientationCheckBox.setOnAction(event -> presenter.updateProcessor());
        bottomToTopCheckBox.setOnAction(event -> presenter.updateProcessor());
        bonusGameCheckBox.setOnAction(event -> toggleBonusFields());
    }

    private void bindSliderToTextField(Slider slider, TextField textField) {
        slider.valueProperty().addListener((obs, old, n) -> textField.setText(String.format("%.1f", n.doubleValue())));
        textField.setOnAction(e -> {
            try { slider.setValue(Double.parseDouble(textField.getText())); } catch (NumberFormatException ex) { textField.setText(String.format("%.1f", slider.getValue())); }
        });
    }

    @FXML private void loadImage() { presenter.updateProcessor(); }
    @FXML private void saveImage() { presenter.saveCoordinates(); }
    @FXML private void toggleBonusFields() { bonusFields.setVisible(bonusGameCheckBox.isSelected()); bonusFields.setManaged(bonusGameCheckBox.isSelected()); }
    @FXML private void toggleJackpotOptionFields() { jackpotOptionFields.setVisible(jackpotOptionCheckBox.isSelected()); jackpotOptionFields.setManaged(jackpotOptionCheckBox.isSelected()); }

    @Override public Mono<Void> setUpUi(String stateName, String gameName) { return Mono.empty(); }
    @Override public String getState() { return stateField.getText(); }
    @Override public String getGame() { return gameField.getText(); }
    @Override public String getPanelCountText() { return panelCountField.getText(); }
    @Override public String getMainBallRowsText() { return mainBallRowsField.getText(); }
    @Override public String getMainBallColumnsText() { return mainBallColumnsField.getText(); }
    @Override public String getBonusBallRowsText() { return bonusBallRowsField.getText(); }
    @Override public String getBonusBallColumnsText() { return bonusBallColumnsField.getText(); }
    @Override public String getXOffsetsText() { return xOffsetsField.getText(); }
    @Override public String getYOffsetsText() { return yOffsetsField.getText(); }
    @Override public String getBonusXOffsetsText() { return bonusXOffsetsField.getText(); }
    @Override public String getBonusYOffsetsText() { return bonusYOffsetsField.getText(); }
    @Override public String getJackpotOptionXText() { return jackpotOptionXField.getText(); }
    @Override public String getJackpotOptionYText() { return jackpotOptionYField.getText(); }
    @Override public double getMainBallHorizontalSpacing() { return mainBallHorizontalSlider.getValue(); }
    @Override public double getBonusBallHorizontalSpacing() { return bonusBallHorizontalSlider.getValue(); }
    @Override public double getVerticalSpacing() { return verticalSlider.getValue(); }
    @Override public double getMarkingSize() { return sizeSlider.getValue(); }
    @Override public boolean isBonusGameChecked() { return bonusGameCheckBox.isSelected(); }
    @Override public boolean isJackpotOptionChecked() { return jackpotOptionCheckBox.isSelected(); }
    @Override public boolean isVerticalOrientationChecked() { return verticalOrientationCheckBox.isSelected(); }
    @Override public boolean isBottomToTopChecked() { return bottomToTopCheckBox.isSelected(); }

    @Override public void setImage(Image image) { imageView.setImage(image); }

    @Override
    public void zoom(double factor) {
        double newScale = Math.max(0.1, Math.min(imageView.getScaleX() * factor, 10.0));
        imageView.setScaleX(newScale); imageView.setScaleY(newScale);
    }

    @Override
    public File showSaveDialog(String initialDirectory, String initialFileName) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Save Coordinates");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Serialized Files", "*.ser"));
        File directory = new File(initialDirectory);
        if (!directory.exists()) directory.mkdirs();
        fileChooser.setInitialDirectory(directory);
        fileChooser.setInitialFileName(initialFileName);
        return fileChooser.showSaveDialog(null);
    }

    @Override
    public void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR); alert.setTitle(title); alert.setHeaderText(null); alert.setContentText(message); alert.showAndWait();
    }
}
