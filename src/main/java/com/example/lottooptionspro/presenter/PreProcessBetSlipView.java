package com.example.lottooptionspro.presenter;

import javafx.scene.image.Image;

public interface PreProcessBetSlipView {
    String getStateName();
    String getGameName();
    int getThreshold();
    int getDistanceFromTop();
    int getDistanceFromBottom();
    int getDistanceFromLeft();
    void setOriginalImage(Image image);
    void setProcessedImage(Image image);
    void showSaveConfirmation();
    void showError(String message);
}
