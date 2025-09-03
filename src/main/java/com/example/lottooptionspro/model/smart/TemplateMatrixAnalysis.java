package com.example.lottooptionspro.model.smart;

import java.util.List;
import java.util.Map;

public class TemplateMatrixAnalysis {
    private String lotteryConfigId;
    private String analysisTimestamp;
    private int totalTemplates;
    private int totalDrawsAnalyzed;
    private boolean hasAnalysis;
    private double enhancedQualityScore;
    private String templateQualityGrade;
    
    // Template Group Distribution
    private Map<String, Double> templateGroupDistribution;
    
    // Core Template Metrics
    private TemplateMetrics templateMetrics;
    
    // Template Strategy Information
    private StrategyInfo strategyInfo;
    
    // Quality Classification
    private String qualityTier;
    private boolean meetsPremiumStandards;
    private boolean meetsProfessionalStandards;
    
    // Timing Indicator Distribution
    private Map<String, Integer> timingIndicatorDistribution;
    
    // Individual Template Details
    private List<TemplateDetail> templateDetails;
    private List<TemplateDetail> allTemplates;
    private Map<String, List<TemplateDetail>> templatesByGroup;
    private List<TemplateDetail> recommendedTemplates;
    private List<TemplateDetail> overdueTemplates;
    
    // Quality Improvement Analysis
    private ImprovementAnalysis improvementAnalysis;
    
    // Intelligent Recommendations
    private Recommendations recommendations;
    
    // Benchmark Assessment
    private BenchmarkAssessment benchmarkAssessment;
    
    // Distribution Metrics
    private DistributionMetrics distributionMetrics;

    // Default constructor
    public TemplateMatrixAnalysis() {
        this.hasAnalysis = false;
    }

    // Getters and Setters
    public String getLotteryConfigId() {
        return lotteryConfigId;
    }

    public void setLotteryConfigId(String lotteryConfigId) {
        this.lotteryConfigId = lotteryConfigId;
    }

    public String getAnalysisTimestamp() {
        return analysisTimestamp;
    }

    public void setAnalysisTimestamp(String analysisTimestamp) {
        this.analysisTimestamp = analysisTimestamp;
    }

    public int getTotalTemplates() {
        return totalTemplates;
    }

    public void setTotalTemplates(int totalTemplates) {
        this.totalTemplates = totalTemplates;
    }

    public int getTotalDrawsAnalyzed() {
        return totalDrawsAnalyzed;
    }

    public void setTotalDrawsAnalyzed(int totalDrawsAnalyzed) {
        this.totalDrawsAnalyzed = totalDrawsAnalyzed;
    }

    public boolean isHasAnalysis() {
        return hasAnalysis;
    }

    public void setHasAnalysis(boolean hasAnalysis) {
        this.hasAnalysis = hasAnalysis;
    }

    public double getEnhancedQualityScore() {
        return enhancedQualityScore;
    }

    public void setEnhancedQualityScore(double enhancedQualityScore) {
        this.enhancedQualityScore = enhancedQualityScore;
    }

    public String getTemplateQualityGrade() {
        return templateQualityGrade;
    }

    public void setTemplateQualityGrade(String templateQualityGrade) {
        this.templateQualityGrade = templateQualityGrade;
    }

    public Map<String, Double> getTemplateGroupDistribution() {
        return templateGroupDistribution;
    }

    public void setTemplateGroupDistribution(Map<String, Double> templateGroupDistribution) {
        this.templateGroupDistribution = templateGroupDistribution;
    }

    public TemplateMetrics getTemplateMetrics() {
        return templateMetrics;
    }

    public void setTemplateMetrics(TemplateMetrics templateMetrics) {
        this.templateMetrics = templateMetrics;
    }

    public StrategyInfo getStrategyInfo() {
        return strategyInfo;
    }

    public void setStrategyInfo(StrategyInfo strategyInfo) {
        this.strategyInfo = strategyInfo;
    }

    public String getQualityTier() {
        return qualityTier;
    }

    public void setQualityTier(String qualityTier) {
        this.qualityTier = qualityTier;
    }

    public boolean isMeetsPremiumStandards() {
        return meetsPremiumStandards;
    }

    public void setMeetsPremiumStandards(boolean meetsPremiumStandards) {
        this.meetsPremiumStandards = meetsPremiumStandards;
    }

    public boolean isMeetsProfessionalStandards() {
        return meetsProfessionalStandards;
    }

    public void setMeetsProfessionalStandards(boolean meetsProfessionalStandards) {
        this.meetsProfessionalStandards = meetsProfessionalStandards;
    }

    public Map<String, Integer> getTimingIndicatorDistribution() {
        return timingIndicatorDistribution;
    }

    public void setTimingIndicatorDistribution(Map<String, Integer> timingIndicatorDistribution) {
        this.timingIndicatorDistribution = timingIndicatorDistribution;
    }

    public List<TemplateDetail> getTemplateDetails() {
        return templateDetails;
    }

    public void setTemplateDetails(List<TemplateDetail> templateDetails) {
        this.templateDetails = templateDetails;
    }

    public List<TemplateDetail> getAllTemplates() {
        return allTemplates;
    }

    public void setAllTemplates(List<TemplateDetail> allTemplates) {
        this.allTemplates = allTemplates;
    }

    public Map<String, List<TemplateDetail>> getTemplatesByGroup() {
        return templatesByGroup;
    }

    public void setTemplatesByGroup(Map<String, List<TemplateDetail>> templatesByGroup) {
        this.templatesByGroup = templatesByGroup;
    }

    public List<TemplateDetail> getRecommendedTemplates() {
        return recommendedTemplates;
    }

    public void setRecommendedTemplates(List<TemplateDetail> recommendedTemplates) {
        this.recommendedTemplates = recommendedTemplates;
    }

    public List<TemplateDetail> getOverdueTemplates() {
        return overdueTemplates;
    }

    public void setOverdueTemplates(List<TemplateDetail> overdueTemplates) {
        this.overdueTemplates = overdueTemplates;
    }

    public ImprovementAnalysis getImprovementAnalysis() {
        return improvementAnalysis;
    }

    public void setImprovementAnalysis(ImprovementAnalysis improvementAnalysis) {
        this.improvementAnalysis = improvementAnalysis;
    }

    public Recommendations getRecommendations() {
        return recommendations;
    }

    public void setRecommendations(Recommendations recommendations) {
        this.recommendations = recommendations;
    }

    public BenchmarkAssessment getBenchmarkAssessment() {
        return benchmarkAssessment;
    }

    public void setBenchmarkAssessment(BenchmarkAssessment benchmarkAssessment) {
        this.benchmarkAssessment = benchmarkAssessment;
    }

    public DistributionMetrics getDistributionMetrics() {
        return distributionMetrics;
    }

    public void setDistributionMetrics(DistributionMetrics distributionMetrics) {
        this.distributionMetrics = distributionMetrics;
    }

    // Nested Classes for Complex Objects

    public static class TemplateMetrics {
        private double templateProbabilityScore;
        private double templateCoveragePercentage;
        private double averageTemplateProbability;
        private int uniqueTemplatesUsed;
        private double mathematicalAccuracyScore;
        private double probabilityDistributionScore;
        private double templateEfficiencyScore;
        private double templateBalanceScore;
        private double templateDiversityScore;
        private double strategicFitScore;
        private double overdueTemplateUtilization;

        // Getters and Setters
        public double getTemplateProbabilityScore() { return templateProbabilityScore; }
        public void setTemplateProbabilityScore(double templateProbabilityScore) { this.templateProbabilityScore = templateProbabilityScore; }

        public double getTemplateCoveragePercentage() { return templateCoveragePercentage; }
        public void setTemplateCoveragePercentage(double templateCoveragePercentage) { this.templateCoveragePercentage = templateCoveragePercentage; }

        public double getAverageTemplateProbability() { return averageTemplateProbability; }
        public void setAverageTemplateProbability(double averageTemplateProbability) { this.averageTemplateProbability = averageTemplateProbability; }

        public int getUniqueTemplatesUsed() { return uniqueTemplatesUsed; }
        public void setUniqueTemplatesUsed(int uniqueTemplatesUsed) { this.uniqueTemplatesUsed = uniqueTemplatesUsed; }

        public double getMathematicalAccuracyScore() { return mathematicalAccuracyScore; }
        public void setMathematicalAccuracyScore(double mathematicalAccuracyScore) { this.mathematicalAccuracyScore = mathematicalAccuracyScore; }

        public double getProbabilityDistributionScore() { return probabilityDistributionScore; }
        public void setProbabilityDistributionScore(double probabilityDistributionScore) { this.probabilityDistributionScore = probabilityDistributionScore; }

        public double getTemplateEfficiencyScore() { return templateEfficiencyScore; }
        public void setTemplateEfficiencyScore(double templateEfficiencyScore) { this.templateEfficiencyScore = templateEfficiencyScore; }

        public double getTemplateBalanceScore() { return templateBalanceScore; }
        public void setTemplateBalanceScore(double templateBalanceScore) { this.templateBalanceScore = templateBalanceScore; }

        public double getTemplateDiversityScore() { return templateDiversityScore; }
        public void setTemplateDiversityScore(double templateDiversityScore) { this.templateDiversityScore = templateDiversityScore; }

        public double getStrategicFitScore() { return strategicFitScore; }
        public void setStrategicFitScore(double strategicFitScore) { this.strategicFitScore = strategicFitScore; }

        public double getOverdueTemplateUtilization() { return overdueTemplateUtilization; }
        public void setOverdueTemplateUtilization(double overdueTemplateUtilization) { this.overdueTemplateUtilization = overdueTemplateUtilization; }
    }

    public static class StrategyInfo {
        private String selectionStrategy;
        private List<String> allowedGroups;
        private boolean useTimingIndicators;
        private boolean considerOverdueTemplates;
        private double minimumProbabilityThreshold;
        private int numberSetsPerTemplate;

        // Getters and Setters
        public String getSelectionStrategy() { return selectionStrategy; }
        public void setSelectionStrategy(String selectionStrategy) { this.selectionStrategy = selectionStrategy; }

        public List<String> getAllowedGroups() { return allowedGroups; }
        public void setAllowedGroups(List<String> allowedGroups) { this.allowedGroups = allowedGroups; }

        public boolean isUseTimingIndicators() { return useTimingIndicators; }
        public void setUseTimingIndicators(boolean useTimingIndicators) { this.useTimingIndicators = useTimingIndicators; }

        public boolean isConsiderOverdueTemplates() { return considerOverdueTemplates; }
        public void setConsiderOverdueTemplates(boolean considerOverdueTemplates) { this.considerOverdueTemplates = considerOverdueTemplates; }

        public double getMinimumProbabilityThreshold() { return minimumProbabilityThreshold; }
        public void setMinimumProbabilityThreshold(double minimumProbabilityThreshold) { this.minimumProbabilityThreshold = minimumProbabilityThreshold; }

        public int getNumberSetsPerTemplate() { return numberSetsPerTemplate; }
        public void setNumberSetsPerTemplate(int numberSetsPerTemplate) { this.numberSetsPerTemplate = numberSetsPerTemplate; }
    }

    public static class ImprovementAnalysis {
        private double improvementPercentage;
        private String description;
        private boolean isSignificantImprovement;

        // Getters and Setters
        public double getImprovementPercentage() { return improvementPercentage; }
        public void setImprovementPercentage(double improvementPercentage) { this.improvementPercentage = improvementPercentage; }

        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }

        public boolean isSignificantImprovement() { return isSignificantImprovement; }
        public void setSignificantImprovement(boolean significantImprovement) { this.isSignificantImprovement = significantImprovement; }
    }

    public static class Recommendations {
        private String currentTier;
        private String targetTier;
        private String priorityAction;
        private List<String> suggestions;

        // Getters and Setters
        public String getCurrentTier() { return currentTier; }
        public void setCurrentTier(String currentTier) { this.currentTier = currentTier; }

        public String getTargetTier() { return targetTier; }
        public void setTargetTier(String targetTier) { this.targetTier = targetTier; }

        public String getPriorityAction() { return priorityAction; }
        public void setPriorityAction(String priorityAction) { this.priorityAction = priorityAction; }

        public List<String> getSuggestions() { return suggestions; }
        public void setSuggestions(List<String> suggestions) { this.suggestions = suggestions; }
    }

    public static class BenchmarkAssessment {
        private boolean meetsAllTargets;
        private double qualityScoreGap;
        private double bestTemplateGap;
        private double coverageGap;
        private double mathematicalAccuracyGap;
        private String summary;

        // Getters and Setters
        public boolean isMeetsAllTargets() { return meetsAllTargets; }
        public void setMeetsAllTargets(boolean meetsAllTargets) { this.meetsAllTargets = meetsAllTargets; }

        public double getQualityScoreGap() { return qualityScoreGap; }
        public void setQualityScoreGap(double qualityScoreGap) { this.qualityScoreGap = qualityScoreGap; }

        public double getBestTemplateGap() { return bestTemplateGap; }
        public void setBestTemplateGap(double bestTemplateGap) { this.bestTemplateGap = bestTemplateGap; }

        public double getCoverageGap() { return coverageGap; }
        public void setCoverageGap(double coverageGap) { this.coverageGap = coverageGap; }

        public double getMathematicalAccuracyGap() { return mathematicalAccuracyGap; }
        public void setMathematicalAccuracyGap(double mathematicalAccuracyGap) { this.mathematicalAccuracyGap = mathematicalAccuracyGap; }

        public String getSummary() { return summary; }
        public void setSummary(String summary) { this.summary = summary; }
    }

    public static class DistributionMetrics {
        private int bestGroupCount;
        private int goodGroupCount;
        private int fairGroupCount;
        private int poorGroupCount;
        private int recommendedCount;
        private int overdueCount;
        private double averageHitCount;
        private double averageGamesOut;
        private double coveragePercentage;

        // Getters and Setters
        public int getBestGroupCount() { return bestGroupCount; }
        public void setBestGroupCount(int bestGroupCount) { this.bestGroupCount = bestGroupCount; }

        public int getGoodGroupCount() { return goodGroupCount; }
        public void setGoodGroupCount(int goodGroupCount) { this.goodGroupCount = goodGroupCount; }

        public int getFairGroupCount() { return fairGroupCount; }
        public void setFairGroupCount(int fairGroupCount) { this.fairGroupCount = fairGroupCount; }

        public int getPoorGroupCount() { return poorGroupCount; }
        public void setPoorGroupCount(int poorGroupCount) { this.poorGroupCount = poorGroupCount; }

        public int getRecommendedCount() { return recommendedCount; }
        public void setRecommendedCount(int recommendedCount) { this.recommendedCount = recommendedCount; }

        public int getOverdueCount() { return overdueCount; }
        public void setOverdueCount(int overdueCount) { this.overdueCount = overdueCount; }

        public double getAverageHitCount() { return averageHitCount; }
        public void setAverageHitCount(double averageHitCount) { this.averageHitCount = averageHitCount; }

        public double getAverageGamesOut() { return averageGamesOut; }
        public void setAverageGamesOut(double averageGamesOut) { this.averageGamesOut = averageGamesOut; }

        public double getCoveragePercentage() { return coveragePercentage; }
        public void setCoveragePercentage(double coveragePercentage) { this.coveragePercentage = coveragePercentage; }
    }
}