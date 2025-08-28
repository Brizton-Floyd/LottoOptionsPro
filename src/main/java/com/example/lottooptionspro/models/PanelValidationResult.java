package com.example.lottooptionspro.models;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Represents the validation result for a single panel
 */
public class PanelValidationResult {
    private final String panelId;
    private boolean passed;
    private int coordinatesMapped;
    private int testsRun;
    private final List<String> errors;
    private final Set<String> numbersTested;
    private double coveragePercentage;
    private int expectedNumbers;

    public PanelValidationResult(String panelId) {
        this.panelId = panelId;
        this.passed = false;
        this.coordinatesMapped = 0;
        this.testsRun = 0;
        this.errors = new ArrayList<>();
        this.numbersTested = new HashSet<>();
        this.coveragePercentage = 0.0;
        this.expectedNumbers = 0;
    }

    // Getters and setters
    public String getPanelId() {
        return panelId;
    }

    public boolean isPassed() {
        return passed;
    }

    public void setPassed(boolean passed) {
        this.passed = passed;
    }

    public int getCoordinatesMapped() {
        return coordinatesMapped;
    }

    public void setCoordinatesMapped(int coordinatesMapped) {
        this.coordinatesMapped = coordinatesMapped;
    }

    public int getTestsRun() {
        return testsRun;
    }

    public void setTestsRun(int testsRun) {
        this.testsRun = testsRun;
    }

    public List<String> getErrors() {
        return errors;
    }

    public void addError(String error) {
        this.errors.add(error);
    }

    public Set<String> getNumbersTested() {
        return numbersTested;
    }

    public void addTestedNumber(String number) {
        this.numbersTested.add(number);
    }

    public double getCoveragePercentage() {
        return coveragePercentage;
    }

    public void setCoveragePercentage(double coveragePercentage) {
        this.coveragePercentage = coveragePercentage;
    }

    public int getExpectedNumbers() {
        return expectedNumbers;
    }

    public void setExpectedNumbers(int expectedNumbers) {
        this.expectedNumbers = expectedNumbers;
    }

    /**
     * Calculate and update coverage percentage based on tested numbers vs expected
     */
    public void updateCoverage() {
        if (expectedNumbers > 0) {
            this.coveragePercentage = (double) numbersTested.size() / expectedNumbers * 100.0;
        }
    }

    /**
     * Generate a summary string for this panel's validation
     */
    public String getSummary() {
        StringBuilder sb = new StringBuilder();
        
        // Handle special case for Global Options
        if ("GLOBAL".equals(panelId)) {
            sb.append("Global Options: ");
        } else {
            sb.append("Panel ").append(panelId).append(": ");
        }
        
        if (passed) {
            sb.append("✓ PASSED");
        } else if (coordinatesMapped == 0) {
            sb.append("✗ NOT CONFIGURED");
        } else {
            sb.append("⚠ PARTIAL");
        }
        
        if (coordinatesMapped > 0) {
            if ("GLOBAL".equals(panelId)) {
                sb.append("\n- Global Options configured: ").append(coordinatesMapped);
            } else {
                sb.append("\n- Coordinates mapped: ").append(coordinatesMapped);
                if (expectedNumbers > 0) {
                    sb.append("/").append(expectedNumbers);
                }
            }
            sb.append("\n- Coverage: ").append(String.format("%.1f", coveragePercentage)).append("%");
            sb.append("\n- Tests passed: ").append(testsRun);
            
            if (!errors.isEmpty()) {
                sb.append("\n- Errors: ").append(errors.size());
            }
        }
        
        return sb.toString();
    }
}