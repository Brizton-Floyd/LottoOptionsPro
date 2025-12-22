package com.example.lottooptionspro.model.strategyengine;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SetPerformance {
    private TrapStatistics trapStatistics;
    private Double averageCoverage;
    private Integer bestMatch;
    private Integer worstMatch;
}
