package com.example.lottooptionspro.presenter;

import javafx.scene.Node;
import javafx.scene.image.Image;
import javafx.stage.Stage;

public interface LotteryBetSlipView {
    void displayPdfPage(Image fxImage);

    void showView();

    Node getContainerNodeForPrinting();

    Stage getStage();

    void showError(String message);
}
