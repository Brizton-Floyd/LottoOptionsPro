package com.example.lottooptionspro.model.range;

import java.util.Arrays;
import java.util.List;

public class GameConfiguration {
    private String state;
    private String game;
    private int minNumber;
    private int maxNumber;
    private int numbersDrawn;
    private int bonusNumbers;
    private List<Integer> rangeSizes;
    private int defaultRangeSize;
    private int recommendedMaxDraws;

    public GameConfiguration(String state, String game) {
        this.state = state.toUpperCase();
        this.game = game;
        configureForGame(game);
    }

    private void configureForGame(String gameName) {
        String normalizedGame = gameName.toLowerCase().replaceAll("[\\s-]", "");
        
        switch (normalizedGame) {
            case "powerball":
                setGameConfig(1, 69, 5, 1, Arrays.asList(5, 10, 15), 10, 100);
                break;
            case "megamillions":
                setGameConfig(1, 70, 5, 1, Arrays.asList(5, 10, 15), 10, 100);
                break;
            case "lotto":
            case "lottoamerica":
                setGameConfig(1, 52, 5, 1, Arrays.asList(5, 10, 15), 10, 80);
                break;
            case "lottotexas":
                setGameConfig(1, 54, 6, 0, Arrays.asList(5, 10, 15), 10, 80);
                break;
            case "cash4life":
                setGameConfig(1, 60, 5, 1, Arrays.asList(5, 10, 12), 10, 75);
                break;
            case "cashfive":
            case "cash5":
            case "take5":
                setGameConfig(1, 39, 5, 0, Arrays.asList(5, 10), 5, 60);
                break;
            case "pick6":
                setGameConfig(1, 49, 6, 0, Arrays.asList(5, 10), 10, 60);
                break;
            case "pick5":
                setGameConfig(1, 39, 5, 0, Arrays.asList(5, 10), 5, 50);
                break;
            case "pick4":
                setGameConfig(0, 9, 4, 0, Arrays.asList(2, 5), 2, 40);
                break;
            case "pick3":
                setGameConfig(0, 9, 3, 0, Arrays.asList(2, 5), 2, 30);
                break;
            case "daily3":
                setGameConfig(0, 9, 3, 0, Arrays.asList(2, 5), 2, 30);
                break;
            case "daily4":
                setGameConfig(0, 9, 4, 0, Arrays.asList(2, 5), 2, 40);
                break;
            case "fantasy5":
                setGameConfig(1, 39, 5, 0, Arrays.asList(5, 10), 5, 60);
                break;
            case "lucky4life":
                setGameConfig(1, 48, 5, 1, Arrays.asList(5, 10, 12), 10, 75);
                break;
            default:
                // Generic lottery configuration
                setGameConfig(1, 49, 5, 0, Arrays.asList(5, 10, 15, 20), 10, 50);
                break;
        }
    }

    private void setGameConfig(int minNumber, int maxNumber, int numbersDrawn, 
                              int bonusNumbers, List<Integer> rangeSizes, 
                              int defaultRangeSize, int recommendedMaxDraws) {
        this.minNumber = minNumber;
        this.maxNumber = maxNumber;
        this.numbersDrawn = numbersDrawn;
        this.bonusNumbers = bonusNumbers;
        this.rangeSizes = rangeSizes;
        this.defaultRangeSize = defaultRangeSize;
        this.recommendedMaxDraws = recommendedMaxDraws;
    }

    public List<Integer> getDrawPositions() {
        List<Integer> positions = new java.util.ArrayList<>();
        for (int i = 1; i <= numbersDrawn; i++) {
            positions.add(i);
        }
        return positions;
    }

    public int calculateOptimalRangeSize() {
        int numberRange = maxNumber - minNumber + 1;
        
        // For small number ranges (like Pick3/4), use smaller range sizes
        if (numberRange <= 10) {
            return 2;
        } else if (numberRange <= 39) {
            return 5;
        } else if (numberRange <= 60) {
            return 10;
        } else {
            return 15;
        }
    }

    public int getOptimalMaxDraws() {
        // Base recommendation adjusted for game complexity
        int baseDraws = recommendedMaxDraws;
        
        // Adjust based on number of positions drawn
        if (numbersDrawn <= 3) {
            baseDraws = Math.min(baseDraws, 30);
        } else if (numbersDrawn >= 6) {
            baseDraws = Math.max(baseDraws, 75);
        }
        
        return baseDraws;
    }

    public List<Integer> getSupportedRangeSizes() {
        return rangeSizes;
    }
    
    public List<Integer> getValidRangeSizes() {
        int totalNumbers = maxNumber - minNumber + 1;
        return rangeSizes.stream()
                .filter(size -> size <= totalNumbers)
                .filter(size -> totalNumbers >= size * 2) // Ensure at least 2 ranges
                .collect(java.util.stream.Collectors.toList());
    }
    
    public List<String> generateValidRangeHeaders(int rangeSize) {
        List<String> ranges = new java.util.ArrayList<>();
        int currentStart = minNumber;
        
        while (currentStart <= maxNumber) {
            int currentEnd = Math.min(currentStart + rangeSize - 1, maxNumber);
            ranges.add(currentStart + "-" + currentEnd);
            currentStart += rangeSize;
            
            // Safety check to prevent infinite loops
            if (currentStart > maxNumber || ranges.size() > 50) {
                break;
            }
        }
        
        return ranges;
    }
    
    public boolean isValidRangeSize(int rangeSize) {
        return getValidRangeSizes().contains(rangeSize);
    }
    
    public String getRangePreviewText(int rangeSize) {
        if (!isValidRangeSize(rangeSize)) {
            return "Invalid range size for " + game + " (numbers " + minNumber + "-" + maxNumber + ")";
        }
        
        List<String> ranges = generateValidRangeHeaders(rangeSize);
        if (ranges.size() <= 6) {
            return "Ranges: " + String.join(", ", ranges);
        } else {
            return "Ranges: " + String.join(", ", ranges.subList(0, 3)) + 
                   "... (" + ranges.size() + " total ranges)";
        }
    }

    // Getters and setters
    public String getState() { return state; }
    public void setState(String state) { this.state = state; }
    
    public String getGame() { return game; }
    public void setGame(String game) { this.game = game; }
    
    public int getMinNumber() { return minNumber; }
    public void setMinNumber(int minNumber) { this.minNumber = minNumber; }
    
    public int getMaxNumber() { return maxNumber; }
    public void setMaxNumber(int maxNumber) { this.maxNumber = maxNumber; }
    
    public int getNumbersDrawn() { return numbersDrawn; }
    public void setNumbersDrawn(int numbersDrawn) { this.numbersDrawn = numbersDrawn; }
    
    public int getBonusNumbers() { return bonusNumbers; }
    public void setBonusNumbers(int bonusNumbers) { this.bonusNumbers = bonusNumbers; }
    
    public int getDefaultRangeSize() { return defaultRangeSize; }
    public void setDefaultRangeSize(int defaultRangeSize) { this.defaultRangeSize = defaultRangeSize; }
    
    public int getRecommendedMaxDraws() { return recommendedMaxDraws; }
    public void setRecommendedMaxDraws(int recommendedMaxDraws) { this.recommendedMaxDraws = recommendedMaxDraws; }
    
    public String getNumberRangeString() {
        return minNumber + "-" + maxNumber;
    }
    
    public boolean hasBonus() {
        return bonusNumbers > 0;
    }
    
    // Enhanced Intelligent Range Generation Methods
    
    public List<Integer> getFibonacciRangeSizes() {
        List<Integer> fibSizes = new java.util.ArrayList<>();
        int a = 1, b = 1;
        int numberRange = maxNumber - minNumber + 1;
        
        while (a <= numberRange / 3) { // Don't go beyond 1/3 of total range
            if (a >= 2 && numberRange >= a * 2) { // Ensure at least 2 ranges
                fibSizes.add(a);
            }
            int temp = a + b;
            a = b;
            b = temp;
        }
        
        return fibSizes.isEmpty() ? Arrays.asList(2, 3, 5) : fibSizes;
    }
    
    public List<Integer> getPercentageBasedRangeSizes() {
        int numberRange = maxNumber - minNumber + 1;
        List<Integer> percentageSizes = new java.util.ArrayList<>();
        
        // Common percentage breakdowns: 5%, 10%, 15%, 20%, 25%
        int[] percentages = {5, 10, 15, 20, 25};
        
        for (int pct : percentages) {
            int size = (int) Math.ceil(numberRange * pct / 100.0);
            if (size >= 2 && size <= numberRange / 2) {
                percentageSizes.add(size);
            }
        }
        
        return percentageSizes.isEmpty() ? Arrays.asList(5, 10) : percentageSizes;
    }
    
    public List<Integer> getDeltaOptimizedRangeSizes() {
        // For DELTA analysis, smaller ranges work better as deltas are typically smaller
        int numberRange = maxNumber - minNumber + 1;
        List<Integer> deltaOptimized = new java.util.ArrayList<>();
        
        if (numberRange <= 10) {
            deltaOptimized.addAll(Arrays.asList(2, 3));
        } else if (numberRange <= 39) {
            deltaOptimized.addAll(Arrays.asList(3, 5, 7));
        } else if (numberRange <= 60) {
            deltaOptimized.addAll(Arrays.asList(5, 7, 10));
        } else {
            deltaOptimized.addAll(Arrays.asList(7, 10, 12));
        }
        
        return deltaOptimized;
    }
    
    public RangeRecommendation getContextualRangeRecommendation(AnalysisType analysisType, int sampleSize) {
        List<Integer> recommendedSizes;
        String rationale;
        double confidence;
        
        switch (analysisType) {
            case DELTA:
            case DELTA_SORTED:
                recommendedSizes = getDeltaOptimizedRangeSizes();
                rationale = "DELTA analysis benefits from smaller ranges to capture gap patterns effectively";
                confidence = calculateConfidenceForDelta(sampleSize);
                break;
            case ACTUAL:
            default:
                recommendedSizes = getAdaptiveRangeSizes(sampleSize);
                rationale = "Balanced approach optimized for your game's number distribution";
                confidence = calculateConfidenceForActual(sampleSize);
                break;
        }
        
        return new RangeRecommendation(recommendedSizes, rationale, confidence);
    }
    
    private List<Integer> getAdaptiveRangeSizes(int sampleSize) {
        List<Integer> adaptive = new java.util.ArrayList<>();
        
        // More granular ranges for larger sample sizes
        if (sampleSize >= recommendedMaxDraws) {
            adaptive.addAll(getSupportedRangeSizes());
            // Add intermediate sizes for fine-grained analysis
            for (int size : getSupportedRangeSizes()) {
                if (size > 5) {
                    int midSize = size / 2;
                    if (!adaptive.contains(midSize) && isValidRangeSize(midSize)) {
                        adaptive.add(midSize);
                    }
                }
            }
        } else {
            // Conservative approach for smaller samples
            adaptive.addAll(getSupportedRangeSizes().subList(0, 
                Math.min(2, getSupportedRangeSizes().size())));
        }
        
        return adaptive.stream().sorted().collect(java.util.stream.Collectors.toList());
    }
    
    private double calculateConfidenceForDelta(int sampleSize) {
        // DELTA analysis requires more data for stability
        double requiredSample = recommendedMaxDraws * 1.5;
        return Math.min(0.95, Math.log(1 + sampleSize / requiredSample) / Math.log(2));
    }
    
    private double calculateConfidenceForActual(int sampleSize) {
        // Standard confidence calculation
        double requiredSample = recommendedMaxDraws;
        return Math.min(0.95, Math.log(1 + sampleSize / requiredSample) / Math.log(1.5));
    }
    
    public List<String> generateDeltaAwareRangeHeaders(int rangeSize, AnalysisType analysisType) {
        if (analysisType == AnalysisType.DELTA || analysisType == AnalysisType.DELTA_SORTED) {
            // For DELTA analysis, ranges represent gap sizes
            return generateDeltaRangeHeaders(rangeSize);
        } else {
            // Standard number ranges
            return generateValidRangeHeaders(rangeSize);
        }
    }
    
    private List<String> generateDeltaRangeHeaders(int rangeSize) {
        List<String> deltaRanges = new java.util.ArrayList<>();
        
        // DELTA ranges are typically 0-based and represent gaps between numbers
        int maxDelta = maxNumber - minNumber; // Maximum possible delta
        int currentStart = 0;
        
        while (currentStart <= maxDelta) {
            int currentEnd = Math.min(currentStart + rangeSize - 1, maxDelta);
            deltaRanges.add("Δ" + currentStart + "-" + currentEnd);
            currentStart += rangeSize;
            
            if (currentStart > maxDelta || deltaRanges.size() > 30) {
                break;
            }
        }
        
        return deltaRanges;
    }
    
    public static class RangeRecommendation {
        private final List<Integer> recommendedSizes;
        private final String rationale;
        private final double confidence;
        
        public RangeRecommendation(List<Integer> recommendedSizes, String rationale, double confidence) {
            this.recommendedSizes = recommendedSizes;
            this.rationale = rationale;
            this.confidence = confidence;
        }
        
        public List<Integer> getRecommendedSizes() { return recommendedSizes; }
        public String getRationale() { return rationale; }
        public double getConfidence() { return confidence; }
        
        public int getPrimaryRecommendation() {
            return recommendedSizes.isEmpty() ? 5 : recommendedSizes.get(0);
        }
        
        public String getConfidenceLevel() {
            if (confidence >= 0.8) return "HIGH";
            if (confidence >= 0.6) return "MEDIUM";
            return "LOW";
        }
    }
    
    public enum AnalysisType {
        ACTUAL, DELTA, DELTA_SORTED
    }
}