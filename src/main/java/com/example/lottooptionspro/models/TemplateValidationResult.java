package com.example.lottooptionspro.models;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Represents the overall template validation result across all panels
 */
public class TemplateValidationResult {
    private final Map<String, PanelValidationResult> panelResults;
    private int totalTestsRun;
    private boolean overallPass;
    private long validationTimeMs;
    private int configuredPanels;
    private int totalCoordinatesMapped;

    public TemplateValidationResult() {
        this.panelResults = new HashMap<>();
        this.totalTestsRun = 0;
        this.overallPass = false;
        this.validationTimeMs = 0;
        this.configuredPanels = 0;
        this.totalCoordinatesMapped = 0;
    }

    // Getters and setters
    public Map<String, PanelValidationResult> getPanelResults() {
        return panelResults;
    }

    public void addPanelResult(PanelValidationResult result) {
        this.panelResults.put(result.getPanelId(), result);
        this.totalCoordinatesMapped += result.getCoordinatesMapped();
        if (result.getCoordinatesMapped() > 0) {
            this.configuredPanels++;
        }
    }

    public PanelValidationResult getPanelResult(String panelId) {
        return panelResults.get(panelId);
    }

    public int getTotalTestsRun() {
        return totalTestsRun;
    }

    public void setTotalTestsRun(int totalTestsRun) {
        this.totalTestsRun = totalTestsRun;
    }

    public boolean isOverallPass() {
        return overallPass;
    }

    public void setOverallPass(boolean overallPass) {
        this.overallPass = overallPass;
    }

    public long getValidationTimeMs() {
        return validationTimeMs;
    }

    public void setValidationTimeMs(long validationTimeMs) {
        this.validationTimeMs = validationTimeMs;
    }

    public int getConfiguredPanels() {
        return configuredPanels;
    }

    public int getTotalCoordinatesMapped() {
        return totalCoordinatesMapped;
    }

    /**
     * Calculate overall pass status based on all configured panels
     */
    public void calculateOverallStatus() {
        this.overallPass = panelResults.values().stream()
                .filter(result -> result.getCoordinatesMapped() > 0) // Only check configured panels
                .allMatch(PanelValidationResult::isPassed);
    }

    /**
     * Get list of panels with errors
     */
    public List<PanelValidationResult> getPanelsWithErrors() {
        return panelResults.values().stream()
                .filter(result -> !result.getErrors().isEmpty())
                .collect(Collectors.toList());
    }

    /**
     * Get list of configured panels (panels with coordinate data)
     */
    public List<PanelValidationResult> getConfiguredPanelResults() {
        return panelResults.values().stream()
                .filter(result -> result.getCoordinatesMapped() > 0)
                .collect(Collectors.toList());
    }

    /**
     * Get average coverage across all configured panels
     */
    public double getAverageCoverage() {
        List<PanelValidationResult> configured = getConfiguredPanelResults();
        if (configured.isEmpty()) {
            return 0.0;
        }
        
        return configured.stream()
                .mapToDouble(PanelValidationResult::getCoveragePercentage)
                .average()
                .orElse(0.0);
    }

    /**
     * Generate a comprehensive validation report
     */
    public String generateReport() {
        StringBuilder report = new StringBuilder();
        
        report.append("Template Validation Report\n");
        report.append("========================\n");
        report.append("Overall Status: ").append(overallPass ? "✓ PASSED" : "✗ FAILED");
        report.append(" (").append(totalTestsRun).append(" tests)\n");
        report.append("Validation Time: ").append(validationTimeMs).append("ms\n");
        report.append("Configured Panels: ").append(configuredPanels).append("/5\n");
        report.append("Total Coordinates: ").append(totalCoordinatesMapped).append("\n");
        report.append("Average Coverage: ").append(String.format("%.1f", getAverageCoverage())).append("%\n\n");
        
        // Panel details
        for (String panelId : List.of("A", "B", "C", "D", "E")) {
            PanelValidationResult result = panelResults.get(panelId);
            if (result != null) {
                report.append(result.getSummary()).append("\n\n");
            }
        }
        
        // Global Options details (if present)
        PanelValidationResult globalResult = panelResults.get("GLOBAL");
        if (globalResult != null) {
            report.append(globalResult.getSummary()).append("\n\n");
        }
        
        // Error summary
        List<PanelValidationResult> panelsWithErrors = getPanelsWithErrors();
        if (!panelsWithErrors.isEmpty()) {
            report.append("Errors Found:\n");
            report.append("=============\n");
            for (PanelValidationResult panel : panelsWithErrors) {
                report.append("Panel ").append(panel.getPanelId()).append(":\n");
                for (String error : panel.getErrors()) {
                    report.append("  - ").append(error).append("\n");
                }
            }
        }
        
        return report.toString();
    }
}