package com.example.lottooptionspro.models;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PlayPanel {
    private String panelId;
    private Map<String, Coordinate> mainNumbers;
    private Map<String, Coordinate> bonusNumbers;
    private Coordinate quickPick;
}
