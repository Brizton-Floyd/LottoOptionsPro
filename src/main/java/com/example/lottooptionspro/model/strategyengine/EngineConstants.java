package com.example.lottooptionspro.model.strategyengine;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EngineConstants {
    private Double averageSkip;
    private Integer longShotThreshold;
    private Integer coldRuleLimit;
}
