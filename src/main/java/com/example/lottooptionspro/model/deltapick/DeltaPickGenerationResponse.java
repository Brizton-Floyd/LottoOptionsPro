package com.example.lottooptionspro.model.deltapick;

import java.util.List;

/**
 * Complete response from the delta pick generation API.
 */
public class DeltaPickGenerationResponse {
    private Configuration configuration;
    private List<GeneratedPick> generatedPicks;
    private Integer totalValidCombinations;
    private Integer executionTimeMs;
    private Metadata metadata;
    private HistoricalPerformance historicalPerformance;

    public DeltaPickGenerationResponse() {
    }

    public Configuration getConfiguration() {
        return configuration;
    }

    public void setConfiguration(Configuration configuration) {
        this.configuration = configuration;
    }

    public List<GeneratedPick> getGeneratedPicks() {
        return generatedPicks;
    }

    public void setGeneratedPicks(List<GeneratedPick> generatedPicks) {
        this.generatedPicks = generatedPicks;
    }

    public Integer getTotalValidCombinations() {
        return totalValidCombinations;
    }

    public void setTotalValidCombinations(Integer totalValidCombinations) {
        this.totalValidCombinations = totalValidCombinations;
    }

    public Integer getExecutionTimeMs() {
        return executionTimeMs;
    }

    public void setExecutionTimeMs(Integer executionTimeMs) {
        this.executionTimeMs = executionTimeMs;
    }

    public Metadata getMetadata() {
        return metadata;
    }

    public void setMetadata(Metadata metadata) {
        this.metadata = metadata;
    }

    public HistoricalPerformance getHistoricalPerformance() {
        return historicalPerformance;
    }

    public void setHistoricalPerformance(HistoricalPerformance historicalPerformance) {
        this.historicalPerformance = historicalPerformance;
    }
}
