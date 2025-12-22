package com.example.lottooptionspro.model.strategyengine;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class HistoricalPerformance {
    private TrapStatistics trapStatistics;
    private PerformanceMetrics performanceMetrics;
    private Integer drawsAnalyzed;
}
