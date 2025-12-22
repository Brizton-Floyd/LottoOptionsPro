package com.example.lottooptionspro.model.strategyengine;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Metadata {
    private String gameId;
    private String lotteryState;
    private Integer fieldSize;
    private Integer drawCount;
    private Integer historicalDrawsUsed;
    private Integer totalCandidatesEvaluated;
    private Integer candidatesAfterFiltering;
    private String strategyBias;
    private Integer executionTimeMs;
}
