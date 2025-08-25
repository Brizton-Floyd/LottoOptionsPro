package com.example.lottooptionspro.presenter;

import javafx.scene.image.Image;
import java.io.File;
import java.util.List;

public interface PdfPreviewView {
    void displayPdfPages(List<Image> pages);
    void closeView();
    File showSavePdfDialog(String initialFileName);
    String getSelectedColorMode();
    void showError(String message);
}
