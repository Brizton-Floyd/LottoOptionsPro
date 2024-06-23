package com.example.lottooptionspro.util;

import com.example.lottooptionspro.models.LotteryGameBetSlipCoordinates;

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
    private Map<Integer, Map<String, Point>> mainBallCoordinates;
    private Map<Integer, Map<String, Point>> bonusBallCoordinates;
    private Point jackpotOptionCoordinate;
    private Boolean isNumbersOrientedVertically;
    private LotteryGameBetSlipCoordinates gameCoordinates;
    private boolean isBottomToTop;

    public LotteryBetslipProcessor(String imagePath, int panelCount, int mainBallRows, int bonusBallRows,
                                   int mainBallColumns, int bonusBallColumns, int[] xOffsets, int[] yOffsets,
                                   int[] bonusXOffsets, int[] bonusYOffsets, Point jackpotOptionCoordinate,
                                   LotteryGameBetSlipCoordinates gameCoordinates,
                                   boolean isNumbersOrientedVertically, boolean isBottomToTop) throws IOException {
        File imageFile = new File(imagePath);
        if (!imageFile.exists()) {
            throw new IOException("File not found: " + imagePath);
        }
        this.betslip = ImageResizer.resizeImageBasedOnTrueSize(ImageIO.read(imageFile), 8.5, 3.5);
        this.panelCount = panelCount;
        this.mainBallRows = mainBallRows;
        this.bonusBallRows = bonusBallRows;
        this.mainBallColumns = mainBallColumns;
        this.bonusBallColumns = bonusBallColumns;
        this.xOffsets = xOffsets;
        this.yOffsets = yOffsets;
        this.bonusXOffsets = bonusXOffsets;
        this.bonusYOffsets = bonusYOffsets;
        this.jackpotOptionCoordinate = jackpotOptionCoordinate;
        this.gameCoordinates = gameCoordinates;
        this.isNumbersOrientedVertically = isNumbersOrientedVertically;
        this.isBottomToTop = isBottomToTop;

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
        g2d.setColor(isNumbersOrientedVertically ? Color.BLACK : Color.RED);

        for (int panel = 0; panel < panelCount; panel++) {
            int startX = xOffsets[panel];
            int startY = panel * panelHeight + yOffsets[panel];

            // Plot main ball coordinates
            plotBallCoordinates(g2d, startX, startY, mainBallRows, mainBallColumns, mainBallHorizontalSpacing, verticalSpacing, true, panel);

            // Plot bonus ball coordinates if they exist
            if (bonusBallRows > 0 && bonusBallColumns > 0) {
                int bonusStartX = bonusXOffsets[panel];
                int bonusStartY = startY + mainBallRows * verticalSpacing + bonusYOffsets[panel];
                plotBallCoordinates(g2d, bonusStartX, bonusStartY, bonusBallRows, bonusBallColumns, bonusBallHorizontalSpacing, verticalSpacing, false, panel);
            }
        }

        // Plot jackpot option if present
        if (jackpotOptionCoordinate != null) {
            g2d.fillRect(jackpotOptionCoordinate.x, jackpotOptionCoordinate.y, markingSize, markingSize);
        }

        g2d.dispose();
        return markedImage;
    }

    private void plotBallCoordinates(Graphics2D g2d, int startX, int startY, int rows, int columns,
                                     int horizontalSpacing, int verticalSpacing, boolean isMainBall, int panel) {
        Map<String, Point> panelCoordinates = new HashMap<>();
        for (int row = 0; row < rows; row++) {
            for (int col = 0; col < columns; col++) {
                int x = startX + col * horizontalSpacing;
                int y = isBottomToTop ? startY + (rows - 1 - row) * verticalSpacing : startY + row * verticalSpacing;

                int lotteryNumber;
                if (isNumbersOrientedVertically) {
                    lotteryNumber = isBottomToTop ? (col * rows + row + 1) : (col * rows + (rows - row));
                } else {
                    lotteryNumber = isBottomToTop ? ((rows - 1 - row) * columns + col + 1) : (row * columns + col + 1);
                }

                Point point = new Point(x, y);
                panelCoordinates.put(String.valueOf(lotteryNumber), point);
                g2d.fillRect(x, y, markingSize, markingSize);
            }
        }
        if (isMainBall) {
            mainBallCoordinates.put(panel, panelCoordinates);
        } else {
            bonusBallCoordinates.put(panel, panelCoordinates);
        }
    }


    public void saveCoordinatesToFile(String filePath) throws IOException {
        LotteryGameBetSlipCoordinates coordinates =
                new LotteryGameBetSlipCoordinates(mainBallCoordinates, bonusBallCoordinates, jackpotOptionCoordinate, isNumbersOrientedVertically);
        try (FileOutputStream fos = new FileOutputStream(filePath);
             ObjectOutputStream oos = new ObjectOutputStream(fos)) {
            oos.writeObject(coordinates);
        }
    }
}
