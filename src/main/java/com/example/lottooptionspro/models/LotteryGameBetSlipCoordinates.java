package com.example.lottooptionspro.models;

import java.awt.Point;
import java.io.Serializable;
import java.util.Map;

public class LotteryGameBetSlipCoordinates implements Serializable {
    private static final long serialVersionUID = 1L;
    private Map<Integer, Map<String, Point>> mainBallCoordinates;
    private Map<Integer, Map<String, Point>> bonusBallCoordinates;
    private Point jackpotOptionCoordinate;
    private boolean isVerticalOrientation;

    public LotteryGameBetSlipCoordinates(Map<Integer, Map<String, Point>> mainBallCoordinates,
                                         Map<Integer, Map<String, Point>> bonusBallCoordinates,
                                         Point jackpotOptionCoordinate,
                                         boolean isVerticalOrientation) {
        this.mainBallCoordinates = mainBallCoordinates;
        this.bonusBallCoordinates = bonusBallCoordinates;
        this.jackpotOptionCoordinate = jackpotOptionCoordinate;
        this.isVerticalOrientation = isVerticalOrientation;
    }

    /**
     * Get the coordinate for a ball based on its position and orientation settings.
     *
     * @param row                The row index of the ball.
     * @param col                The column index of the ball.
     * @param startX             The starting X coordinate.
     * @param startY             The starting Y coordinate.
     * @param horizontalSpacing  The horizontal spacing between balls.
     * @param verticalSpacing    The vertical spacing between balls.
     * @param totalRows          The total number of rows.
     * @param isVertical         Whether the orientation is vertical.
     * @param isBottomToTop      Whether the plotting is from bottom to top.
     * @return                   The calculated coordinate for the ball.
     */
    public Point getBallCoordinate(int row, int col, int startX, int startY,
                                   int horizontalSpacing, int verticalSpacing,
                                   int totalRows, boolean isVertical, boolean isBottomToTop) {
        if (isVertical) {
            return getVerticalCoordinate(row, col, startX, startY, horizontalSpacing, verticalSpacing, totalRows, isBottomToTop);
        } else {
            return getHorizontalCoordinate(row, col, startX, startY, horizontalSpacing, verticalSpacing, totalRows, isBottomToTop);
        }
    }

    private Point getHorizontalCoordinate(int row, int col, int startX, int startY,
                                          int horizontalSpacing, int verticalSpacing,
                                          int totalRows, boolean isBottomToTop) {
        int x = startX + col * horizontalSpacing;
        int y = isBottomToTop ? startY + (totalRows - 1 - row) * verticalSpacing : startY + row * verticalSpacing;
        return new Point(x, y);
    }

    private Point getVerticalCoordinate(int row, int col, int startX, int startY,
                                        int horizontalSpacing, int verticalSpacing,
                                        int totalRows, boolean isBottomToTop) {
        int x = startX + col * horizontalSpacing;
        int y = isBottomToTop ? startY + (totalRows - 1 - row) * verticalSpacing : startY + row * verticalSpacing;
        return new Point(x, y);
    }

    public Map<Integer, Map<String, Point>> getMainBallCoordinates() {
        return mainBallCoordinates;
    }

    public Map<Integer, Map<String, Point>> getBonusBallCoordinates() {
        return bonusBallCoordinates;
    }

    public Point getJackpotOptionCoordinate() {
        return jackpotOptionCoordinate;
    }

    public boolean isVerticalOrientation() {
        return isVerticalOrientation;
    }

    public void setVerticalOrientation(boolean verticalOrientation) {
        isVerticalOrientation = verticalOrientation;
    }
}
