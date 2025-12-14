package com.example.lottooptionspro.model.deltapick;

import java.util.List;

/**
 * Represents a single generated lottery pick with its associated deltas and probability.
 */
public class GeneratedPick implements Comparable<GeneratedPick> {
    private List<Integer> numbers;
    private List<Integer> rawDeltas;
    private List<Integer> sortedDeltas;
    private Double probabilityScore;
    private Integer rank;

    public GeneratedPick() {
    }
    
    @Override
    public int compareTo(GeneratedPick other) {
        // Compare by rank (ascending order)
        if (this.rank == null && other.rank == null) return 0;
        if (this.rank == null) return 1;
        if (other.rank == null) return -1;
        return this.rank.compareTo(other.rank);
    }

    public List<Integer> getNumbers() {
        return numbers;
    }

    public void setNumbers(List<Integer> numbers) {
        this.numbers = numbers;
    }

    public List<Integer> getRawDeltas() {
        return rawDeltas;
    }

    public void setRawDeltas(List<Integer> rawDeltas) {
        this.rawDeltas = rawDeltas;
    }

    public List<Integer> getSortedDeltas() {
        return sortedDeltas;
    }

    public void setSortedDeltas(List<Integer> sortedDeltas) {
        this.sortedDeltas = sortedDeltas;
    }

    public Double getProbabilityScore() {
        return probabilityScore;
    }

    public void setProbabilityScore(Double probabilityScore) {
        this.probabilityScore = probabilityScore;
    }

    public Integer getRank() {
        return rank;
    }

    public void setRank(Integer rank) {
        this.rank = rank;
    }
}
