package com.example.lottooptionspro.model.strategyengine;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MatchStatistic {
    private String matchDescription;
    private Integer count;
    private Integer drawsSinceLastOccurrence;
    private String lastOccurrenceDate;
    private Double percentage;
}
