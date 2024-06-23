package com.example.lottooptionspro.models;

import java.awt.*;
import java.io.Serializable;
import java.util.Map;

public class LotteryGameBetSlipCoordinates implements Serializable {
    private static final long serialVersionUID = 1L;

    private Map<String, Point> mainBallCoordinates;
    private Map<String, Point> bonusBallCoordinates;
    private Point jackpotOptionCoordinate;

    public LotteryGameBetSlipCoordinates(Map<String, Point> mainBallCoordinates, Map<String, Point> bonusBallCoordinates,
                                         Point jackpotOptionCoordinate) {
        this.mainBallCoordinates = mainBallCoordinates;
        this.bonusBallCoordinates = bonusBallCoordinates;
        this.jackpotOptionCoordinate = jackpotOptionCoordinate;
    }

    public Point getJackpotOptionCoordinate() {
        return this.jackpotOptionCoordinate;
    }
    public Map<String, Point> getMainBallCoordinates() {
        return mainBallCoordinates;
    }

    public void setMainBallCoordinates(Map<String, Point> mainBallCoordinates) {
        this.mainBallCoordinates = mainBallCoordinates;
    }

    public Map<String, Point> getBonusBallCoordinates() {
        return bonusBallCoordinates;
    }

    public void setBonusBallCoordinates(Map<String, Point> bonusBallCoordinates) {
        this.bonusBallCoordinates = bonusBallCoordinates;
    }
}