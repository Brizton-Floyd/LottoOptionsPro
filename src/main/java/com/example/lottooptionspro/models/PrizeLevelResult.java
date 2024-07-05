package com.example.lottooptionspro.models;

import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import jdk.jfr.DataAmount;
import lombok.Data;

@Data
public class PrizeLevelResult {
    private final IntegerProperty correctNumbers = new SimpleIntegerProperty();
    private final IntegerProperty hits = new SimpleIntegerProperty();
    private final IntegerProperty gamesOut = new SimpleIntegerProperty();

    public PrizeLevelResult(int correctNumbers, int hits, int gamesOut) {
        this.correctNumbers.set(correctNumbers);
        this.hits.set(hits);
        this.gamesOut.set(gamesOut);
    }
}
