package com.example.lottooptionspro.model.wheel;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class WheelGenerationResponse {
    private List<int[]> combinations;
    private int totalLines;
    private GuaranteeLevel guarantee;
    private WheelStatistics statistics;
}
