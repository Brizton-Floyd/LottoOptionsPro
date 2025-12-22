package com.example.lottooptionspro.model.strategyengine;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TierBreakdown {
    private List<Integer> tier1Anchors;
    private List<Integer> tier2Rotators;
    private List<Integer> tier3Rotators;
}
