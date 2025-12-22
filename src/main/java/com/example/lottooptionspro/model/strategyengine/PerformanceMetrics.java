package com.example.lottooptionspro.model.strategyengine;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PerformanceMetrics {
    private Double hitRate;
    private Double averageCoverage;
    private Integer bestPerformance;
    private Double consistencyScore;
}
