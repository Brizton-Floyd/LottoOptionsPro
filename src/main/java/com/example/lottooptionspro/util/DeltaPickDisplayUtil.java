package com.example.lottooptionspro.util;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Utility class for formatting delta pick data for display.
 */
public class DeltaPickDisplayUtil {
    
    /**
     * Formats a list of integers as a hyphen-separated string.
     * Example: [3, 6, 9, 16, 29] -> "3-6-9-16-29"
     *
     * @param numbers the list of numbers to format
     * @return hyphen-separated string representation
     */
    public static String formatNumbersArray(List<Integer> numbers) {
        if (numbers == null || numbers.isEmpty()) {
            return "";
        }
        
        return numbers.stream()
                .map(String::valueOf)
                .collect(Collectors.joining("-"));
    }
    
    /**
     * Formats a probability score as a percentage string.
     * Example: 0.002246999347118714 -> "0.22%"
     *
     * @param probabilityScore the probability score (0.0 to 1.0)
     * @return formatted percentage string
     */
    public static String formatProbabilityAsPercentage(Double probabilityScore) {
        if (probabilityScore == null) {
            return "0.00%";
        }
        
        double percentage = probabilityScore * 100;
        return String.format("%.2f%%", percentage);
    }
    
    /**
     * Formats a probability score with scientific notation for very small values.
     * Example: 0.002246999347118714 -> "2.25e-3"
     *
     * @param probabilityScore the probability score
     * @return formatted scientific notation string
     */
    public static String formatProbabilityScientific(Double probabilityScore) {
        if (probabilityScore == null) {
            return "0.00e0";
        }
        
        return String.format("%.2e", probabilityScore);
    }
    
    /**
     * Formats execution time in milliseconds to a human-readable string.
     * Example: 1500 -> "1.5s", 60 -> "60ms"
     *
     * @param executionTimeMs execution time in milliseconds
     * @return formatted time string
     */
    public static String formatExecutionTime(Integer executionTimeMs) {
        if (executionTimeMs == null || executionTimeMs == 0) {
            return "0ms";
        }
        
        if (executionTimeMs < 1000) {
            return executionTimeMs + "ms";
        }
        
        double seconds = executionTimeMs / 1000.0;
        return String.format("%.1fs", seconds);
    }
    
    /**
     * Formats a large number with comma separators.
     * Example: 20740 -> "20,740"
     *
     * @param number the number to format
     * @return formatted number string with commas
     */
    public static String formatNumberWithCommas(Integer number) {
        if (number == null) {
            return "0";
        }
        
        return String.format("%,d", number);
    }
}
