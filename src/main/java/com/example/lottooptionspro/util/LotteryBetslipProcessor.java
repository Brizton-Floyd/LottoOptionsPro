package com.example.lottooptionspro.util;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.HashMap;
import java.util.Map;

public class LotteryBetslipProcessor implements Serializable {
    private static final long serialVersionUID = 1L;
    private BufferedImage betslip;
    private int panelCount;
    private int mainBallRows;
    private int bonusBallRows;
    private int mainBallColumns;
    private int bonusBallColumns;
    private int panelWidth;
    private int panelHeight;
    private int mainBallHorizontalSpacing;
    private int bonusBallHorizontalSpacing;
    private int verticalSpacing;
    private int markingSize;
    private int[] xOffsets;
    private int[] yOffsets;
    private int[] bonusXOffsets;
    private int[] bonusYOffsets;
    private Map<String, Point> mainBallCoordinates;
    private Map<String, Point> bonusBallCoordinates;

    public LotteryBetslipProcessor(String imagePath, int panelCount, int mainBallRows, int mainBallColumns, int[] xOffsets, int[] yOffsets) throws IOException {
        this(imagePath, panelCount, mainBallRows, 0, mainBallColumns, 0, xOffsets, yOffsets, null, null);
    }

    public LotteryBetslipProcessor(String imagePath, int panelCount, int mainBallRows, int bonusBallRows, int mainBallColumns, int bonusBallColumns, int[] xOffsets, int[] yOffsets, int[] bonusXOffsets, int[] bonusYOffsets) throws IOException {
        File imageFile = new File(imagePath);
        if (!imageFile.exists()) {
            throw new IOException("File not found: " + imagePath);
        }
        this.betslip = ImageIO.read(imageFile);
        this.panelCount = panelCount;
        this.mainBallRows = mainBallRows;
        this.bonusBallRows = bonusBallRows;
        this.mainBallColumns = mainBallColumns;
        this.bonusBallColumns = bonusBallColumns;
        this.xOffsets = xOffsets;
        this.yOffsets = yOffsets;
        this.bonusXOffsets = bonusXOffsets;
        this.bonusYOffsets = bonusYOffsets;

        this.panelWidth = betslip.getWidth();
        this.panelHeight = betslip.getHeight() / panelCount;
        this.mainBallHorizontalSpacing = panelWidth / mainBallColumns;
        this.bonusBallHorizontalSpacing = bonusBallColumns > 0 ? panelWidth / bonusBallColumns : 0;
        this.verticalSpacing = panelHeight / (mainBallRows + bonusBallRows);
        this.markingSize = 10;
        this.mainBallCoordinates = new HashMap<>();
        this.bonusBallCoordinates = new HashMap<>();
    }

    public void setSpacing(int mainBallHorizontalSpacing, int bonusBallHorizontalSpacing, int verticalSpacing) {
        this.mainBallHorizontalSpacing = mainBallHorizontalSpacing;
        this.bonusBallHorizontalSpacing = bonusBallHorizontalSpacing;
        this.verticalSpacing = verticalSpacing;
    }

    public void setMarkingProperties(int size) {
        this.markingSize = size;
    }

    public BufferedImage plotMarkings() {
        BufferedImage markedImage = new BufferedImage(betslip.getWidth(), betslip.getHeight(), betslip.getType());
        Graphics2D g2d = markedImage.createGraphics();
        g2d.drawImage(betslip, 0, 0, null);
        g2d.setColor(Color.BLACK);

        for (int panel = 0; panel < panelCount; panel++) {
            int startX = xOffsets[panel];
            int startY = panel * panelHeight + yOffsets[panel];

            // Plot main ball coordinates
            for (int row = 0; row < mainBallRows; row++) {
                for (int col = 0; col < mainBallColumns; col++) {
                    int x = startX + col * mainBallHorizontalSpacing;
                    int y = startY + row * verticalSpacing;
                    String lotteryNumber = "Panel " + (panel + 1) + " Main Ball " + (row * mainBallColumns + col + 1);
                    System.out.println(lotteryNumber);
                    mainBallCoordinates.put(lotteryNumber, new Point(x, y));
                    g2d.fillRect(x, y, markingSize, markingSize);
                }
            }

            // Plot bonus ball coordinates if they exist
            if (bonusBallRows > 0 && bonusBallColumns > 0) {
                int bonusStartX = bonusXOffsets[panel];
                int bonusStartY = startY + mainBallRows * verticalSpacing + bonusYOffsets[panel];
                for (int row = 0; row < bonusBallRows; row++) {
                    for (int col = 0; col < bonusBallColumns; col++) {
                        int x = bonusStartX + col * bonusBallHorizontalSpacing;
                        int y = bonusStartY + row * verticalSpacing;
                        String lotteryNumber = "Panel " + (panel + 1) + " Bonus Ball " + (row * bonusBallColumns + col + 1);
                        System.out.println(lotteryNumber);
                        bonusBallCoordinates.put(lotteryNumber, new Point(x, y));
                        g2d.fillRect(x, y, markingSize, markingSize);
                    }
                }
            }
        }

        g2d.dispose();
        return markedImage;
    }

    public void saveCoordinatesToFile(String filePath) throws IOException {
        try (FileOutputStream fos = new FileOutputStream(filePath);
             ObjectOutputStream oos = new ObjectOutputStream(fos)) {
            oos.writeObject(mainBallCoordinates);
            if (bonusBallRows > 0 && bonusBallColumns > 0) {
                oos.writeObject(bonusBallCoordinates);
            }
        }
    }
}
