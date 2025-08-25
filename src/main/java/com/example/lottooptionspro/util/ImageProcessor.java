package com.example.lottooptionspro.util;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

public class ImageProcessor {

    private static final int BLACK_THRESHOLD = 50; // For scanner marks
    private static final double SCAN_ZONE_PERCENTAGE = 0.15; // Scan top 15% and bottom 15%

    /**
     * Converts a marked image to a selective black and white version.
     * It preserves dark pixels in the top and bottom zones (scanner marks)
     * and pure black pixels anywhere (user markings), turning everything else white.
     *
     * @param markedColorImage The source image with user markings.
     * @return A new BufferedImage with a white background and preserved marks.
     */
    public static BufferedImage convertToSelectiveBnW(BufferedImage markedColorImage) {
        int width = markedColorImage.getWidth();
        int height = markedColorImage.getHeight();

        // 1. Create a blank white image
        BufferedImage newImage = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_BINARY);
        Graphics2D g2d = newImage.createGraphics();
        g2d.setColor(Color.WHITE);
        g2d.fillRect(0, 0, width, height);
        g2d.dispose();

        int scanZoneHeight = (int) (height * SCAN_ZONE_PERCENTAGE);

        // 2. Iterate through all pixels and apply the two-rule system
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int rgb = markedColorImage.getRGB(x, y);
                int red = (rgb >> 16) & 0xFF;
                int green = (rgb >> 8) & 0xFF;
                int blue = rgb & 0xFF;

                // Rule 1: Preserve scanner marks in top/bottom zones
                boolean inScanZone = y < scanZoneHeight || y >= (height - scanZoneHeight);
                if (inScanZone) {
                    if (red < BLACK_THRESHOLD && green < BLACK_THRESHOLD && blue < BLACK_THRESHOLD) {
                        newImage.setRGB(x, y, Color.BLACK.getRGB());
                    }
                } else {
                    // Rule 2: Preserve only pure black user markings in the middle zone
                    if (red == 0 && green == 0 && blue == 0) {
                        newImage.setRGB(x, y, Color.BLACK.getRGB());
                    }
                }
            }
        }

        return newImage;
    }
}
