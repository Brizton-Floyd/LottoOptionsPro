package com.example.lottooptionspro.util;

import com.example.lottooptionspro.model.dashboard.NumberSegment;
import com.example.lottooptionspro.model.dashboard.SegmentAnalysisResult;
import com.example.lottooptionspro.model.dashboard.SegmentGameOutHistory;
import com.floyd.model.dashboard.DrawResultPattern;
import com.floyd.model.dashboard.LotteryNumber;

import java.util.*;
import java.util.stream.Collectors;

public class SegmentCalculator {
    private static final int DEFAULT_SEGMENT_SIZE = 10;
    
    public static List<NumberSegment> createSegments(int minNumber, int maxNumber, int segmentSize) {
        List<NumberSegment> segments = new ArrayList<>();
        
        // Create segments based on first digit groupings
        // 1-9, 10-19, 20-29, 30-39, 40-49, 50-59, etc.
        
        int currentStart = 1;
        
        while (currentStart <= maxNumber) {
            int segmentEnd;
            
            if (currentStart < 10) {
                // First segment: 1-9
                segmentEnd = 9;
            } else {
                // Other segments based on tens: 10-19, 20-29, etc.
                int tens = (currentStart / 10) * 10;
                segmentEnd = tens + 9;
            }
            
            // Adjust to actual range bounds
            int actualStart = Math.max(currentStart, minNumber);
            int actualEnd = Math.min(segmentEnd, maxNumber);
            
            // Only add if the segment overlaps with our range
            if (actualStart <= actualEnd && actualEnd >= minNumber && actualStart <= maxNumber) {
                segments.add(new NumberSegment(actualStart, actualEnd));
            }
            
            // Move to next segment
            if (currentStart < 10) {
                currentStart = 10;
            } else {
                currentStart += 10;
            }
        }
        
        return segments;
    }
    
    public static List<NumberSegment> createSegments(int minNumber, int maxNumber) {
        return createSegments(minNumber, maxNumber, DEFAULT_SEGMENT_SIZE);
    }
    
    public static Map<String, SegmentGameOutHistory> buildSegmentHistories(
            List<NumberSegment> segments, 
            List<DrawResultPattern> drawResultPatterns) {
        
        Map<String, SegmentGameOutHistory> histories = new HashMap<>();
        
        for (NumberSegment segment : segments) {
            SegmentGameOutHistory history = new SegmentGameOutHistory(
                    segment.getStartNumber(), segment.getEndNumber());
            
            for (DrawResultPattern pattern : drawResultPatterns) {
                String drawDate = pattern.getDrawDate();
                
                for (LotteryNumber lotteryNumber : pattern.getLotteryNumbers()) {
                    int number = lotteryNumber.getNumber();
                    int gamesOut = lotteryNumber.getGamesOut();
                    boolean wasHit = gamesOut == 0;
                    
                    if (segment.containsNumber(number)) {
                        history.addGameOutEntry(number, drawDate, gamesOut, wasHit);
                    }
                }
            }
            
            histories.put(segment.getSegmentName(), history);
        }
        
        return histories;
    }
    
    public static void populateSegmentData(List<NumberSegment> segments, 
                                         Map<String, SegmentGameOutHistory> histories) {
        
        for (NumberSegment segment : segments) {
            SegmentGameOutHistory history = histories.get(segment.getSegmentName());
            
            if (history != null) {
                Map<Integer, Integer> currentGamesOut = history.getCurrentGamesOutMap();
                segment.setCurrentGamesOut(currentGamesOut);
                
                double averageGamesOut = calculateAverageGamesOut(currentGamesOut);
                segment.setAverageGamesOut(averageGamesOut);
                
                int totalHits = calculateTotalHits(history);
                segment.setTotalHits(totalHits);
                
                String lastHitDate = findLastHitDate(history);
                segment.setLastHitDate(lastHitDate);
                
                List<Integer> recommendations = generateRecommendations(history, currentGamesOut);
                segment.setRecommendedGamesOut(recommendations);
                
                // Calculate optimal number selections for hot and normal groups
                System.out.println("Calculating optimal numbers for segment: " + segment.getSegmentName());
                List<Integer> optimalHotNumbers = calculateOptimalHotNumbers(segment, history);
                segment.setOptimalHotNumbers(optimalHotNumbers);
                System.out.println("Set " + (optimalHotNumbers != null ? optimalHotNumbers.size() : 0) + " optimal hot numbers for " + segment.getSegmentName());
                
                List<Integer> optimalNormalNumbers = calculateOptimalNormalNumbers(segment, history);
                segment.setOptimalNormalNumbers(optimalNormalNumbers);
                System.out.println("Set " + (optimalNormalNumbers != null ? optimalNormalNumbers.size() : 0) + " optimal normal numbers for " + segment.getSegmentName());
            }
        }
    }
    
    public static SegmentAnalysisResult.SegmentPerformanceMetrics calculateSegmentMetrics(
            NumberSegment segment, SegmentGameOutHistory history) {
        
        SegmentAnalysisResult.SegmentPerformanceMetrics metrics = 
                new SegmentAnalysisResult.SegmentPerformanceMetrics();
        
        metrics.setSegmentName(segment.getSegmentName());
        metrics.setAverageGamesOut(segment.getAverageGamesOut());
        metrics.setTotalHits(segment.getTotalHits());
        
        Map<Integer, Integer> currentGamesOut = segment.getCurrentGamesOut();
        if (currentGamesOut != null && !currentGamesOut.isEmpty()) {
            OptionalInt coldest = currentGamesOut.values().stream().mapToInt(Integer::intValue).max();
            OptionalInt hottest = currentGamesOut.values().stream().mapToInt(Integer::intValue).min();
            
            if (coldest.isPresent()) {
                int coldestNumber = currentGamesOut.entrySet().stream()
                        .filter(e -> e.getValue().equals(coldest.getAsInt()))
                        .mapToInt(Map.Entry::getKey)
                        .findFirst()
                        .orElse(0);
                metrics.setColdestNumber(coldestNumber);
            }
            
            if (hottest.isPresent()) {
                int hottestNumber = currentGamesOut.entrySet().stream()
                        .filter(e -> e.getValue().equals(hottest.getAsInt()))
                        .mapToInt(Map.Entry::getKey)
                        .findFirst()
                        .orElse(0);
                metrics.setHottestNumber(hottestNumber);
            }
        }
        
        double hitFrequency = calculateHitFrequency(history);
        metrics.setHitFrequency(hitFrequency);
        
        double efficiency = calculateSegmentEfficiency(segment, history);
        metrics.setSegmentEfficiency(efficiency);
        
        List<Integer> trendingValues = identifyTrendingGamesOutValues(history);
        metrics.setTrendingGamesOutValues(trendingValues);
        
        return metrics;
    }
    
    private static double calculateAverageGamesOut(Map<Integer, Integer> currentGamesOut) {
        if (currentGamesOut == null || currentGamesOut.isEmpty()) {
            return 0.0;
        }
        
        return currentGamesOut.values().stream()
                .mapToInt(Integer::intValue)
                .average()
                .orElse(0.0);
    }
    
    private static int calculateTotalHits(SegmentGameOutHistory history) {
        return history.getNumberHistories().values().stream()
                .mapToInt(entries -> (int) entries.stream().filter(SegmentGameOutHistory.GameOutEntry::isWasHit).count())
                .sum();
    }
    
    private static String findLastHitDate(SegmentGameOutHistory history) {
        return history.getNumberHistories().values().stream()
                .flatMap(Collection::stream)
                .filter(SegmentGameOutHistory.GameOutEntry::isWasHit)
                .map(SegmentGameOutHistory.GameOutEntry::getDrawDate)
                .max(String::compareTo)
                .orElse("No hits found");
    }
    
    private static double calculateHitFrequency(SegmentGameOutHistory history) {
        Map<Integer, List<SegmentGameOutHistory.GameOutEntry>> numberHistories = history.getNumberHistories();
        if (numberHistories.isEmpty()) return 0.0;
        
        int totalEntries = numberHistories.values().stream().mapToInt(List::size).sum();
        int totalHits = numberHistories.values().stream()
                .mapToInt(entries -> (int) entries.stream().filter(SegmentGameOutHistory.GameOutEntry::isWasHit).count())
                .sum();
        
        return totalEntries > 0 ? (double) totalHits / totalEntries : 0.0;
    }
    
    private static double calculateSegmentEfficiency(NumberSegment segment, SegmentGameOutHistory history) {
        int segmentSize = segment.getSegmentSize();
        int totalHits = segment.getTotalHits();
        double averageGamesOut = segment.getAverageGamesOut();
        
        if (averageGamesOut == 0) return 0.0;
        
        return (totalHits * segmentSize) / averageGamesOut;
    }
    
    private static List<Integer> identifyTrendingGamesOutValues(SegmentGameOutHistory history) {
        Map<Integer, Integer> gamesOutFrequency = new HashMap<>();
        
        history.getNumberHistories().values().stream()
                .flatMap(Collection::stream)
                .filter(entry -> !entry.isWasHit())
                .forEach(entry -> gamesOutFrequency.merge(entry.getGamesOutValue(), 1, Integer::sum));
        
        return gamesOutFrequency.entrySet().stream()
                .sorted(Map.Entry.<Integer, Integer>comparingByValue().reversed())
                .limit(5)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
    }
    
    private static List<Integer> generateRecommendations(SegmentGameOutHistory history, 
                                                       Map<Integer, Integer> currentGamesOut) {
        List<Integer> recommendations = new ArrayList<>();
        
        // Enhanced analysis with multiple factors
        Map<Integer, RecommendationScore> gamesOutScoring = new HashMap<>();
        
        // Factor 1: Historical hit frequency analysis
        Map<Integer, Integer> hitFrequency = new HashMap<>();
        for (Map.Entry<Integer, List<SegmentGameOutHistory.GameOutEntry>> entry : history.getNumberHistories().entrySet()) {
            List<SegmentGameOutHistory.GameOutEntry> numberHistory = entry.getValue();
            
            for (int i = 1; i < numberHistory.size(); i++) {
                SegmentGameOutHistory.GameOutEntry currentEntry = numberHistory.get(i);
                SegmentGameOutHistory.GameOutEntry previousEntry = numberHistory.get(i - 1);
                
                if (currentEntry.isWasHit()) {
                    int previousGamesOut = previousEntry.getGamesOutValue();
                    hitFrequency.merge(previousGamesOut, 1, Integer::sum);
                }
            }
        }
        
        // Factor 2: Recent trends (weight recent data more heavily)
        Map<Integer, Double> recentTrends = new HashMap<>();
        for (Map.Entry<Integer, List<SegmentGameOutHistory.GameOutEntry>> entry : history.getNumberHistories().entrySet()) {
            List<SegmentGameOutHistory.GameOutEntry> numberHistory = entry.getValue();
            int totalEntries = numberHistory.size();
            
            for (int i = Math.max(0, totalEntries - 20); i < totalEntries - 1; i++) {  // Last 20 draws
                SegmentGameOutHistory.GameOutEntry currentEntry = numberHistory.get(i + 1);
                SegmentGameOutHistory.GameOutEntry previousEntry = numberHistory.get(i);
                
                if (currentEntry.isWasHit()) {
                    int previousGamesOut = previousEntry.getGamesOutValue();
                    double weight = (double)(i - Math.max(0, totalEntries - 20)) / 20.0; // Recent weight
                    recentTrends.merge(previousGamesOut, weight, Double::sum);
                }
            }
        }
        
        // Factor 3: Current segment state analysis
        Map<Integer, Double> currentStateScore = new HashMap<>();
        if (currentGamesOut != null && !currentGamesOut.isEmpty()) {
            double avgGamesOut = currentGamesOut.values().stream().mapToInt(Integer::intValue).average().orElse(0.0);
            
            for (Integer gamesOut : currentGamesOut.values()) {
                if (gamesOut > 0) {
                    // Score based on how close to average and distribution
                    double proximityToAverage = Math.max(0, 1.0 - Math.abs(gamesOut - avgGamesOut) / avgGamesOut);
                    currentStateScore.merge(gamesOut, proximityToAverage, Double::sum);
                }
            }
        }
        
        // Combine all factors into composite scores
        Set<Integer> allGamesOutValues = new HashSet<>();
        allGamesOutValues.addAll(hitFrequency.keySet());
        allGamesOutValues.addAll(recentTrends.keySet());
        allGamesOutValues.addAll(currentStateScore.keySet());
        
        for (Integer gamesOut : allGamesOutValues) {
            if (gamesOut > 0 && gamesOut <= 50) { // Reasonable range
                double hitScore = hitFrequency.getOrDefault(gamesOut, 0) * 0.4;
                double trendScore = recentTrends.getOrDefault(gamesOut, 0.0) * 0.4;
                double stateScore = currentStateScore.getOrDefault(gamesOut, 0.0) * 0.2;
                
                double totalScore = hitScore + trendScore + stateScore;
                gamesOutScoring.put(gamesOut, new RecommendationScore(totalScore, hitFrequency.getOrDefault(gamesOut, 0)));
            }
        }
        
        return gamesOutScoring.entrySet().stream()
                .filter(entry -> entry.getValue().totalScore > 0.1) // Minimum threshold
                .sorted((a, b) -> Double.compare(b.getValue().totalScore, a.getValue().totalScore))
                .limit(5)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
    }
    
    private static class RecommendationScore {
        final double totalScore;
        final int hitCount;
        
        RecommendationScore(double totalScore, int hitCount) {
            this.totalScore = totalScore;
            this.hitCount = hitCount;
        }
    }
    
    /**
     * Calculates optimal hot numbers (games out 1-5) to select in this segment
     * based on historical hit patterns and current games out values
     */
    public static List<Integer> calculateOptimalHotNumbers(NumberSegment segment, SegmentGameOutHistory history) {
        List<Integer> optimalHotNumbers = new ArrayList<>();
        Map<Integer, Integer> currentGamesOut = segment.getCurrentGamesOut();
        
        if (currentGamesOut == null || currentGamesOut.isEmpty()) {
            System.out.println("No current games out data for segment: " + segment.getSegmentName());
            return optimalHotNumbers;
        }
        
        // Find numbers in hot range (games out 1-5) and analyze their performance
        Map<Integer, Double> numberScores = new HashMap<>();
        int hotNumbersFound = 0;
        
        for (Map.Entry<Integer, Integer> entry : currentGamesOut.entrySet()) {
            int number = entry.getKey();
            int gamesOut = entry.getValue();
            
            // Only consider numbers in hot range
            if (gamesOut >= 1 && gamesOut <= 5) {
                hotNumbersFound++;
                double score = calculateNumberOptimalScore(number, history, gamesOut);
                numberScores.put(number, score);
                System.out.println("Hot number found - " + segment.getSegmentName() + ": " + number + " (games out: " + gamesOut + ", score: " + score + ")");
            }
        }
        
        System.out.println("Segment " + segment.getSegmentName() + " - Found " + hotNumbersFound + " hot numbers out of " + currentGamesOut.size() + " total numbers");
        
        // Sort by score and return top performers
        optimalHotNumbers = numberScores.entrySet().stream()
                .sorted(Map.Entry.<Integer, Double>comparingByValue().reversed())
                .limit(3) // Top 3 hot numbers
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
        
        return optimalHotNumbers;
    }
    
    /**
     * Calculates optimal normal numbers (games out 6-20) to select in this segment
     * based on historical hit patterns and due probability analysis
     */
    public static List<Integer> calculateOptimalNormalNumbers(NumberSegment segment, SegmentGameOutHistory history) {
        List<Integer> optimalNormalNumbers = new ArrayList<>();
        Map<Integer, Integer> currentGamesOut = segment.getCurrentGamesOut();
        
        if (currentGamesOut == null || currentGamesOut.isEmpty()) {
            System.out.println("No current games out data for normal numbers in segment: " + segment.getSegmentName());
            return optimalNormalNumbers;
        }
        
        // Find numbers in normal range (games out 6-20) and analyze their due probability
        Map<Integer, Double> numberScores = new HashMap<>();
        int normalNumbersFound = 0;
        
        for (Map.Entry<Integer, Integer> entry : currentGamesOut.entrySet()) {
            int number = entry.getKey();
            int gamesOut = entry.getValue();
            
            // Only consider numbers in normal range
            if (gamesOut >= 6 && gamesOut <= 20) {
                normalNumbersFound++;
                double score = calculateNumberDueScore(number, history, gamesOut);
                numberScores.put(number, score);
                System.out.println("Normal number found - " + segment.getSegmentName() + ": " + number + " (games out: " + gamesOut + ", score: " + score + ")");
            }
        }
        
        System.out.println("Segment " + segment.getSegmentName() + " - Found " + normalNumbersFound + " normal numbers out of " + currentGamesOut.size() + " total numbers");
        
        // Sort by score and return top performers
        optimalNormalNumbers = numberScores.entrySet().stream()
                .sorted(Map.Entry.<Integer, Double>comparingByValue().reversed())
                .limit(2) // Top 2 normal numbers
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
        
        return optimalNormalNumbers;
    }
    
    /**
     * Calculates an optimal score for hot numbers based on recent performance
     * and hit frequency at similar games out levels
     */
    private static double calculateNumberOptimalScore(int number, SegmentGameOutHistory history, int currentGamesOut) {
        List<SegmentGameOutHistory.GameOutEntry> numberHistory = history.getNumberHistories().get(number);
        
        if (numberHistory == null || numberHistory.isEmpty()) {
            return 0.0;
        }
        
        double score = 0.0;
        
        // Factor 1: Hit frequency at similar games out levels (40% weight)
        long hitsAtSimilarGamesOut = numberHistory.stream()
                .filter(entry -> Math.abs(entry.getGamesOutValue() - currentGamesOut) <= 2)
                .filter(SegmentGameOutHistory.GameOutEntry::isWasHit)
                .count();
        
        double hitFrequencyScore = (double) hitsAtSimilarGamesOut / Math.max(1, numberHistory.size()) * 40.0;
        score += hitFrequencyScore;
        
        // Factor 2: Recent performance (30% weight)
        long recentHits = numberHistory.stream()
                .skip(Math.max(0, numberHistory.size() - 50)) // Last 50 entries
                .filter(SegmentGameOutHistory.GameOutEntry::isWasHit)
                .count();
        
        double recentPerformanceScore = (double) recentHits / Math.min(50, numberHistory.size()) * 30.0;
        score += recentPerformanceScore;
        
        // Factor 3: Current games out proximity to average (30% weight)
        double avgHitGamesOut = numberHistory.stream()
                .filter(SegmentGameOutHistory.GameOutEntry::isWasHit)
                .mapToInt(SegmentGameOutHistory.GameOutEntry::getGamesOutValue)
                .average()
                .orElse(10.0);
        
        double proximityScore = Math.max(0, 30.0 - Math.abs(currentGamesOut - avgHitGamesOut) * 2.0);
        score += proximityScore;
        
        return score;
    }
    
    /**
     * Calculates a due score for normal numbers based on how overdue they are
     * compared to their historical average hit cycle
     */
    private static double calculateNumberDueScore(int number, SegmentGameOutHistory history, int currentGamesOut) {
        List<SegmentGameOutHistory.GameOutEntry> numberHistory = history.getNumberHistories().get(number);
        
        if (numberHistory == null || numberHistory.isEmpty()) {
            return 0.0;
        }
        
        double score = 0.0;
        
        // Factor 1: Due analysis - how overdue compared to historical average (50% weight)
        double avgGamesOutForHits = numberHistory.stream()
                .filter(SegmentGameOutHistory.GameOutEntry::isWasHit)
                .mapToInt(SegmentGameOutHistory.GameOutEntry::getGamesOutValue)
                .average()
                .orElse(10.0);
        
        double overdueRatio = Math.max(0, (currentGamesOut - avgGamesOutForHits) / avgGamesOutForHits);
        double dueScore = Math.min(50.0, overdueRatio * 25.0); // Cap at 50 points
        score += dueScore;
        
        // Factor 2: Historical performance at current games out level (30% weight)
        long hitsAtCurrentLevel = numberHistory.stream()
                .filter(entry -> Math.abs(entry.getGamesOutValue() - currentGamesOut) <= 3)
                .filter(SegmentGameOutHistory.GameOutEntry::isWasHit)
                .count();
        
        double historicalScore = (double) hitsAtCurrentLevel / Math.max(1, numberHistory.size()) * 30.0;
        score += historicalScore;
        
        // Factor 3: Consistency factor - numbers with more consistent cycles score higher (20% weight)
        double[] gamesOutForHits = numberHistory.stream()
                .filter(SegmentGameOutHistory.GameOutEntry::isWasHit)
                .mapToDouble(SegmentGameOutHistory.GameOutEntry::getGamesOutValue)
                .toArray();
        
        if (gamesOutForHits.length > 1) {
            double variance = calculateVariance(gamesOutForHits);
            double consistencyScore = Math.max(0, 20.0 - variance / 2.0);
            score += consistencyScore;
        }
        
        return score;
    }
    
    /**
     * Helper method to calculate variance for consistency analysis
     */
    private static double calculateVariance(double[] values) {
        if (values.length == 0) return 0.0;
        
        double mean = Arrays.stream(values).average().orElse(0.0);
        double sumSquaredDifferences = Arrays.stream(values)
                .map(x -> Math.pow(x - mean, 2))
                .sum();
        
        return sumSquaredDifferences / values.length;
    }
    
    /**
     * Calculates how accurately the optimal number predictions performed historically
     * by simulating the prediction algorithm on past data and tracking hits
     */
    public static void calculatePredictionAccuracy(NumberSegment segment, SegmentGameOutHistory history) {
        if (history == null || history.getNumberHistories().isEmpty()) {
            return;
        }
        
        int hotHits = 0, hotAttempts = 0;
        int normalHits = 0, normalAttempts = 0;
        
        // We'll simulate predictions by looking at historical scenarios
        // For each historical point, calculate what the optimal predictions would have been
        // then check if those predictions hit in the next few draws
        
        Map<Integer, List<SegmentGameOutHistory.GameOutEntry>> numberHistories = history.getNumberHistories();
        
        // For each number in this segment
        for (Map.Entry<Integer, List<SegmentGameOutHistory.GameOutEntry>> entry : numberHistories.entrySet()) {
            int number = entry.getKey();
            List<SegmentGameOutHistory.GameOutEntry> numberHistory = entry.getValue();
            
            if (numberHistory.size() < 50) continue; // Need sufficient data
            
            // Look at the last 200 historical scenarios (or all if less than 200)
            int scenarioStart = Math.max(0, numberHistory.size() - 200);
            
            for (int i = scenarioStart; i < numberHistory.size() - 10; i++) { // Leave 10 draws for prediction validation
                SegmentGameOutHistory.GameOutEntry currentEntry = numberHistory.get(i);
                int gamesOut = currentEntry.getGamesOutValue();
                
                // Simulate what our prediction algorithm would have recommended at this historical point
                boolean wouldPredictHot = wouldBeRecommendedAsHot(number, gamesOut, numberHistory, i);
                boolean wouldPredictNormal = wouldBeRecommendedAsNormal(number, gamesOut, numberHistory, i);
                
                if (wouldPredictHot) {
                    hotAttempts++;
                    // Check if this number hit in the next 5 draws (hot prediction window)
                    boolean hitInNext5 = checkIfHitInNextNDraws(numberHistory, i, 5);
                    if (hitInNext5) {
                        hotHits++;
                    }
                }
                
                if (wouldPredictNormal) {
                    normalAttempts++;
                    // Check if this number hit in the next 10 draws (normal prediction window)
                    boolean hitInNext10 = checkIfHitInNextNDraws(numberHistory, i, 10);
                    if (hitInNext10) {
                        normalHits++;
                    }
                }
            }
        }
        
        // Set the accuracy statistics
        segment.setHotPredictionHits(hotHits);
        segment.setHotPredictionAttempts(hotAttempts);
        segment.setNormalPredictionHits(normalHits);
        segment.setNormalPredictionAttempts(normalAttempts);
        
        // Calculate percentages
        double hotAccuracy = hotAttempts > 0 ? (double) hotHits / hotAttempts * 100 : 0.0;
        double normalAccuracy = normalAttempts > 0 ? (double) normalHits / normalAttempts * 100 : 0.0;
        
        segment.setHotAccuracyPercentage(hotAccuracy);
        segment.setNormalAccuracyPercentage(normalAccuracy);
        
        System.out.println("Prediction accuracy for " + segment.getSegmentName() + 
                " - Hot: " + String.format("%.1f%%", hotAccuracy) + 
                " (" + hotHits + "/" + hotAttempts + "), Normal: " + 
                String.format("%.1f%%", normalAccuracy) + " (" + normalHits + "/" + normalAttempts + ")");
    }
    
    /**
     * Determines if a number would have been recommended as hot at a historical point
     */
    private static boolean wouldBeRecommendedAsHot(int number, int gamesOut, 
                                                 List<SegmentGameOutHistory.GameOutEntry> numberHistory, int historicalIndex) {
        // A number would be recommended as hot if:
        // 1. It's in the hot range (games out 1-5)
        // 2. It has good historical performance at similar games out levels
        if (gamesOut < 1 || gamesOut > 5) return false;
        
        // Simplified version of the hot recommendation logic
        // Count recent hits at similar games out levels (using data up to historicalIndex only)
        int recentHits = 0;
        int recentOpportunities = 0;
        
        for (int i = Math.max(0, historicalIndex - 50); i < historicalIndex; i++) {
            SegmentGameOutHistory.GameOutEntry entry = numberHistory.get(i);
            if (Math.abs(entry.getGamesOutValue() - gamesOut) <= 2) {
                recentOpportunities++;
                if (i < numberHistory.size() - 1 && numberHistory.get(i + 1).isWasHit()) {
                    recentHits++;
                }
            }
        }
        
        // Would recommend if hit rate is above 25% with sufficient data
        return recentOpportunities >= 5 && (double) recentHits / recentOpportunities > 0.25;
    }
    
    /**
     * Determines if a number would have been recommended as normal/due at a historical point
     */
    private static boolean wouldBeRecommendedAsNormal(int number, int gamesOut,
                                                    List<SegmentGameOutHistory.GameOutEntry> numberHistory, int historicalIndex) {
        // A number would be recommended as normal if:
        // 1. It's in the normal range (games out 6-20)
        // 2. It's overdue compared to its historical average
        if (gamesOut < 6 || gamesOut > 20) return false;
        
        // Calculate historical average using data up to historicalIndex only
        double avgGamesOutForHits = 0.0;
        int hitCount = 0;
        
        for (int i = 0; i < historicalIndex; i++) {
            SegmentGameOutHistory.GameOutEntry entry = numberHistory.get(i);
            if (entry.isWasHit()) {
                avgGamesOutForHits += entry.getGamesOutValue();
                hitCount++;
            }
        }
        
        if (hitCount == 0) return false;
        avgGamesOutForHits /= hitCount;
        
        // Would recommend if current games out is significantly above average
        return gamesOut > avgGamesOutForHits * 1.3; // 30% above average
    }
    
    /**
     * Checks if a number hit in the next N draws after a historical point
     */
    private static boolean checkIfHitInNextNDraws(List<SegmentGameOutHistory.GameOutEntry> numberHistory, 
                                                int startIndex, int nDraws) {
        int endIndex = Math.min(numberHistory.size(), startIndex + nDraws + 1);
        
        for (int i = startIndex + 1; i < endIndex; i++) {
            if (numberHistory.get(i).isWasHit()) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * Calculates segment-level hit accuracy by analyzing historical data to determine
     * how accurately the system predicts the number of winning numbers from each segment.
     * For example, if winning numbers were 2-6-8-30-35, that's 3 numbers from segment 1-9.
     */
    public static void calculateSegmentHitAccuracy(NumberSegment segment, SegmentGameOutHistory history, 
                                                 List<DrawResultPattern> allDrawResults) {
        if (history == null || allDrawResults == null || allDrawResults.isEmpty()) {
            return;
        }
        
        int totalPredictions = 0;
        int accuratePredictions = 0;
        int totalExpectedHits = 0;
        int totalActualHits = 0;
        
        // Analyze the last 100 draws for segment hit accuracy
        int analysisSize = Math.min(100, allDrawResults.size());
        List<DrawResultPattern> recentDraws = allDrawResults.subList(
            Math.max(0, allDrawResults.size() - analysisSize), 
            allDrawResults.size()
        );
        
        for (DrawResultPattern draw : recentDraws) {
            // Count actual winning numbers from this segment
            int actualHitsFromSegment = 0;
            for (LotteryNumber lotteryNumber : draw.getLotteryNumbers()) {
                if (segment.containsNumber(lotteryNumber.getNumber()) && lotteryNumber.getGamesOut() == 0) {
                    actualHitsFromSegment++;
                }
            }
            
            // Calculate expected hits based on segment analysis
            int expectedHitsFromSegment = calculateExpectedHitsForDraw(segment, draw);
            
            totalPredictions++;
            totalExpectedHits += expectedHitsFromSegment;
            totalActualHits += actualHitsFromSegment;
            
            // Consider a prediction accurate if it's within 1 of the actual count
            if (Math.abs(expectedHitsFromSegment - actualHitsFromSegment) <= 1) {
                accuratePredictions++;
            }
            
            System.out.println("Segment " + segment.getSegmentName() + " draw " + draw.getDrawDate() + 
                             " - Expected: " + expectedHitsFromSegment + ", Actual: " + actualHitsFromSegment);
        }
        
        // Update segment statistics
        segment.setSegmentHitPredictions(accuratePredictions);
        segment.setSegmentHitAttempts(totalPredictions);
        segment.setExpectedHitsFromSegment(totalExpectedHits);
        segment.setActualHitsFromSegment(totalActualHits);
        
        double accuracy = totalPredictions > 0 ? (double) accuratePredictions / totalPredictions * 100 : 0.0;
        segment.setSegmentHitAccuracy(accuracy);
        
        System.out.println("Segment hit accuracy for " + segment.getSegmentName() + 
                         " - Accuracy: " + String.format("%.1f%%", accuracy) + 
                         " (" + accuratePredictions + "/" + totalPredictions + 
                         "), Expected: " + totalExpectedHits + ", Actual: " + totalActualHits);
    }
    
    /**
     * Calculates expected number of hits from this segment for a specific draw
     * based on segment performance metrics and current games out values
     */
    private static int calculateExpectedHitsForDraw(NumberSegment segment, DrawResultPattern draw) {
        Map<Integer, Integer> currentGamesOut = segment.getCurrentGamesOut();
        if (currentGamesOut == null || currentGamesOut.isEmpty()) {
            return 0;
        }
        
        // Analyze segment numbers and their hit probability based on games out
        double totalHitProbability = 0.0;
        int numbersInSegment = 0;
        
        for (Map.Entry<Integer, Integer> entry : currentGamesOut.entrySet()) {
            int number = entry.getKey();
            int gamesOut = entry.getValue();
            numbersInSegment++;
            
            // Calculate hit probability based on games out value
            double hitProbability = calculateHitProbability(gamesOut);
            totalHitProbability += hitProbability;
        }
        
        // Factor in segment historical performance
        double segmentHitRate = segment.getTotalHits() > 0 ? 
            (double) segment.getTotalHits() / (numbersInSegment * 100) : 0.02; // Default 2% if no history
        
        // Adjust expected hits based on typical lottery game mechanics
        // Most lottery games draw 5-6 numbers, so scale appropriately
        double expectedHits = totalHitProbability * segmentHitRate * 5.0; // Assume 5-number draw
        
        // Round to nearest integer and ensure reasonable bounds
        return Math.max(0, Math.min(numbersInSegment, (int) Math.round(expectedHits)));
    }
    
    /**
     * Calculates hit probability based on games out value
     * Lower games out = higher hit probability
     */
    private static double calculateHitProbability(int gamesOut) {
        if (gamesOut == 0) return 1.0; // Already hit
        if (gamesOut <= 3) return 0.15; // Hot numbers
        if (gamesOut <= 7) return 0.10; // Warm numbers  
        if (gamesOut <= 15) return 0.06; // Normal numbers
        if (gamesOut <= 25) return 0.04; // Due numbers
        return 0.02; // Cold numbers
    }
}