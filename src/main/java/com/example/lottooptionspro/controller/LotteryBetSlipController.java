package com.example.lottooptionspro.controller;

import com.example.lottooptionspro.presenter.LotteryBetSlipPresenter;
import com.example.lottooptionspro.presenter.LotteryBetSlipView;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import net.rgielen.fxweaver.core.FxmlView;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@FxmlView("/com.example.lottooptionspro/controller/BetSlipView.fxml")
public class LotteryBetSlipController implements LotteryBetSlipView {
    private final LotteryBetSlipPresenter presenter;
    private Stage stage;

    @FXML
    private VBox mainContainer;
    @FXML
    private VBox betslipsContainer;
    @FXML
    private Button printButton;
    @FXML
    private Button extractColorButton;

    public LotteryBetSlipController() {
        this.presenter = new LotteryBetSlipPresenter(this);
    }

    @FXML
    public void initialize() {
        this.stage = new Stage();
        stage.setScene(new Scene(mainContainer));

        printButton.setOnAction(e -> presenter.printPDF());
        extractColorButton.setOnAction(e -> presenter.extractColorAndRegenerate());
    }

    public void setData(List<List<int[]>> partitionedNumbers, String stateName, String gameName) {
        presenter.setData(partitionedNumbers, stateName, gameName);
    }

    @Override
    public void displayPdfPage(Image fxImage) {
        ImageView imageView = new ImageView(fxImage);
        betslipsContainer.getChildren().clear();
        betslipsContainer.getChildren().add(imageView);
    }

    @Override
    public void showView() {
        if (stage != null) {
            stage.show();
        }
    }

    @Override
    public Node getContainerNodeForPrinting() {
        return betslipsContainer;
    }

    @Override
    public Stage getStage() {
        return stage;
    }

    @Override
    public void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
