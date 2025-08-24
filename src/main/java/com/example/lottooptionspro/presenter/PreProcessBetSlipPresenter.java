package com.example.lottooptionspro.presenter;

import javafx.scene.image.Image;
import javafx.scene.image.WritableImage;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

public class PreProcessBetSlipPresenter {

    private final PreProcessBetSlipView view;
    private BufferedImage originalBufferedImage;
    private BufferedImage processedBufferedImage;

    public PreProcessBetSlipPresenter(PreProcessBetSlipView view) {
        this.view = view;
    }

    public void loadOriginalImage() {
        String imagePath = "src/main/resources/images/" + view.getStateName() + "/" + view.getGameName() + ".jpg";
        try {
            originalBufferedImage = ImageIO.read(new File(imagePath));
            Image fxImage = convertToFxImage(originalBufferedImage);
            view.setOriginalImage(fxImage);
        } catch (IOException e) {
            view.showError("Could not load original image: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void processImage() {
        if (originalBufferedImage == null) {
            view.showError("Original image not loaded.");
            return;
        }

        int threshold = view.getThreshold();
        int distanceFromTop = view.getDistanceFromTop();
        int distanceFromBottom = view.getDistanceFromBottom();
        int distanceFromLeft = view.getDistanceFromLeft();

        processedBufferedImage = new BufferedImage(originalBufferedImage.getWidth(), originalBufferedImage.getHeight(), BufferedImage.TYPE_INT_RGB);

        for (int y = 0; y < originalBufferedImage.getHeight(); y++) {
            for (int x = 0; x < originalBufferedImage.getWidth(); x++) {
                int rgb = originalBufferedImage.getRGB(x, y);
                Color color = new Color(rgb);

                boolean processPixel = false;

                if (y < distanceFromTop) {
                    processPixel = (x < distanceFromLeft);
                    if (processPixel && isBlackish(color, threshold)) {
                        processedBufferedImage.setRGB(x, y, Color.BLACK.getRGB());
                    } else {
                        processedBufferedImage.setRGB(x, y, Color.WHITE.getRGB());
                    }
                } else if (y >= originalBufferedImage.getHeight() - distanceFromBottom) {
                    if (isBlackish(color, threshold)) {
                        processedBufferedImage.setRGB(x, y, Color.BLACK.getRGB());
                    } else {
                        processedBufferedImage.setRGB(x, y, rgb);
                    }
                } else {
                    processedBufferedImage.setRGB(x, y, Color.WHITE.getRGB());
                }
            }
        }

        Image processedFxImage = convertToFxImage(processedBufferedImage);
        view.setProcessedImage(processedFxImage);
    }

    public void saveProcessedImage() {
        if (processedBufferedImage != null) {
            try {
                File outputFile = new File("src/main/resources/images/" + view.getStateName() + "/Processed_" + view.getGameName() + ".jpg");
                ImageIO.write(processedBufferedImage, "jpg", outputFile);
                view.showSaveConfirmation();
            } catch (IOException e) {
                view.showError("Could not save processed image: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    private boolean isBlackish(Color color, int threshold) {
        return color.getRed() < threshold && color.getGreen() < threshold && color.getBlue() < threshold;
    }

    public Image convertToFxImage(BufferedImage image) {
        if (image == null) return null;
        WritableImage wr = new WritableImage(image.getWidth(), image.getHeight());
        for (int x = 0; x < image.getWidth(); x++) {
            for (int y = 0; y < image.getHeight(); y++) {
                wr.getPixelWriter().setArgb(x, y, image.getRGB(x, y));
            }
        }
        return wr;
    }
}
