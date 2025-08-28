package com.example.lottooptionspro.presenter;

import com.example.lottooptionspro.models.Coordinate;
import com.example.lottooptionspro.models.GlobalOption;
import com.example.lottooptionspro.models.GridDefinition;
import com.example.lottooptionspro.models.GridMappingMode;
import com.example.lottooptionspro.models.ScannerMark;

import java.io.File;
import java.util.Optional;

public interface TemplateCreatorView {
    void showView();
    File showOpenImageDialog();
    File showOpenTemplateDialog();
    File showSaveDialog(String initialFileName);
    void displayImage(String imagePath);
    void clearAllRectangles();
    void clearPanelRectangles(String panelId);
    void drawRectangle(Coordinate coordinate, int width, int height);
    void drawRectangle(Coordinate coordinate, int width, int height, String type);
    void drawRectangle(Coordinate coordinate, int width, int height, String type, String panelId);
    void drawScannerMark(ScannerMark mark);
    void drawGlobalOption(GlobalOption globalOption);
    void drawPreviewRectangle(Coordinate coordinate, int width, int height);
    void clearPreviewRectangles();
    String getGameName();
    String getJurisdiction();
    void setGameName(String name);
    void setJurisdiction(String name);
    String getSelectedMappingMode();
    String getSelectedPanel();
    String getGlobalOptionName();
    void setGlobalOptionName(String name);
    void showError(String message);
    void showSuccess(String message);
    Optional<String> askForPreviewNumbers();
    
    // Validation methods
    void setValidationStatus(String status);
    void setValidationProgress(double progress);
    void showValidationProgress(boolean visible);
    void drawValidationMark(Coordinate coordinate, int width, int height, String panelId, String color);
    void setSelectedMarkControlsVisible(boolean visible, String markType);
    void setSelectedMarkDimensions(double width, double height);
    void setScannerMarkCount(int count);
    void updateScannerMarkRectangle(ScannerMark mark, double width, double height);
    void updateGlobalOptionRectangle(GlobalOption globalOption, double width, double height);
    void selectScannerMark(ScannerMark mark);  // Auto-select newly placed scanner mark
    
    // Grid mapping methods
    void setGridMappingControlsVisible(boolean visible);
    void setGridMappingMode(GridMappingMode mode);
    void showGridCornerMarker(double x, double y, boolean isFirst);
    void showGridOverlay(GridDefinition grid);
    void clearGridVisuals();
    void updateGridMappingStatus(String status);
    String getGridNumberRange();
    int getGridColumns();
    int getGridRows();
    String getGridFillOrder();
    boolean isGridModeEnabled();
    void showGridPreview(GridDefinition grid);
    boolean confirmGridMapping(String message);
    void setGridMappingProgress(int current, int total);
    
    // Grid configuration setters (for auto-population)
    void setGridNumberRange(String range);
    void setGridColumns(int columns);
    void setGridRows(int rows);
    void setGridFillOrder(String fillOrder);
}
