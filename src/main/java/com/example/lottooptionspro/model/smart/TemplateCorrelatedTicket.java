package com.example.lottooptionspro.model.smart;

import java.util.List;

public class TemplateCorrelatedTicket {
    private List<Integer> numbers;
    private String templatePattern;
    private String templateGroup;
    private double templateProbability;
    private int templateHitCount;
    private int templateGamesOut;
    private double templatePerformanceScore;
    private String numbersAsString;
    private boolean templateOverdue;

    // Default constructor
    public TemplateCorrelatedTicket() {
    }

    // Constructor with essential fields
    public TemplateCorrelatedTicket(List<Integer> numbers, String templatePattern, String templateGroup) {
        this.numbers = numbers;
        this.templatePattern = templatePattern;
        this.templateGroup = templateGroup;
        this.numbersAsString = formatNumbers(numbers);
    }

    // Getters and Setters
    public List<Integer> getNumbers() {
        return numbers;
    }

    public void setNumbers(List<Integer> numbers) {
        this.numbers = numbers;
        this.numbersAsString = formatNumbers(numbers);
    }

    public String getTemplatePattern() {
        return templatePattern;
    }

    public void setTemplatePattern(String templatePattern) {
        this.templatePattern = templatePattern;
    }

    public String getTemplateGroup() {
        return templateGroup;
    }

    public void setTemplateGroup(String templateGroup) {
        this.templateGroup = templateGroup;
    }

    public double getTemplateProbability() {
        return templateProbability;
    }

    public void setTemplateProbability(double templateProbability) {
        this.templateProbability = templateProbability;
    }

    public int getTemplateHitCount() {
        return templateHitCount;
    }

    public void setTemplateHitCount(int templateHitCount) {
        this.templateHitCount = templateHitCount;
    }

    public int getTemplateGamesOut() {
        return templateGamesOut;
    }

    public void setTemplateGamesOut(int templateGamesOut) {
        this.templateGamesOut = templateGamesOut;
    }

    public double getTemplatePerformanceScore() {
        return templatePerformanceScore;
    }

    public void setTemplatePerformanceScore(double templatePerformanceScore) {
        this.templatePerformanceScore = templatePerformanceScore;
    }

    public String getNumbersAsString() {
        return numbersAsString;
    }

    public void setNumbersAsString(String numbersAsString) {
        this.numbersAsString = numbersAsString;
    }

    public boolean isTemplateOverdue() {
        return templateOverdue;
    }

    public void setTemplateOverdue(boolean templateOverdue) {
        this.templateOverdue = templateOverdue;
    }

    // Utility methods
    private String formatNumbers(List<Integer> numbers) {
        if (numbers == null || numbers.isEmpty()) {
            return "";
        }
        return numbers.toString();
    }

    public String getFormattedNumbers() {
        if (numbers == null || numbers.isEmpty()) {
            return "";
        }
        return numbers.stream()
                .map(String::valueOf)
                .reduce((a, b) -> a + ", " + b)
                .orElse("");
    }

    public String getFormattedProbability() {
        return String.format("%.2f%%", templateProbability * 100);
    }

    public String getFormattedPerformanceScore() {
        return String.format("%.1f", templatePerformanceScore);
    }

    public boolean isBestOrGoodTemplate() {
        return "BEST".equals(templateGroup) || "GOOD".equals(templateGroup);
    }

    public boolean isFairOrPoorTemplate() {
        return "FAIR".equals(templateGroup) || "POOR".equals(templateGroup);
    }

    public String getQualityIndicator() {
        if ("BEST".equals(templateGroup)) {
            return "⭐";
        } else if ("GOOD".equals(templateGroup)) {
            return "✓";
        } else if ("FAIR".equals(templateGroup)) {
            return "○";
        } else if ("POOR".equals(templateGroup)) {
            return "△";
        }
        return "";
    }

    public String getOverdueIndicator() {
        return templateOverdue ? "🔴" : "";
    }

    @Override
    public String toString() {
        return "TemplateCorrelatedTicket{" +
                "numbers=" + numbers +
                ", templatePattern='" + templatePattern + '\'' +
                ", templateGroup='" + templateGroup + '\'' +
                ", templateProbability=" + templateProbability +
                ", templateOverdue=" + templateOverdue +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        TemplateCorrelatedTicket that = (TemplateCorrelatedTicket) o;

        if (numbers != null ? !numbers.equals(that.numbers) : that.numbers != null) return false;
        return templatePattern != null ? templatePattern.equals(that.templatePattern) : that.templatePattern == null;
    }

    @Override
    public int hashCode() {
        int result = numbers != null ? numbers.hashCode() : 0;
        result = 31 * result + (templatePattern != null ? templatePattern.hashCode() : 0);
        return result;
    }
}