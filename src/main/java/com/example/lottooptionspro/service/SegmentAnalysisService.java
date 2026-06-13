package com.example.lottooptionspro.service;

import com.example.lottooptionspro.model.dashboard.NumberSegment;
import com.example.lottooptionspro.model.dashboard.SegmentAnalysisResult;
import com.example.lottooptionspro.model.dashboard.SegmentGameOutHistory;
import com.example.lottooptionspro.util.SegmentCalculator;
import com.floyd.model.dashboard.DrawResultPattern;
import com.floyd.model.dashboard.LotteryNumber;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class SegmentAnalysisService {
    private static final Logger logger = LoggerFactory.getLogger(SegmentAnalysisService.class);
    private static final int DEFAULT_SEGMENT_SIZE = 10;
    
    public SegmentAnalysisResult analyzeSegments(List<DrawResultPattern> drawResultPatterns) {
        if (drawResultPatterns == null || drawResultPatterns.isEmpty()) {
            return new SegmentAnalysisResult();
        }
        
        // Performance optimization: limit data processing for very large datasets
        List<DrawResultPattern> processedPatterns = drawResultPatterns;
        if (drawResultPatterns.size() > 1000) {
            // For large datasets, use only the most recent 1000 draws for faster processing
            processedPatterns = drawResultPatterns.subList(
                Math.max(0, drawResultPatterns.size() - 1000), 
                drawResultPatterns.size()
            );
        }
        
        int minNumber = findMinNumber(processedPatterns);
        int maxNumber = findMaxNumber(processedPatterns);
        
        // Parallel processing for segment creation
        List<NumberSegment> segments = SegmentCalculator.createSegments(minNumber, maxNumber, DEFAULT_SEGMENT_SIZE);
        
        // Optimized batch processing for segment histories
        Map<String, SegmentGameOutHistory> histories = SegmentCalculator.buildSegmentHistories(segments, processedPatterns);
        
        // Sequential processing for segment data population — populateSegmentDataOptimized
        // calls SegmentCalculator helpers that mutate the segment and are not documented
        // as thread-safe, so avoid parallelStream here.
        final List<DrawResultPattern> finalProcessedPatterns = processedPatterns;
        segments.forEach(segment -> {
            SegmentGameOutHistory history = histories.get(segment.getSegmentName());
            if (history != null) {
                populateSegmentDataOptimized(segment, history, finalProcessedPatterns);
            }
        });
        
        SegmentAnalysisResult result = new SegmentAnalysisResult();
        result.setSegments(segments);
        result.setSegmentHistories(histories);
        
        // Parallel processing for metrics calculation
        Map<String, SegmentAnalysisResult.SegmentPerformanceMetrics> segmentMetrics = segments.parallelStream()
            .filter(segment -> histories.get(segment.getSegmentName()) != null)
            .collect(Collectors.toConcurrentMap(
                NumberSegment::getSegmentName,
                segment -> SegmentCalculator.calculateSegmentMetrics(segment, histories.get(segment.getSegmentName()))
            ));
        result.setSegmentMetrics(segmentMetrics);
        
        SegmentAnalysisResult.OverallAnalysis overallAnalysis = createOverallAnalysis(segments, processedPatterns);
        result.setOverallAnalysis(overallAnalysis);
        
        List<SegmentAnalysisResult.SegmentRecommendation> recommendations = generateSegmentRecommendations(segments, histories);
        result.setRecommendations(recommendations);
        
        return result;
    }
    
    private void populateSegmentDataOptimized(NumberSegment segment, SegmentGameOutHistory history, List<DrawResultPattern> processedPatterns) {
        Map<Integer, Integer> currentGamesOut = history.getCurrentGamesOutMap();
        segment.setCurrentGamesOut(currentGamesOut);
        
        // Optimized calculations using streams where possible
        if (!currentGamesOut.isEmpty()) {
            double averageGamesOut = currentGamesOut.values().stream()
                    .mapToInt(Integer::intValue)
                    .average()
                    .orElse(0.0);
            segment.setAverageGamesOut(averageGamesOut);
        }
        
        // Optimized hit counting
        int totalHits = history.getNumberHistories().values().parallelStream()
                .mapToInt(entries -> (int) entries.stream().filter(SegmentGameOutHistory.GameOutEntry::isWasHit).count())
                .sum();
        segment.setTotalHits(totalHits);
        
        // Optimized last hit date finding
        String lastHitDate = history.getNumberHistories().values().parallelStream()
                .flatMap(Collection::stream)
                .filter(SegmentGameOutHistory.GameOutEntry::isWasHit)
                .map(SegmentGameOutHistory.GameOutEntry::getDrawDate)
                .max(String::compareTo)
                .orElse("No hits found");
        segment.setLastHitDate(lastHitDate);
        
        // Generate recommendations with optimized algorithm
        List<Integer> recommendations = generateOptimizedRecommendations(history, currentGamesOut);
        segment.setRecommendedGamesOut(recommendations);
        
        // Calculate optimal number selections for hot and normal groups
        logger.debug("Calculating optimal numbers for segment: {}", segment.getSegmentName());
        List<Integer> optimalHotNumbers = SegmentCalculator.calculateOptimalHotNumbers(segment, history);
        segment.setOptimalHotNumbers(optimalHotNumbers);
        logger.debug("Set {} optimal hot numbers for {}",
                optimalHotNumbers != null ? optimalHotNumbers.size() : 0, segment.getSegmentName());

        List<Integer> optimalNormalNumbers = SegmentCalculator.calculateOptimalNormalNumbers(segment, history);
        segment.setOptimalNormalNumbers(optimalNormalNumbers);
        logger.debug("Set {} optimal normal numbers for {}",
                optimalNormalNumbers != null ? optimalNormalNumbers.size() : 0, segment.getSegmentName());
        
        // Calculate prediction accuracy statistics
        SegmentCalculator.calculatePredictionAccuracy(segment, history);
        
        // Calculate segment-level hit accuracy
        SegmentCalculator.calculateSegmentHitAccuracy(segment, history, processedPatterns);
    }
    
    private List<Integer> generateOptimizedRecommendations(SegmentGameOutHistory history, Map<Integer, Integer> currentGamesOut) {
        // Simplified but efficient recommendation algorithm for large datasets
        Map<Integer, Long> hitPatterns = history.getNumberHistories().values().parallelStream()
                .flatMap(Collection::stream)
                .filter(SegmentGameOutHistory.GameOutEntry::isWasHit)
                .collect(Collectors.groupingBy(
                    entry -> Math.min(50, Math.max(1, entry.getGamesOutValue())), // Clamp to reasonable range
                    Collectors.counting()
                ));
        
        return hitPatterns.entrySet().stream()
                .sorted(Map.Entry.<Integer, Long>comparingByValue().reversed())
                .limit(3)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
    }
    
    public List<NumberSegment> getSegmentsForRange(int minNumber, int maxNumber) {
        return SegmentCalculator.createSegments(minNumber, maxNumber, DEFAULT_SEGMENT_SIZE);
    }
    
    public List<NumberSegment> getSegmentsForRange(int minNumber, int maxNumber, int segmentSize) {
        return SegmentCalculator.createSegments(minNumber, maxNumber, segmentSize);
    }
    
    private int findMinNumber(List<DrawResultPattern> drawResultPatterns) {
        return drawResultPatterns.stream()
                .flatMap(pattern -> pattern.getLotteryNumbers().stream())
                .mapToInt(LotteryNumber::getNumber)
                .min()
                .orElse(1);
    }
    
    private int findMaxNumber(List<DrawResultPattern> drawResultPatterns) {
        return drawResultPatterns.stream()
                .flatMap(pattern -> pattern.getLotteryNumbers().stream())
                .mapToInt(LotteryNumber::getNumber)
                .max()
                .orElse(54);
    }
    
    private SegmentAnalysisResult.OverallAnalysis createOverallAnalysis(List<NumberSegment> segments, 
                                                                      List<DrawResultPattern> drawResultPatterns) {
        SegmentAnalysisResult.OverallAnalysis analysis = new SegmentAnalysisResult.OverallAnalysis();
        
        analysis.setTotalSegments(segments.size());
        analysis.setTotalDrawsAnalyzed(drawResultPatterns.size());
        
        double overallAverage = segments.stream()
                .mapToDouble(NumberSegment::getAverageGamesOut)
                .average()
                .orElse(0.0);
        analysis.setOverallAverageGamesOut(overallAverage);
        
        Optional<NumberSegment> mostProductive = segments.stream()
                .max(Comparator.comparing(NumberSegment::getTotalHits));
        if (mostProductive.isPresent()) {
            analysis.setMostProductiveSegment(mostProductive.get().getSegmentName());
        }
        
        Optional<NumberSegment> leastProductive = segments.stream()
                .min(Comparator.comparing(NumberSegment::getTotalHits));
        if (leastProductive.isPresent()) {
            analysis.setLeastProductiveSegment(leastProductive.get().getSegmentName());
        }
        
        if (!drawResultPatterns.isEmpty()) {
            String latestDate = drawResultPatterns.stream()
                    .map(DrawResultPattern::getDrawDate)
                    .max(String::compareTo)
                    .orElse("Unknown");
            analysis.setAnalysisDate(latestDate);
        }
        
        return analysis;
    }
    
    private List<SegmentAnalysisResult.SegmentRecommendation> generateSegmentRecommendations(
            List<NumberSegment> segments, Map<String, SegmentGameOutHistory> histories) {
        
        List<SegmentAnalysisResult.SegmentRecommendation> recommendations = new ArrayList<>();
        
        for (NumberSegment segment : segments) {
            SegmentGameOutHistory history = histories.get(segment.getSegmentName());
            if (history != null && segment.getRecommendedGamesOut() != null && !segment.getRecommendedGamesOut().isEmpty()) {
                
                SegmentAnalysisResult.SegmentRecommendation recommendation = 
                        new SegmentAnalysisResult.SegmentRecommendation();
                
                recommendation.setSegmentName(segment.getSegmentName());
                recommendation.setRecommendedGamesOutValues(segment.getRecommendedGamesOut());
                
                double confidenceScore = calculateConfidenceScore(segment, history);
                recommendation.setConfidenceScore(confidenceScore);
                
                String reasoning = generateReasoning(segment, history);
                recommendation.setReasoning(reasoning);
                
                String priority = determinePriority(segment, confidenceScore);
                recommendation.setPriority(priority);
                
                recommendations.add(recommendation);
            }
        }
        
        return recommendations.stream()
                .sorted((r1, r2) -> Double.compare(r2.getConfidenceScore(), r1.getConfidenceScore()))
                .collect(Collectors.toList());
    }
    
    private double calculateConfidenceScore(NumberSegment segment, SegmentGameOutHistory history) {
        int totalHits = segment.getTotalHits();
        double averageGamesOut = segment.getAverageGamesOut();
        int totalEntries = history.getNumberHistories().values().stream().mapToInt(List::size).sum();
        
        if (totalEntries == 0) return 0.0;
        
        double hitRate = (double) totalHits / totalEntries;
        double consistencyFactor = averageGamesOut > 0 ? Math.min(1.0, 10.0 / averageGamesOut) : 0.0;
        double dataVolumeFactor = Math.min(1.0, totalEntries / 100.0);
        
        return (hitRate * 0.4 + consistencyFactor * 0.3 + dataVolumeFactor * 0.3) * 100;
    }
    
    private String generateReasoning(NumberSegment segment, SegmentGameOutHistory history) {
        StringBuilder reasoning = new StringBuilder();
        
        reasoning.append("Based on analysis of ").append(segment.getSegmentName()).append(": ");
        
        if (segment.getTotalHits() > 0) {
            reasoning.append("Historical patterns show ").append(segment.getTotalHits())
                    .append(" hits with average games out of ")
                    .append(String.format("%.1f", segment.getAverageGamesOut())).append(". ");
        }
        
        if (segment.getRecommendedGamesOut() != null && !segment.getRecommendedGamesOut().isEmpty()) {
            reasoning.append("Recommended games out values (")
                    .append(segment.getRecommendedGamesOut().stream()
                            .map(String::valueOf)
                            .collect(Collectors.joining(", ")))
                    .append(") show strong historical performance.");
        }
        
        return reasoning.toString();
    }
    
    private String determinePriority(NumberSegment segment, double confidenceScore) {
        if (confidenceScore >= 80) {
            return "HIGH";
        } else if (confidenceScore >= 60) {
            return "MEDIUM";
        } else {
            return "LOW";
        }
    }
}