package com.example.lottooptionspro.model.range;

public enum AnalysisType {
    ACTUAL("Standard lottery analysis with position-specific patterns"),
    DELTA("Spacing pattern analysis for consistent gap patterns"),
    DELTA_SORTED("Normalized pattern recognition for order-independent analysis");

    private final String description;

    AnalysisType(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    public String getDisplayName() {
        return name().replace("_", " ");
    }
}