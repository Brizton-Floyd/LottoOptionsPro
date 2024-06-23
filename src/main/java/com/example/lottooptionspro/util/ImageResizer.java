package com.example.lottooptionspro.util;

import java.awt.*;
import java.awt.image.BufferedImage;

public class ImageResizer {
    public static BufferedImage resizeImageBasedOnTrueSize(BufferedImage originalImage, double actualHeight, double actualWidth) {
        // DPI (Dots Per Inch)
        int dpi = 96; // Common screen DPI

        // Convert inches to pixels
        int newWidth = (int) (actualWidth * dpi);
        int newHeight = (int) (actualHeight * dpi);

        return resizeImage(originalImage, newWidth, newHeight);
    }

    private static BufferedImage resizeImage(BufferedImage originalImage, int targetWidth, int targetHeight) {
        BufferedImage resizedImage = new BufferedImage(targetWidth, targetHeight, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = resizedImage.createGraphics();

        // Fill the background with white color
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, targetWidth, targetHeight);

        // Draw the resized image centered
        g.drawImage(originalImage, 0, 0, targetWidth, targetHeight, null);
        g.dispose();

        return resizedImage;
    }
}
