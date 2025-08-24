package com.example.lottooptionspro.presenter;

import com.example.lottooptionspro.models.Coordinate;

import java.io.File;
import java.util.Optional;

public interface TemplateCreatorView {
    void showView();
    void showError(String message);
    void showSuccess(String message);

    // File Dialogs
    File showOpenImageDialog();
    File showOpenTemplateDialog();
    File showSaveDialog(String initialFileName);

    // Input Dialogs
    Optional<String> askForPreviewNumbers();

    // Getters for UI data
    String getGameName();
    String getJurisdiction();
    String getSelectedMappingMode();
    String getSelectedPanel();
    String getGlobalOptionName();
    String getNextNumber();

    // Setters for UI data (for loading)
    void setGameName(String name);
    void setJurisdiction(String name);
    void setNextNumber(String number);

    // Drawing methods
    void displayImage(String imagePath);
    void drawRectangle(Coordinate coordinate, int width, int height);
    void drawPreviewRectangle(Coordinate coordinate, int width, int height);
    void clearAllRectangles();
    void clearPreviewRectangles();
}
