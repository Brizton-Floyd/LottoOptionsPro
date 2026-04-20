package com.example.lottooptionspro.service;

import com.example.lottooptionspro.model.dashboard.PositionAnalysisResult;
import com.example.lottooptionspro.model.dashboard.PositionTrendData;
import com.example.lottooptionspro.model.dashboard.PositionTrendPoint;
import com.floyd.model.dashboard.DrawResultPattern;
import com.floyd.model.dashboard.LotteryNumber;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class PositionTrendService {
    
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    
    public PositionTrendData analyzePositionTrends(List<DrawResultPattern> drawPatterns, 
                                                  int position, String timeRange) {
        if (drawPatterns == null || drawPatterns.isEmpty()) {
            return createEmptyTrendData(position, timeRange);
        }
        
        // Filter draws based on time range
        List<DrawResultPattern> filteredDraws = filterDrawsByTimeRange(drawPatterns, timeRange);
        
        if (filteredDraws.isEmpty()) {
            return createEmptyTrendData(position, timeRange);
        }
        
        // Generate trend points for the position
        List<PositionTrendPoint> trendPoints = generateTrendPoints(filteredDraws, position);
        
        // Calculate technical indicators
        Map<String, Double> technicalIndicators = calculateTechnicalIndicators(trendPoints);
        
        // Perform position analysis
        PositionAnalysisResult analysisResult = analyzePosition(filteredDraws, position, trendPoints);
        
        // Determine date range
        LocalDate startDate = parseDrawDate(filteredDraws.get(filteredDraws.size() - 1).getDrawDate());
        LocalDate endDate = parseDrawDate(filteredDraws.get(0).getDrawDate());
        
        return new PositionTrendData(position, timeRange, trendPoints, technicalIndicators,
                                   analysisResult, startDate, endDate);
    }
    
    private List<DrawResultPattern> filterDrawsByTimeRange(List<DrawResultPattern> drawPatterns, String timeRange) {
        int maxDraws;
        switch (timeRange) {
            case "Last 30 draws":
                maxDraws = 30;
                break;
            case "Last 60 draws":
                maxDraws = 60;
                break;
            case "Last 90 draws":
                maxDraws = 90;
                break;
            default: // "All time"
                return new ArrayList<>(drawPatterns);
        }
        
        return drawPatterns.size() <= maxDraws ? 
            new ArrayList<>(drawPatterns) : 
            drawPatterns.subList(0, maxDraws);
    }
    
    private List<PositionTrendPoint> generateTrendPoints(List<DrawResultPattern> draws, int position) {
        List<PositionTrendPoint> points = new ArrayList<>();
        
        // Group draws into periods (every 5 draws for OHLC calculation)
        int periodSize = 5;
        
        for (int i = 0; i < draws.size(); i += periodSize) {
            int endIndex = Math.min(i + periodSize, draws.size());
            List<DrawResultPattern> periodDraws = draws.subList(i, endIndex);
            
            PositionTrendPoint point = createTrendPoint(periodDraws, position, i / periodSize);
            if (point != null) {
                points.add(point);
            }
        }
        
        // Calculate moving averages for all points
        calculateMovingAveragesForPoints(points);
        
        return points;
    }
    
    private PositionTrendPoint createTrendPoint(List<DrawResultPattern> periodDraws, int position, int periodIndex) {
        if (periodDraws.isEmpty()) return null;
        
        // Extract games out values for this position across the period
        List<Integer> gamesOutValues = new ArrayList<>();
        List<Integer> numbersInPosition = new ArrayList<>();
        int hitCount = 0;
        
        for (DrawResultPattern draw : periodDraws) {
            if (draw.getLotteryNumbers().size() > position - 1) {
                LotteryNumber numberAtPosition = draw.getLotteryNumbers().get(position - 1);
                int gamesOut = numberAtPosition.getGamesOut();
                gamesOutValues.add(gamesOut);
                numbersInPosition.add(numberAtPosition.getNumber());
                
                if (gamesOut == 0) {
                    hitCount++;
                }
            }
        }
        
        if (gamesOutValues.isEmpty()) return null;
        
        // Calculate OHLC values
        double open = gamesOutValues.get(0);
        double close = gamesOutValues.get(gamesOutValues.size() - 1);
        double high = Collections.max(gamesOutValues);
        double low = Collections.min(gamesOutValues);
        
        // Calculate hit frequency
        double hitFrequency = (double) hitCount / periodDraws.size();
        
        // Use the latest draw date as the point date
        String drawDate = periodDraws.get(0).getDrawDate();
        LocalDate date = parseDrawDate(drawDate);
        
        // Calculate trend (simplified for now)
        String trendDirection = close > open ? "BULLISH" : close < open ? "BEARISH" : "SIDEWAYS";
        double trendStrength = Math.abs(close - open) / Math.max(1, high - low);
        
        return new PositionTrendPoint(
            drawDate, date, periodIndex,
            open, high, low, close,
            hitCount, periodDraws.size(), hitFrequency,
            numbersInPosition,
            new HashMap<>(), // Moving averages will be calculated later
            trendStrength, trendDirection
        );
    }
    
    private void calculateMovingAveragesForPoints(List<PositionTrendPoint> points) {
        for (int i = 0; i < points.size(); i++) {
            PositionTrendPoint point = points.get(i);
            Map<String, Double> movingAverages = new HashMap<>();
            
            // Calculate MA5
            if (i >= 4) {
                double ma5 = points.subList(i - 4, i + 1).stream()
                    .mapToDouble(PositionTrendPoint::getClose)
                    .average().orElse(0.0);
                movingAverages.put("MA5", ma5);
            }
            
            // Calculate MA10
            if (i >= 9) {
                double ma10 = points.subList(i - 9, i + 1).stream()
                    .mapToDouble(PositionTrendPoint::getClose)
                    .average().orElse(0.0);
                movingAverages.put("MA10", ma10);
            }
            
            // Calculate MA20
            if (i >= 19) {
                double ma20 = points.subList(i - 19, i + 1).stream()
                    .mapToDouble(PositionTrendPoint::getClose)
                    .average().orElse(0.0);
                movingAverages.put("MA20", ma20);
            }
            
            // Update the point with calculated moving averages
            point.getMovingAverages().putAll(movingAverages);
        }
    }
    
    private Map<String, Double> calculateTechnicalIndicators(List<PositionTrendPoint> points) {
        Map<String, Double> indicators = new HashMap<>();
        
        if (points.isEmpty()) return indicators;
        
        // Current moving averages
        PositionTrendPoint lastPoint = points.get(points.size() - 1);
        indicators.putAll(lastPoint.getMovingAverages());
        
        // Trend strength (average of most recent 10 trend strengths)
        int fromIndex = Math.max(0, points.size() - 10);
        double avgTrendStrength = points.subList(fromIndex, points.size()).stream()
            .mapToDouble(PositionTrendPoint::getTrendStrength)
            .average().orElse(0.0);
        indicators.put("TrendStrength", avgTrendStrength);
        
        // Volatility (standard deviation of close values)
        double avgClose = points.stream().mapToDouble(PositionTrendPoint::getClose).average().orElse(0.0);
        double volatility = Math.sqrt(points.stream()
            .mapToDouble(p -> Math.pow(p.getClose() - avgClose, 2))
            .average().orElse(0.0));
        indicators.put("Volatility", volatility);
        
        // Hit frequency trend
        double avgHitFreq = points.stream().mapToDouble(PositionTrendPoint::getHitFrequency).average().orElse(0.0);
        indicators.put("AvgHitFrequency", avgHitFreq);
        
        return indicators;
    }
    
    private PositionAnalysisResult analyzePosition(List<DrawResultPattern> draws, int position, 
                                                  List<PositionTrendPoint> trendPoints) {
        // Extract all numbers that appeared in this position
        Map<Integer, Integer> numberFrequency = new HashMap<>();
        double totalGamesOut = 0.0;
        
        for (DrawResultPattern draw : draws) {
            if (draw.getLotteryNumbers().size() > position - 1) {
                LotteryNumber number = draw.getLotteryNumbers().get(position - 1);
                numberFrequency.merge(number.getNumber(), 1, Integer::sum);
                totalGamesOut += number.getGamesOut();
            }
        }
        
        double avgGamesOut = totalGamesOut / draws.size();
        
        // Determine hot and cold numbers
        List<Integer> hotNumbers = numberFrequency.entrySet().stream()
            .sorted(Map.Entry.<Integer, Integer>comparingByValue().reversed())
            .limit(5)
            .map(Map.Entry::getKey)
            .collect(Collectors.toList());
            
        List<Integer> coldNumbers = numberFrequency.entrySet().stream()
            .sorted(Map.Entry.comparingByValue())
            .limit(5)
            .map(Map.Entry::getKey)
            .collect(Collectors.toList());
        
        // Analyze trend
        String primaryTrend = determinePrimaryTrend(trendPoints);
        double trendStrength = calculateOverallTrendStrength(trendPoints);
        String trendDuration = determineTrendDuration(trendPoints);
        
        // Calculate volatility
        double volatility = trendPoints.isEmpty() ? 0.0 : 
            trendPoints.stream().mapToDouble(PositionTrendPoint::getTrendStrength).average().orElse(0.0);
        
        // Generate insights
        List<String> insights = generateInsights(position, avgGamesOut, primaryTrend, hotNumbers, coldNumbers);
        
        // Generate recommendations (simplified)
        List<Integer> recommendations = new ArrayList<>();
        if ("BULLISH".equals(primaryTrend)) {
            recommendations.addAll(hotNumbers.subList(0, Math.min(3, hotNumbers.size())));
        } else {
            recommendations.addAll(coldNumbers.subList(0, Math.min(3, coldNumbers.size())));
        }
        
        double confidenceScore = Math.min(0.9, trendStrength * (draws.size() / 50.0));
        
        return new PositionAnalysisResult(
            position, draws.size(), avgGamesOut, volatility,
            primaryTrend, trendStrength, trendDuration,
            numberFrequency, hotNumbers, coldNumbers, new ArrayList<>(),
            new ArrayList<>(), new ArrayList<>(), new HashMap<>(),
            "NEUTRAL", insights, recommendations, confidenceScore
        );
    }
    
    private String determinePrimaryTrend(List<PositionTrendPoint> points) {
        if (points.size() < 5) return "NEUTRAL";
        
        List<PositionTrendPoint> recentPoints = points.subList(Math.max(0, points.size() - 10), points.size());
        
        long bullishCount = recentPoints.stream()
            .filter(PositionTrendPoint::isBullish)
            .count();
        long bearishCount = recentPoints.stream()
            .filter(PositionTrendPoint::isBearish)
            .count();
            
        if (bullishCount > bearishCount * 1.5) return "BULLISH";
        if (bearishCount > bullishCount * 1.5) return "BEARISH";
        return "SIDEWAYS";
    }
    
    private double calculateOverallTrendStrength(List<PositionTrendPoint> points) {
        return points.stream()
            .mapToDouble(PositionTrendPoint::getTrendStrength)
            .average()
            .orElse(0.0);
    }
    
    private String determineTrendDuration(List<PositionTrendPoint> points) {
        // Simplified duration analysis
        if (points.size() > 20) return "LONG";
        if (points.size() > 10) return "MEDIUM";
        return "SHORT";
    }
    
    private List<String> generateInsights(int position, double avgGamesOut, String trend, 
                                        List<Integer> hotNumbers, List<Integer> coldNumbers) {
        List<String> insights = new ArrayList<>();
        
        insights.add("Position " + position + " shows " + trend.toLowerCase() + " trend");
        insights.add("Average games out: " + String.format("%.1f", avgGamesOut));
        
        if (!hotNumbers.isEmpty()) {
            insights.add("Hot numbers: " + hotNumbers.stream()
                .map(String::valueOf)
                .collect(Collectors.joining(", ")));
        }
        
        if (!coldNumbers.isEmpty()) {
            insights.add("Cold numbers: " + coldNumbers.stream()
                .map(String::valueOf)
                .collect(Collectors.joining(", ")));
        }
        
        return insights;
    }
    
    private PositionTrendData createEmptyTrendData(int position, String timeRange) {
        return new PositionTrendData(position, timeRange, new ArrayList<>(), 
            new HashMap<>(), null, null, null);
    }
    
    private LocalDate parseDrawDate(String dateStr) {
        try {
            return LocalDate.parse(dateStr, DATE_FORMATTER);
        } catch (Exception e) {
            return LocalDate.now();
        }
    }
}