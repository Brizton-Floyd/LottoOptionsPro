package com.example.lottooptionspro.controller;

import com.example.lottooptionspro.GameInformation;
import net.rgielen.fxweaver.core.FxmlView;
import org.springframework.stereotype.Component;
import javafx.fxml.FXML;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.image.WritableImage;
import reactor.core.publisher.Mono;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

@Component
@FxmlView("/com.example.lottooptionspro/controller/BetSlipPreprocessView.fxml")
public class PreProcessBetSlipController implements GameInformation {

    @FXML private TextField stateNameField;
    @FXML private TextField gameNameField;
    @FXML private TextField thresholdField;
    @FXML private TextField distanceFromLeftField;
    @FXML private TextField distanceFromRightField;
    @FXML private TextField distanceFromTopField;
    @FXML private TextField distanceFromBottomField;
    @FXML private ImageView originalImageView;
    @FXML private ImageView processedImageView;
    @FXML private ImageView saveConfirmationIcon;


    private BufferedImage originalBufferedImage;
    private BufferedImage processedBufferedImage;

    @FXML
    public void initialize() {
        // Load the green checkmark icon
        try {
            BufferedImage read = ImageIO.read(new File("src/main/resources/images/icons/success.jpg"));
            Image image = convertToFxImage(read);
            saveConfirmationIcon.setImage(image);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

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

        int distanceFromLeft = 0;
        if (!distanceFromLeftField.getText().isEmpty()) {
            distanceFromLeft = Integer.parseInt(distanceFromLeftField.getText());
        }

        processedBufferedImage = new BufferedImage(originalBufferedImage.getWidth(), originalBufferedImage.getHeight(), BufferedImage.TYPE_INT_RGB);

        for (int y = 0; y < originalBufferedImage.getHeight(); y++) {
            for (int x = 0; x < originalBufferedImage.getWidth(); x++) {
                int rgb = originalBufferedImage.getRGB(x, y);
                java.awt.Color color = new java.awt.Color(rgb);

                boolean processPixel = false;

                // Check if pixel is in top region
                if (y < distanceFromTop) {
                    // Only process pixels within left range in top region
                    processPixel = (x < distanceFromLeft);

                    if (processPixel && isBlackish(color, threshold)) {
                        processedBufferedImage.setRGB(x, y, java.awt.Color.BLACK.getRGB());
                    } else {
                        processedBufferedImage.setRGB(x, y, Color.WHITE.getRGB()); // Keep original color
                    }
                }
                // Check if pixel is in bottom region
                else if (y >= originalBufferedImage.getHeight() - distanceFromBottom) {
                    // Process entire width of bottom region
                    if (isBlackish(color, threshold)) {
                        processedBufferedImage.setRGB(x, y, java.awt.Color.BLACK.getRGB());
                    } else {
                        processedBufferedImage.setRGB(x, y, rgb); // Keep original color
                    }
                }
                // Middle region
                else {
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

                // Show the green checkmark icon
                saveConfirmationIcon.setVisible(true);
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