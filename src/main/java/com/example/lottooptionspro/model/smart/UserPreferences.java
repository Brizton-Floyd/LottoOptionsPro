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
}