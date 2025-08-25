package com.example.lottooptionspro.controller;

import com.example.lottooptionspro.presenter.PdfPreviewPresenter;
import com.example.lottooptionspro.presenter.PdfPreviewView;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.RadioButton;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Toggle;
import javafx.scene.control.ToggleGroup;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import net.rgielen.fxweaver.core.FxmlView;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.List;

@Component
@FxmlView("/com.example.lottooptionspro/controller/PdfPreviewView.fxml")
public class PdfPreviewController implements PdfPreviewView {

    public final PdfPreviewPresenter presenter;

    @FXML
    private VBox pdfPagesContainer;
    @FXML
    private RadioButton colorRadioButton;
    @FXML
    private RadioButton bwRadioButton;
    @FXML
    private RadioButton scannerRadioButton;
    @FXML
    private ToggleGroup colorToggleGroup;
    @FXML
    private ProgressIndicator progressIndicator;
    @FXML
    private ScrollPane scrollPane;

    @Autowired
    public PdfPreviewController(PdfPreviewPresenter presenter) {
        this.presenter = presenter;
        this.presenter.setView(this);
    }

    @FXML
    public void initialize() {
        colorToggleGroup.selectedToggleProperty().addListener((observable, oldValue, newValue) -> {
            presenter.onColorModeChanged(getSelectedColorMode());
        });
    }

    @FXML
    private void handleSave() {
        presenter.save();
    }

    @FXML
    private void handleCancel() {
        presenter.cancel();
    }

    @Override
    public void displayPdfPages(List<Image> pages) {
        pdfPagesContainer.getChildren().clear();
        for (Image pageImage : pages) {
            ImageView imageView = new ImageView(pageImage);
            imageView.setPreserveRatio(true);
            imageView.fitWidthProperty().bind(scrollPane.widthProperty().subtract(20));
            pdfPagesContainer.getChildren().add(imageView);
        }
    }

    @Override
    public void closeView() {
        Stage stage = (Stage) pdfPagesContainer.getScene().getWindow();
        stage.close();
    }

    @Override
    public File showSavePdfDialog(String initialFileName) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Save PDF");
        fileChooser.setInitialFileName(initialFileName);
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF Files", "*.pdf"));
        return fileChooser.showSaveDialog(pdfPagesContainer.getScene().getWindow());
    }

    @Override
    public String getSelectedColorMode() {
        RadioButton selected = (RadioButton) colorToggleGroup.getSelectedToggle();
        if (selected == null) return "Color";
        return selected.getText();
    }

    @Override
    public void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    @Override
    public void showProgress(boolean show) {
        scrollPane.setVisible(!show);
        progressIndicator.setVisible(show);
    }
}
