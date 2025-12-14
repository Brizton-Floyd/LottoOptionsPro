package com.example.lottooptionspro.model.deltapick;

/**
 * Metadata about the delta pick generation process.
 */
public class Metadata {
    private Integer searchSpaceExplored;
    private Integer prunedBranches;
    private Boolean usedMappingMatrix;
    private Integer historicalDrawsUsed;
    private String generationStrategy;

    public Metadata() {
    }

    public Integer getSearchSpaceExplored() {
        return searchSpaceExplored;
    }

    public void setSearchSpaceExplored(Integer searchSpaceExplored) {
        this.searchSpaceExplored = searchSpaceExplored;
    }

    public Integer getPrunedBranches() {
        return prunedBranches;
    }

    public void setPrunedBranches(Integer prunedBranches) {
        this.prunedBranches = prunedBranches;
    }

    public Boolean getUsedMappingMatrix() {
        return usedMappingMatrix;
    }

    public void setUsedMappingMatrix(Boolean usedMappingMatrix) {
        this.usedMappingMatrix = usedMappingMatrix;
    }

    public Integer getHistoricalDrawsUsed() {
        return historicalDrawsUsed;
    }

    public void setHistoricalDrawsUsed(Integer historicalDrawsUsed) {
        this.historicalDrawsUsed = historicalDrawsUsed;
    }

    public String getGenerationStrategy() {
        return generationStrategy;
    }

    public void setGenerationStrategy(String generationStrategy) {
        this.generationStrategy = generationStrategy;
    }
}
