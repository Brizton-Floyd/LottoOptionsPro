//package com.example.lottooptionspro.controller;
//
//import com.example.lottooptionspro.models.LotteryGameBetSlipCoordinates;
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.Test;
//
//import java.awt.Point;
//import java.util.HashMap;
//import java.util.Map;
//
//import static org.junit.jupiter.api.Assertions.assertEquals;
//
//public class LotteryGameBetSlipCoordinatesTest {
//
//    private LotteryGameBetSlipCoordinates coordinates;
//    private Map<String, Point> mainBallCoordinates;
//    private Map<String, Point> bonusBallCoordinates;
//    private Point jackpotOptionCoordinate;
//
//    @BeforeEach
//    public void setUp() {
//        mainBallCoordinates = new HashMap<>();
//        bonusBallCoordinates = new HashMap<>();
//        jackpotOptionCoordinate = new Point(100, 200);
//        coordinates = new LotteryGameBetSlipCoordinates(mainBallCoordinates, bonusBallCoordinates, jackpotOptionCoordinate, true);
//    }
//
//    @Test
//    public void testGetMainBallCoordinateHorizontal() {
//        coordinates.setVerticalOrientation(false);
//        Point expected = new Point(110, 220);
//        Point actual = coordinates.getMainBallCoordinate(1, 1, 100, 200, 10, 20, 5, 5);
//        assertEquals(expected, actual);
//    }
//
//    @Test
//    public void testGetMainBallCoordinateVertical() {
//        coordinates.setVerticalOrientation(true);
//        Point expected = new Point(110, 220); // Updated expected value
//        Point actual = coordinates.getMainBallCoordinate(1, 1, 100, 200, 10, 20, 5, 5);
//        assertEquals(expected, actual);
//    }
//
//    @Test
//    public void testGetBonusBallCoordinateHorizontal() {
//        coordinates.setVerticalOrientation(false);
//        Point expected = new Point(110, 220);
//        Point actual = coordinates.getBonusBallCoordinate(1, 1, 100, 200, 10, 20, 5, 5);
//        assertEquals(expected, actual);
//    }
//
//    @Test
//    public void testGetBonusBallCoordinateVertical() {
//        coordinates.setVerticalOrientation(true);
//        Point expected = new Point(110, 220); // Updated expected value
//        Point actual = coordinates.getBonusBallCoordinate(1, 1, 100, 200, 10, 20, 5, 5);
//        assertEquals(expected, actual);
//    }
//
//    @Test
//    public void testGetJackpotOptionCoordinate() {
//        assertEquals(jackpotOptionCoordinate, coordinates.getJackpotOptionCoordinate());
//    }
//
//    @Test
//    public void testIsVerticalOrientation() {
//        assertEquals(true, coordinates.isVerticalOrientation());
//        coordinates.setVerticalOrientation(false);
//        assertEquals(false, coordinates.isVerticalOrientation());
//    }
//}