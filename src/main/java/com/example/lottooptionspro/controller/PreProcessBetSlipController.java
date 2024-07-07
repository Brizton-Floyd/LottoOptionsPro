package com.example.lottooptionspro.controller;

import com.example.lottooptionspro.GameInformation;
import net.rgielen.fxweaver.core.FxmlView;
import org.springframework.stereotype.Component;
import javafx.fxml.FXML;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;
import reactor.core.publisher.Mono;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

@Component
@FxmlView("/com.example.lottooptionspro/controller/BetSlipPreprocessView.fxml")
public class PreProcessBetSlipController implements GameInformation {

    @FXML private TextField stateNameField;
    @FXML private TextField gameNameField;
    @FXML private TextField thresholdField;
    @FXML private TextField distanceFromTopField;
    @FXML private TextField distanceFromBottomField;
    @FXML private ImageView originalImageView;
    @FXML private ImageView processedImageView;

    private BufferedImage originalBufferedImage;
    private BufferedImage processedBufferedImage;


    private void loadOriginalImage() {
        String imagePath = "src/main/resources/images/" + stateNameField.getText() + "/" + gameNameField.getText() + ".jpg";
        try {
            originalBufferedImage = ImageIO.read(new File(imagePath));
            Image fxImage = convertToFxImage(originalBufferedImage);
            originalImageView.setImage(fxImage);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void processImage() {
        int threshold = Integer.parseInt(thresholdField.getText());
        int distanceFromTop = Integer.parseInt(distanceFromTopField.getText());
        int distanceFromBottom = Integer.parseInt(distanceFromBottomField.getText());

        processedBufferedImage = new BufferedImage(originalBufferedImage.getWidth(), originalBufferedImage.getHeight(), BufferedImage.TYPE_INT_RGB);

        // Process the image (similar to your extractColor method)
        for (int y = 0; y < originalBufferedImage.getHeight(); y++) {
            for (int x = 0; x < originalBufferedImage.getWidth(); x++) {
                if (y < distanceFromTop || y >= originalBufferedImage.getHeight() - distanceFromBottom) {
                    java.awt.Color color = new java.awt.Color(originalBufferedImage.getRGB(x, y));
                    if (isBlackish(color, threshold)) {
                        processedBufferedImage.setRGB(x, y, java.awt.Color.BLACK.getRGB());
                    } else {
                        processedBufferedImage.setRGB(x, y, java.awt.Color.WHITE.getRGB());
                    }
                } else {
                    processedBufferedImage.setRGB(x, y, java.awt.Color.WHITE.getRGB());
                }
            }
        }

        // Update the processed image view
        Image processedFxImage = convertToFxImage(processedBufferedImage);
        processedImageView.setImage(processedFxImage);
    }

    @FXML
    private void saveProcessedImage() {
        if (processedBufferedImage != null) {
            try {
                File outputFile = new File("src/main/resources/images/" + stateNameField.getText() + "/Processed_" + gameNameField.getText() + ".jpg");
                ImageIO.write(processedBufferedImage, "jpg", outputFile);
                System.out.println("Processed image saved: " + outputFile.getAbsolutePath());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private boolean isBlackish(java.awt.Color color, int threshold) {
        return color.getRed() < threshold && color.getGreen() < threshold && color.getBlue() < threshold;
    }

    private Image convertToFxImage(BufferedImage image) {
        WritableImage wr = new WritableImage(image.getWidth(), image.getHeight());
        for (int x = 0; x < image.getWidth(); x++) {
            for (int y = 0; y < image.getHeight(); y++) {
                wr.getPixelWriter().setArgb(x, y, image.getRGB(x, y));
            }
        }
        return wr;
    }

    @Override
    public Mono<Void> setUpUi(String stateName, String gameName) {
        // Set initial values
        stateNameField.setText(stateName);
        gameNameField.setText(gameName);

        // Load the original image
        loadOriginalImage();
        return Mono.empty();
    }
}