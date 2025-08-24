package com.example.lottooptionspro.controller;

import com.example.lottooptionspro.models.BetslipTemplate;
import com.example.lottooptionspro.presenter.TemplateCreatorPresenter;
import com.example.lottooptionspro.presenter.TemplateCreatorView;
import javafx.fxml.FXML;
import javafx.scene.control.TextField;
import javafx.scene.image.ImageView;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import net.rgielen.fxweaver.core.FxmlView;
import org.springframework.stereotype.Component;

import java.io.File;

@Component
@FxmlView("/com.example.lottooptionspro/controller/TemplateCreatorView.fxml")
public class TemplateCreatorController implements TemplateCreatorView {

    private TemplateCreatorPresenter presenter;

    @FXML
    private TextField gameNameField;

    @FXML
    private TextField jurisdictionField;

    @FXML
    private ImageView betslipImageView;

    @FXML
    public void initialize() {
        this.presenter = new TemplateCreatorPresenter(new BetslipTemplate(), this);
    }

    @FXML
    private void loadImage() {
        presenter.loadImage();
    }

    @Override
    public void showView() {
        // Handled by ScreenManager
    }

    @Override
    public void showError(String message) {
        // Implement error display logic
    }

    public void openFileChooser() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Open Betslip Image");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg")
        );
        File selectedFile = fileChooser.showOpenDialog(getStage());
        if (selectedFile != null) {
            presenter.onImageSelected(selectedFile.toURI().toString(), selectedFile.getPath());
        }
    }

    public void displayImage(String imagePath) {
        javafx.scene.image.Image image = new javafx.scene.image.Image(imagePath);
        betslipImageView.setImage(image);
    }

    private Stage getStage() {
        return (Stage) gameNameField.getScene().getWindow();
    }
}
