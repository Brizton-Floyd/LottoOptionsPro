package com.example.lottooptionspro.controller;

import com.example.lottooptionspro.GameInformation;
import com.example.lottooptionspro.presenter.PreProcessBetSlipPresenter;
import com.example.lottooptionspro.presenter.PreProcessBetSlipView;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import net.rgielen.fxweaver.core.FxmlView;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

@Component
@FxmlView("/com.example.lottooptionspro/controller/BetSlipPreprocessView.fxml")
public class PreProcessBetSlipController implements GameInformation, PreProcessBetSlipView {

    @FXML private TextField stateNameField;
    @FXML private TextField gameNameField;
    @FXML private TextField thresholdField;
    @FXML private TextField distanceFromLeftField;
    @FXML private TextField distanceFromRightField;
    @FXML private TextField distanceFromTopField;
    @FXML private TextField distanceFromBottomField;
    @FXML private ImageView originalImageView;
    @FXML private ImageView processedImageView;
    @FXML private ImageView saveConfirmationIcon;

    private final PreProcessBetSlipPresenter presenter;

    public PreProcessBetSlipController() {
        this.presenter = new PreProcessBetSlipPresenter(this);
    }

    @FXML
    public void initialize() {
        try {
            BufferedImage read = ImageIO.read(new File("src/main/resources/images/icons/success.jpg"));
            Image image = presenter.convertToFxImage(read);
            saveConfirmationIcon.setImage(image);
        } catch (IOException e) {
            showError("Could not load success icon: " + e.getMessage());
        }
    }

    @FXML
    private void processImage() {
        presenter.processImage();
    }

    @FXML
    private void saveProcessedImage() {
        presenter.saveProcessedImage();
    }

    @Override
    public Mono<Void> setUpUi(String stateName, String gameName) {
        stateNameField.setText(stateName);
        gameNameField.setText(gameName);
        presenter.loadOriginalImage();
        return Mono.empty();
    }

    @Override
    public String getStateName() {
        return stateNameField.getText();
    }

    @Override
    public String getGameName() {
        return gameNameField.getText();
    }

    @Override
    public int getThreshold() {
        try {
            return Integer.parseInt(thresholdField.getText());
        } catch (NumberFormatException e) {
            showError("Invalid threshold value.");
            return 0;
        }
    }

    @Override
    public int getDistanceFromTop() {
        try {
            return Integer.parseInt(distanceFromTopField.getText());
        } catch (NumberFormatException e) {
            showError("Invalid distance from top value.");
            return 0;
        }
    }

    @Override
    public int getDistanceFromBottom() {
        try {
            return Integer.parseInt(distanceFromBottomField.getText());
        } catch (NumberFormatException e) {
            showError("Invalid distance from bottom value.");
            return 0;
        }
    }

    @Override
    public int getDistanceFromLeft() {
        try {
            if (distanceFromLeftField.getText().isEmpty()) return 0;
            return Integer.parseInt(distanceFromLeftField.getText());
        } catch (NumberFormatException e) {
            showError("Invalid distance from left value.");
            return 0;
        }
    }

    @Override
    public void setOriginalImage(Image image) {
        originalImageView.setImage(image);
    }

    @Override
    public void setProcessedImage(Image image) {
        processedImageView.setImage(image);
    }

    @Override
    public void showSaveConfirmation() {
        saveConfirmationIcon.setVisible(true);
    }

    @Override
    public void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
