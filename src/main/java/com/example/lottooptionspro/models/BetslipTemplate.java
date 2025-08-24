package com.example.lottooptionspro.models;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BetslipTemplate {
    private String gameName;
    private String jurisdiction;
    private String imagePath;
    private Mark mark;
    private List<PlayPanel> playPanels;
    private Map<String, Coordinate> globalOptions;
}
