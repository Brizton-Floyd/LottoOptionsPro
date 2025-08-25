package com.example.lottooptionspro.util;

import com.example.lottooptionspro.models.BetslipTemplate;
import com.example.lottooptionspro.models.ScannerMark;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.List;

public class ImageProcessor {

    /**
     * Converts a marked image to a scanner-ready black and white version.
     * This method uses a robust, two-step process to ensure all markings are drawn correctly.
     *
     * @param markedColorImage The source image with user markings (unscaled).
     * @param template The betslip template containing the scanner mark coordinates.
     * @return A new, clean BufferedImage with only the necessary black markings on a white background.
     */
    public static BufferedImage convertToSelectiveBnW(BufferedImage markedColorImage, BetslipTemplate template) {
        int width = markedColorImage.getWidth();
        int height = markedColorImage.getHeight();

        // Step 1: Create an intermediate color canvas to reliably draw all markings on.
        BufferedImage intermediateImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = intermediateImage.createGraphics();

        try {
            // Prepare the canvas with a white background.
            g2d.setColor(Color.WHITE);
            g2d.fillRect(0, 0, width, height);

            // Set the drawing color to black for all subsequent marks.
            g2d.setColor(Color.BLACK);

            // Plot the user's number selections onto the canvas by finding pure black pixels.
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    if (markedColorImage.getRGB(x, y) == Color.BLACK.getRGB()) {
                        g2d.fillRect(x, y, 1, 1);
                    }
                }
            }

            // Plot the scanner marks from the template directly onto the same canvas.
            List<ScannerMark> scannerMarks = template.getScannerMarks();
            if (scannerMarks != null) {
                for (ScannerMark mark : scannerMarks) {
                    g2d.fillRect((int) mark.getX(), (int) mark.getY(), (int) mark.getWidth(), (int) mark.getHeight());
                }
            }
        } finally {
            g2d.dispose();
        }

        // Step 2: Create the final binary image and draw the intermediate canvas onto it.
        // This correctly handles the conversion to the final black-and-white format.
        BufferedImage finalImage = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_BINARY);
        Graphics2D finalG2d = finalImage.createGraphics();
        try {
            finalG2d.drawImage(intermediateImage, 0, 0, null);
        } finally {
            finalG2d.dispose();
        }

        return finalImage;
    }
}
