package com.example.lottooptionspro.presenter;

import javafx.scene.image.Image;

import java.io.File;

public interface LotteryBetslipCoordinateView {
    String getState();
    String getGame();
    String getPanelCountText();
    String getMainBallRowsText();
    String getMainBallColumnsText();
    String getBonusBallRowsText();
    String getBonusBallColumnsText();
    String getXOffsetsText();
    String getYOffsetsText();
    String getBonusXOffsetsText();
    String getBonusYOffsetsText();
    String getJackpotOptionXText();
    String getJackpotOptionYText();
    double getMainBallHorizontalSpacing();
    double getBonusBallHorizontalSpacing();
    double getVerticalSpacing();
    double getMarkingSize();
    boolean isBonusGameChecked();
    boolean isJackpotOptionChecked();
    boolean isVerticalOrientationChecked();
    boolean isBottomToTopChecked();
    void setImage(Image image);
    void zoom(double factor);
    File showSaveDialog(String initialDirectory, String initialFileName);
    void showAlert(String title, String message);
}
