package com.example.lottooptionspro.presenter;

import javafx.scene.image.Image;
import java.util.List;

public interface PrintPreviewView {
    void displayPrintPages(List<Image> pages);
    void closeView();
    void showError(String message);
    void showSuccess(String message);
    void showProgress(boolean show);
    boolean showPrintDialog();
}