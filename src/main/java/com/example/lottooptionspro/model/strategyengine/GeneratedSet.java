package com.example.lottooptionspro.model.strategyengine;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class GeneratedSet {
    private Integer setId;
    private List<Integer> numbers;
    private Double diversityScore;
    private TierBreakdown tierBreakdown;
    private SetPerformance setPerformance;
}
