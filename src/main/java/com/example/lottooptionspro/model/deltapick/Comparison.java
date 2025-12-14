package com.example.lottooptionspro.model.deltapick;

/**
 * Comparison of generated picks against random tickets and all possible combinations.
 */
public class Comparison {
    private ComparisonMetric vsRandomTickets;
    private ComparisonMetric vsAllPossibleCombinations;

    public Comparison() {
    }

    public ComparisonMetric getVsRandomTickets() {
        return vsRandomTickets;
    }

    public void setVsRandomTickets(ComparisonMetric vsRandomTickets) {
        this.vsRandomTickets = vsRandomTickets;
    }

    public ComparisonMetric getVsAllPossibleCombinations() {
        return vsAllPossibleCombinations;
    }

    public void setVsAllPossibleCombinations(ComparisonMetric vsAllPossibleCombinations) {
        this.vsAllPossibleCombinations = vsAllPossibleCombinations;
    }
}
