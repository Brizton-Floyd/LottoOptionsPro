package com.example.lottooptionspro.model.smart;

import java.util.Arrays;
import java.util.List;

public class UserPreferences {
    private boolean avoidConsecutive = false;
    private List<Integer> preferredNumbers;
    private List<Integer> excludeNumbers;
    private int minHotPatterns = 3;
    private int maxColdPatterns = 2;
    private double targetCoveragePercentage = 80.0;
    private double minQualityScore = 72.0;
    private boolean preferBalancedDistribution = true;
    private boolean enableMultiBatch = true;
    private int maxBatchVariance = 3;
    private boolean preventDuplicates = true;
    
    // TemplateMatrix Configuration Fields
    private boolean enableTemplateMatrix = false;
    private String templateSelectionStrategy = "BALANCED";
    private List<String> allowedTemplateGroups = Arrays.asList("BEST", "GOOD");
    private boolean useTimingIndicators = true;
    private boolean considerOverdueTemplates = true;
    private double minimumTemplateProbability = 0.01; // 1%
    private int numberSetsPerTemplate = 5;
    
    // Delta Strategy Configuration Fields
    private boolean enableDeltaStrategy = false;
    private String deltaPatternPreference = "BALANCED"; // BALANCED, AGGRESSIVE, CONSERVATIVE, CUSTOM
    private double deltaQualityThreshold = 0.7;
    private boolean enableDroughtIntelligence = true;
    private boolean enableTierOptimization = true;
    private int deltaVariationCount = 10;
    private String deltaComplexityLevel = "MEDIUM"; // LOW, MEDIUM, HIGH
    private boolean excludePreviousWinners = false;

    public boolean isAvoidConsecutive() {
        return avoidConsecutive;
    }

    public void setAvoidConsecutive(boolean avoidConsecutive) {
        this.avoidConsecutive = avoidConsecutive;
    }

    public List<Integer> getPreferredNumbers() {
        return preferredNumbers;
    }

    public void setPreferredNumbers(List<Integer> preferredNumbers) {
        this.preferredNumbers = preferredNumbers;
    }

    public List<Integer> getExcludeNumbers() {
        return excludeNumbers;
    }

    public void setExcludeNumbers(List<Integer> excludeNumbers) {
        this.excludeNumbers = excludeNumbers;
    }

    public int getMinHotPatterns() {
        return minHotPatterns;
    }

    public void setMinHotPatterns(int minHotPatterns) {
        this.minHotPatterns = minHotPatterns;
    }

    public int getMaxColdPatterns() {
        return maxColdPatterns;
    }

    public void setMaxColdPatterns(int maxColdPatterns) {
        this.maxColdPatterns = maxColdPatterns;
    }

    public double getTargetCoveragePercentage() {
        return targetCoveragePercentage;
    }

    public void setTargetCoveragePercentage(double targetCoveragePercentage) {
        this.targetCoveragePercentage = targetCoveragePercentage;
    }

    public double getMinQualityScore() {
        return minQualityScore;
    }

    public void setMinQualityScore(double minQualityScore) {
        this.minQualityScore = minQualityScore;
    }

    public boolean isPreferBalancedDistribution() {
        return preferBalancedDistribution;
    }

    public void setPreferBalancedDistribution(boolean preferBalancedDistribution) {
        this.preferBalancedDistribution = preferBalancedDistribution;
    }

    public boolean isEnableMultiBatch() {
        return enableMultiBatch;
    }

    public void setEnableMultiBatch(boolean enableMultiBatch) {
        this.enableMultiBatch = enableMultiBatch;
    }

    public int getMaxBatchVariance() {
        return maxBatchVariance;
    }

    public void setMaxBatchVariance(int maxBatchVariance) {
        this.maxBatchVariance = maxBatchVariance;
    }

    public boolean isPreventDuplicates() {
        return preventDuplicates;
    }

    public void setPreventDuplicates(boolean preventDuplicates) {
        this.preventDuplicates = preventDuplicates;
    }

    // TemplateMatrix Getters and Setters

    public boolean isEnableTemplateMatrix() {
        return enableTemplateMatrix;
    }

    public void setEnableTemplateMatrix(boolean enableTemplateMatrix) {
        this.enableTemplateMatrix = enableTemplateMatrix;
    }

    public String getTemplateSelectionStrategy() {
        return templateSelectionStrategy;
    }

    public void setTemplateSelectionStrategy(String templateSelectionStrategy) {
        this.templateSelectionStrategy = templateSelectionStrategy;
    }

    public List<String> getAllowedTemplateGroups() {
        return allowedTemplateGroups;
    }

    public void setAllowedTemplateGroups(List<String> allowedTemplateGroups) {
        this.allowedTemplateGroups = allowedTemplateGroups;
    }

    public boolean isUseTimingIndicators() {
        return useTimingIndicators;
    }

    public void setUseTimingIndicators(boolean useTimingIndicators) {
        this.useTimingIndicators = useTimingIndicators;
    }

    public boolean isConsiderOverdueTemplates() {
        return considerOverdueTemplates;
    }

    public void setConsiderOverdueTemplates(boolean considerOverdueTemplates) {
        this.considerOverdueTemplates = considerOverdueTemplates;
    }

    public double getMinimumTemplateProbability() {
        return minimumTemplateProbability;
    }

    public void setMinimumTemplateProbability(double minimumTemplateProbability) {
        this.minimumTemplateProbability = minimumTemplateProbability;
    }

    public int getNumberSetsPerTemplate() {
        return numberSetsPerTemplate;
    }

    public void setNumberSetsPerTemplate(int numberSetsPerTemplate) {
        this.numberSetsPerTemplate = numberSetsPerTemplate;
    }

    // Utility methods for TemplateMatrix
    
    public boolean hasTemplateGroupEnabled(String group) {
        return allowedTemplateGroups != null && allowedTemplateGroups.contains(group);
    }
    
    public boolean isTimingStrategyEnabled() {
        return "TIMING_AWARE".equals(templateSelectionStrategy) || "BALANCED".equals(templateSelectionStrategy);
    }
    
    public boolean isProbabilityStrategyEnabled() {
        return !"TIMING_AWARE".equals(templateSelectionStrategy);
    }
    
    public String getFormattedMinimumProbability() {
        return String.format("%.1f%%", minimumTemplateProbability * 100);
    }
    
    // Delta Strategy Getters and Setters
    
    public boolean isEnableDeltaStrategy() {
        return enableDeltaStrategy;
    }
    
    public void setEnableDeltaStrategy(boolean enableDeltaStrategy) {
        this.enableDeltaStrategy = enableDeltaStrategy;
    }
    
    public String getDeltaPatternPreference() {
        return deltaPatternPreference;
    }
    
    public void setDeltaPatternPreference(String deltaPatternPreference) {
        this.deltaPatternPreference = deltaPatternPreference;
    }
    
    public double getDeltaQualityThreshold() {
        return deltaQualityThreshold;
    }
    
    public void setDeltaQualityThreshold(double deltaQualityThreshold) {
        this.deltaQualityThreshold = deltaQualityThreshold;
    }
    
    public boolean isEnableDroughtIntelligence() {
        return enableDroughtIntelligence;
    }
    
    public void setEnableDroughtIntelligence(boolean enableDroughtIntelligence) {
        this.enableDroughtIntelligence = enableDroughtIntelligence;
    }
    
    public boolean isEnableTierOptimization() {
        return enableTierOptimization;
    }
    
    public void setEnableTierOptimization(boolean enableTierOptimization) {
        this.enableTierOptimization = enableTierOptimization;
    }
    
    public int getDeltaVariationCount() {
        return deltaVariationCount;
    }
    
    public void setDeltaVariationCount(int deltaVariationCount) {
        this.deltaVariationCount = deltaVariationCount;
    }
    
    public String getDeltaComplexityLevel() {
        return deltaComplexityLevel;
    }
    
    public void setDeltaComplexityLevel(String deltaComplexityLevel) {
        this.deltaComplexityLevel = deltaComplexityLevel;
    }
    
    public boolean isExcludePreviousWinners() {
        return excludePreviousWinners;
    }
    
    public void setExcludePreviousWinners(boolean excludePreviousWinners) {
        this.excludePreviousWinners = excludePreviousWinners;
    }
    
    // Delta Strategy Utility Methods
    
    public boolean isDeltaStrategyEnabled() {
        return enableDeltaStrategy;
    }
    
    public String getFormattedDeltaQualityThreshold() {
        return String.format("%.0f%%", deltaQualityThreshold * 100);
    }
}