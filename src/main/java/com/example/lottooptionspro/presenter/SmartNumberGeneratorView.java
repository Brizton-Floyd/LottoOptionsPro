package com.example.lottooptionspro.presenter;

import com.example.lottooptionspro.model.smart.QualityMetrics;
import com.example.lottooptionspro.model.smart.SmartGenerationRequest;
import com.example.lottooptionspro.model.smart.TicketGenerationResult;

import java.io.File;

public interface SmartNumberGeneratorView {
    
    SmartGenerationRequest createGenerationRequest();
    
    void showLoading(boolean show);
    
    void setContentDisabled(boolean disabled);
    
    void showGenerationProgress(boolean show);
    
    void updateProgress(double progress, String message);
    
    void updateSessionInfo(String sessionId);
    
    void updateQualityMetrics(QualityMetrics metrics);
    
    void updateTimeElapsed(double seconds);
    
    void showResults(TicketGenerationResult result);
    
    void enableGenerationControls(boolean enabled);
    
    void showAlert(String title, String message);
    
    void showLoadFullAnalysisButton(boolean show);
    
    void setLoadFullAnalysisButtonLoading(boolean loading);
    
    File showSaveDialog(String initialDirectory, String initialFileName);
}