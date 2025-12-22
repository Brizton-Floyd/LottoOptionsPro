package com.example.lottooptionspro.model.strategyengine;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class StrategyEngineResponse {
    private EngineConstants engineConstants;
    private Tier1Anchors tier1Anchors;
    private List<GeneratedSet> generatedSets;
    private List<ExclusionReport> exclusionReport;
    private HistoricalPerformance historicalPerformance;
    private Metadata metadata;
}
