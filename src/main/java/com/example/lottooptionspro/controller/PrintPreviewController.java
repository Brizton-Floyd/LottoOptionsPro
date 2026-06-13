package com.example.lottooptionspro.controller;

import com.example.lottooptionspro.presenter.PrintPreviewPresenter;
import com.example.lottooptionspro.presenter.PrintPreviewView;
import javafx.fxml.FXML;
import javafx.print.PrinterJob;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.ScrollPane;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import net.rgielen.fxweaver.core.FxmlView;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@FxmlView("/com.example.lottooptionspro/controller/PrintPreviewView.fxml")
public class PrintPreviewController implements PrintPreviewView {

    public final PrintPreviewPresenter presenter;

    @FXML
    private VBox printPagesContainer;
    @FXML
    private ProgressIndicator progressIndicator;
    @FXML
    private ScrollPane scrollPane;

    @Autowired
    public PrintPreviewController(PrintPreviewPresenter presenter) {
        this.presenter = presenter;
        this.presenter.setView(this);
    }

    @FXML
    private void handlePrint() {
        presenter.executePrint();
    }

    @FXML
    private void handleCancel() {
        presenter.cancel();
    }

    @Override
    public void displayPrintPages(List<Image> pages) {
        printPagesContainer.getChildren().clear();
        for (Image pageImage : pages) {
            ImageView imageView = new ImageView(pageImage);
            imageView.setPreserveRatio(true);
            imageView.fitWidthProperty().bind(scrollPane.widthProperty().subtract(40));
            
            // Add styling to make it look like printed pages
            imageView.setStyle("-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.3), 5, 0, 2, 2);");
            
            printPagesContainer.getChildren().add(imageView);
        }
    }

    @Override
    public void closeView() {
        Stage stage = (Stage) printPagesContainer.getScene().getWindow();
        stage.close();
    }

    @Override
    public void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Print Error");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    @Override
    public void showSuccess(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Print Success");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    @Override
    public void showProgress(boolean show) {
        scrollPane.setVisible(!show);
        progressIndicator.setVisible(show);
    }

    @Override
    public boolean showPrintDialog() {
        PrinterJob printerJob = PrinterJob.createPrinterJob();
        if (printerJob != null) {
            // Configure page layout for landscape orientation
            javafx.print.PageLayout pageLayout = printerJob.getPrinter().createPageLayout(
                javafx.print.Paper.NA_LETTER,
                javafx.print.PageOrientation.LANDSCAPE,
                javafx.print.Printer.MarginType.DEFAULT
            );
            printerJob.getJobSettings().setPageLayout(pageLayout);
            
            boolean showDialog = printerJob.showPageSetupDialog(printPagesContainer.getScene().getWindow());
            if (showDialog) {
                showDialog = printerJob.showPrintDialog(printPagesContainer.getScene().getWindow());
                if (showDialog) {
                    // Print each page from the preview container
                    boolean success = true;
                    for (Node child : printPagesContainer.getChildren()) {
                        if (child instanceof ImageView) {
                            ImageView imageView = (ImageView) child;
                            boolean printed = printerJob.printPage(imageView);
                            if (!printed) {
                                success = false;
                                break;
                            }
                        }
                    }
                    printerJob.endJob();
                    return success;
                }
            }
        }
        return false;
    }
}