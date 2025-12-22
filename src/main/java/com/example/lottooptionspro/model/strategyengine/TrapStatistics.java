package com.example.lottooptionspro.model.strategyengine;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TrapStatistics {
    private MatchStatistic perfectMatch;
    private MatchStatistic matchMinus1;
    private MatchStatistic matchMinus2;
    private MatchStatistic matchMinus3;
}
