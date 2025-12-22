package com.example.lottooptionspro.model.wheel;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class WheelStatistics {
    private int totalCombinations;
    private double coveragePercentage;
    private int poolSize;
    private int pickSize;
    private String guarantee;
    private double efficiency;

    public static WheelStatistics calculate(int totalCombinations, int poolSize, int pickSize, GuaranteeLevel guarantee) {
        long fullWheelSize = calculateCombinations(poolSize, pickSize);
        double coverage = (double) totalCombinations / fullWheelSize * 100.0;
        double efficiency = fullWheelSize > 0 ? (double) fullWheelSize / totalCombinations : 0;
        
        return new WheelStatistics(
            totalCombinations,
            coverage,
            poolSize,
            pickSize,
            guarantee.getDisplayName(),
            efficiency
        );
    }

    private static long calculateCombinations(int n, int k) {
        if (k > n) return 0;
        if (k == 0 || k == n) return 1;
        
        long result = 1;
        for (int i = 0; i < k; i++) {
            result = result * (n - i) / (i + 1);
        }
        return result;
    }
}
