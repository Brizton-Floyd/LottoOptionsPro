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
    private List<GlobalOption> globalOptions;
    private List<ScannerMark> scannerMarks;
    private GridConfiguration gridConfig; // Grid settings for this template
    
    // Constructor for backward compatibility (without gridConfig)
    public BetslipTemplate(String gameName, String jurisdiction, String imagePath, Mark mark,
                          List<PlayPanel> playPanels, List<GlobalOption> globalOptions,
                          List<ScannerMark> scannerMarks) {
        this.gameName = gameName;
        this.jurisdiction = jurisdiction;
        this.imagePath = imagePath;
        this.mark = mark;
        this.playPanels = playPanels;
        this.globalOptions = globalOptions;
        this.scannerMarks = scannerMarks;
        this.gridConfig = null; // No grid config in old format
    }
    
    // Getter and setter for gridConfig (Lombok will generate the others)
    public GridConfiguration getGridConfig() { return gridConfig; }
    public void setGridConfig(GridConfiguration gridConfig) { this.gridConfig = gridConfig; }
}
