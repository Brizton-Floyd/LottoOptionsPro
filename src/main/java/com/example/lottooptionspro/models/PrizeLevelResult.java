package com.example.lottooptionspro.models;

import javafx.beans.property.DoubleProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;
import jdk.jfr.DataAmount;
import lombok.Data;

@Data
public class PrizeLevelResult {
    private final IntegerProperty correctNumbers = new SimpleIntegerProperty();
    private final IntegerProperty hits = new SimpleIntegerProperty();
    private final IntegerProperty gamesOut = new SimpleIntegerProperty();
    private final DoubleProperty expectedElapsedDaysBeforeWin = new SimpleDoubleProperty();

    public PrizeLevelResult(int correctNumbers, int hits, int gamesOut, double expectedElapsedDaysBeforeWin) {
        this.correctNumbers.set(correctNumbers);
        this.hits.set(hits);
        this.gamesOut.set(gamesOut);
        this.expectedElapsedDaysBeforeWin.set(expectedElapsedDaysBeforeWin);
    }
}
