package com.example.lottooptionspro.presenter;

import com.example.lottooptionspro.models.Coordinate;
import com.example.lottooptionspro.models.ScannerMark;

import java.io.File;
import java.util.Optional;

public interface TemplateCreatorView {
    void showView();
    File showOpenImageDialog();
    File showOpenTemplateDialog();
    File showSaveDialog(String initialFileName);
    void displayImage(String imagePath);
    void clearAllRectangles();
    void drawRectangle(Coordinate coordinate, int width, int height);
    void drawScannerMark(ScannerMark mark);
    void drawPreviewRectangle(Coordinate coordinate, int width, int height);
    void clearPreviewRectangles();
    String getGameName();
    String getJurisdiction();
    void setGameName(String name);
    void setJurisdiction(String name);
    String getNextNumber();
    void setNextNumber(String number);
    String getSelectedMappingMode();
    String getSelectedPanel();
    String getGlobalOptionName();
    void showError(String message);
    void showSuccess(String message);
    Optional<String> askForPreviewNumbers();
    void setSelectedMarkControlsVisible(boolean visible);
    void setSelectedMarkDimensions(double width, double height);
    void setScannerMarkCount(int count);
    void updateScannerMarkRectangle(ScannerMark mark, double width, double height);
}
