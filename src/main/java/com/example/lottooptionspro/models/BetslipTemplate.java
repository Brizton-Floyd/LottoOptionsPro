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
    private GridConfiguration gridConfig; // Grid settings for main numbers (backward compatibility)
    private GridConfiguration mainNumberGridConfig; // Grid settings for main numbers
    private GridConfiguration bonusNumberGridConfig; // Grid settings for bonus numbers
    
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
    
    // Getters and setters for grid configurations
    public GridConfiguration getGridConfig() { 
        // For backward compatibility, return mainNumberGridConfig if available, otherwise gridConfig
        return mainNumberGridConfig != null ? mainNumberGridConfig : gridConfig; 
    }
    public void setGridConfig(GridConfiguration gridConfig) { 
        this.gridConfig = gridConfig; 
        // Also set as main number config if not already set
        if (this.mainNumberGridConfig == null) {
            this.mainNumberGridConfig = gridConfig;
        }
    }
    
    public GridConfiguration getMainNumberGridConfig() { return mainNumberGridConfig; }
    public void setMainNumberGridConfig(GridConfiguration config) { this.mainNumberGridConfig = config; }
    
    public GridConfiguration getBonusNumberGridConfig() { return bonusNumberGridConfig; }
    public void setBonusNumberGridConfig(GridConfiguration config) { this.bonusNumberGridConfig = config; }
}
