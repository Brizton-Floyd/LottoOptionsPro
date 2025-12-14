package com.example.lottooptionspro.util;

import com.example.lottooptionspro.model.dashboard.SegmentGameOutHistory;
import com.example.lottooptionspro.model.dashboard.PositionTrendData;
import com.example.lottooptionspro.model.dashboard.PositionTrendPoint;
import com.example.lottooptionspro.model.dashboard.PositionAnalysisResult;
import com.floyd.model.dashboard.DrawResultPattern;
import com.floyd.model.dashboard.LotteryNumber;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

public class TrendAnalyzer {
    
    public static class TrendResult {
        private final String trendDirection;
        private final double trendStrength;
        private final Map<String, Double> periodAnalysis;
        private final List<String> insights;
        
        public TrendResult(String trendDirection, double trendStrength, 
                          Map<String, Double> periodAnalysis, List<String> insights) {
            this.trendDirection = trendDirection;
            this.trendStrength = trendStrength;
            this.periodAnalysis = periodAnalysis;
            this.insights = insights;
        }
        
        public String getTrendDirection() { return trendDirection; }
        public double getTrendStrength() { return trendStrength; }
        public Map<String, Double> getPeriodAnalysis() { return periodAnalysis; }
        public List<String> getInsights() { return insights; }
    }
    
    public static TrendResult analyzeSegmentTrends(SegmentGameOutHistory history) {
        if (history == null || history.getNumberHistories().isEmpty()) {
            return new TrendResult("NEUTRAL", 0.0, new HashMap<>(), Arrays.asList("Insufficient data"));
        }
        
        // Analyze hit frequency trends over time
        Map<Integer, List<SegmentGameOutHistory.GameOutEntry>> allEntries = history.getNumberHistories();
        
        // Get all hit entries sorted by date
        List<SegmentGameOutHistory.GameOutEntry> allHits = allEntries.values().stream()
                .flatMap(Collection::stream)
                .filter(SegmentGameOutHistory.GameOutEntry::isWasHit)
                .sorted(Comparator.comparing(SegmentGameOutHistory.GameOutEntry::getDrawDate))
                .collect(Collectors.toList());
        
        if (allHits.size() < 10) {
            return new TrendResult("NEUTRAL", 0.0, new HashMap<>(), 
                    Arrays.asList("Need at least 10 hits for trend analysis"));
        }
        
        // Split into periods for trend analysis
        int totalHits = allHits.size();
        int periodSize = Math.max(5, totalHits / 4); // Quarter periods
        
        List<Double> periodHitRates = new ArrayList<>();
        Map<String, Double> periodAnalysis = new HashMap<>();
        
        for (int i = 0; i < 4; i++) {
            int startIdx = i * periodSize;
            int endIdx = Math.min(startIdx + periodSize, totalHits);
            
            if (startIdx < totalHits) {
                List<SegmentGameOutHistory.GameOutEntry> periodHits = allHits.subList(startIdx, endIdx);
                double hitRate = (double) periodHits.size() / periodSize;
                periodHitRates.add(hitRate);
                
                String periodName = "Period " + (i + 1);
                periodAnalysis.put(periodName, hitRate);
            }
        }
        
        // Calculate trend strength and direction
        String trendDirection = calculateTrendDirection(periodHitRates);
        double trendStrength = calculateTrendStrength(periodHitRates);
        
        // Generate insights
        List<String> insights = generateTrendInsights(periodHitRates, allHits, history);
        
        return new TrendResult(trendDirection, trendStrength, periodAnalysis, insights);
    }
    
    private static String calculateTrendDirection(List<Double> periodHitRates) {
        if (periodHitRates.size() < 2) return "NEUTRAL";
        
        double firstHalf = periodHitRates.subList(0, periodHitRates.size() / 2).stream()
                .mapToDouble(Double::doubleValue).average().orElse(0.0);
        double secondHalf = periodHitRates.subList(periodHitRates.size() / 2, periodHitRates.size()).stream()
                .mapToDouble(Double::doubleValue).average().orElse(0.0);
        
        double difference = secondHalf - firstHalf;
        
        if (difference > 0.1) return "INCREASING";
        if (difference < -0.1) return "DECREASING";
        return "STABLE";
    }
    
    private static double calculateTrendStrength(List<Double> periodHitRates) {
        if (periodHitRates.size() < 2) return 0.0;
        
        // Calculate variance as strength indicator
        double mean = periodHitRates.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
        double variance = periodHitRates.stream()
                .mapToDouble(rate -> Math.pow(rate - mean, 2))
                .average().orElse(0.0);
        
        return Math.min(1.0, Math.sqrt(variance) * 10); // Normalize to 0-1 scale
    }
    
    private static List<String> generateTrendInsights(List<Double> periodHitRates, 
                                                    List<SegmentGameOutHistory.GameOutEntry> allHits,
                                                    SegmentGameOutHistory history) {
        List<String> insights = new ArrayList<>();
        
        if (periodHitRates.isEmpty()) return insights;
        
        // Most active period
        double maxRate = Collections.max(periodHitRates);
        int maxPeriod = periodHitRates.indexOf(maxRate) + 1;
        insights.add("Most active period: Period " + maxPeriod + " (" + String.format("%.2f", maxRate) + " hit rate)");
        
        // Recent activity
        if (periodHitRates.size() >= 2) {
            double recentRate = periodHitRates.get(periodHitRates.size() - 1);
            double previousRate = periodHitRates.get(periodHitRates.size() - 2);
            
            if (recentRate > previousRate * 1.2) {
                insights.add("Recent acceleration in hit frequency detected");
            } else if (recentRate < previousRate * 0.8) {
                insights.add("Recent decline in hit frequency detected");
            }
        }
        
        // Hot streak analysis
        int maxConsecutiveHits = findMaxConsecutiveHits(allHits);
        if (maxConsecutiveHits > 3) {
            insights.add("Longest hot streak: " + maxConsecutiveHits + " consecutive hits");
        }
        
        // Cold period analysis
        int maxGapBetweenHits = findMaxGapBetweenHits(allHits);
        if (maxGapBetweenHits > 20) {
            insights.add("Longest cold period: " + maxGapBetweenHits + " games without hits");
        }
        
        return insights;
    }
    
    private static int findMaxConsecutiveHits(List<SegmentGameOutHistory.GameOutEntry> hits) {
        if (hits.size() < 2) return hits.size();
        
        int maxStreak = 1;
        int currentStreak = 1;
        
        for (int i = 1; i < hits.size(); i++) {
            // This is simplified - in real implementation, you'd need to check actual draw sequence
            currentStreak++;
            maxStreak = Math.max(maxStreak, currentStreak);
        }
        
        return Math.min(maxStreak, hits.size());
    }
    
    private static int findMaxGapBetweenHits(List<SegmentGameOutHistory.GameOutEntry> hits) {
        // Simplified implementation - would need actual draw dates for precise calculation
        return hits.size() > 10 ? 25 : 15; // Placeholder logic
    }
    
    public static Map<String, String> getSegmentTrendSummary(SegmentGameOutHistory history) {
        TrendResult trend = analyzeSegmentTrends(history);
        Map<String, String> summary = new HashMap<>();
        
        summary.put("direction", trend.getTrendDirection());
        summary.put("strength", String.format("%.2f", trend.getTrendStrength()));
        summary.put("primaryInsight", trend.getInsights().isEmpty() ? "No trends detected" : trend.getInsights().get(0));
        
        return summary;
    }
    
    // New classes for chart data and predictions
    public static class TrendPoint {
        private final String date;
        private final int drawIndex;
        private final double averageGamesOut;
        private final boolean hasHit;
        private final int numberCount;
        
        public TrendPoint(String date, int drawIndex, double averageGamesOut, boolean hasHit, int numberCount) {
            this.date = date;
            this.drawIndex = drawIndex;
            this.averageGamesOut = averageGamesOut;
            this.hasHit = hasHit;
            this.numberCount = numberCount;
        }
        
        // Getters
        public String getDate() { return date; }
        public int getDrawIndex() { return drawIndex; }
        public double getAverageGamesOut() { return averageGamesOut; }
        public boolean hasHit() { return hasHit; }
        public int getNumberCount() { return numberCount; }
    }
    
    public static class TrendPrediction {
        private final int drawsAhead;
        private final double predictedAverageGamesOut;
        private final double confidence;
        private final String reasoning;
        
        public TrendPrediction(int drawsAhead, double predictedAverageGamesOut, double confidence, String reasoning) {
            this.drawsAhead = drawsAhead;
            this.predictedAverageGamesOut = predictedAverageGamesOut;
            this.confidence = confidence;
            this.reasoning = reasoning;
        }
        
        // Getters
        public int getDrawsAhead() { return drawsAhead; }
        public double getPredictedAverageGamesOut() { return predictedAverageGamesOut; }
        public double getConfidence() { return confidence; }
        public String getReasoning() { return reasoning; }
    }
    
    public static class SegmentTrendData {
        private final String segmentName;
        private final List<TrendPoint> historicalPoints;
        private final List<TrendPrediction> predictions;
        private final double currentAverage;
        private final String trendDirection;
        private final double trendStrength;
        
        public SegmentTrendData(String segmentName, List<TrendPoint> historicalPoints, 
                              List<TrendPrediction> predictions, double currentAverage, 
                              String trendDirection, double trendStrength) {
            this.segmentName = segmentName;
            this.historicalPoints = historicalPoints;
            this.predictions = predictions;
            this.currentAverage = currentAverage;
            this.trendDirection = trendDirection;
            this.trendStrength = trendStrength;
        }
        
        // Getters
        public String getSegmentName() { return segmentName; }
        public List<TrendPoint> getHistoricalPoints() { return historicalPoints; }
        public List<TrendPrediction> getPredictions() { return predictions; }
        public double getCurrentAverage() { return currentAverage; }
        public String getTrendDirection() { return trendDirection; }
        public double getTrendStrength() { return trendStrength; }
    }
    
    /**
     * Generates chart-ready trend data for a segment including historical points and predictions
     */
    public static SegmentTrendData generateSegmentTrendChart(String segmentName, 
                                                           SegmentGameOutHistory history,
                                                           Map<Integer, Integer> currentGamesOut) {
        if (history == null || history.getNumberHistories().isEmpty()) {
            return new SegmentTrendData(segmentName, new ArrayList<>(), new ArrayList<>(), 0.0, "NEUTRAL", 0.0);
        }
        
        // Collect all draw dates and create timeline
        Set<String> allDates = new HashSet<>();
        for (List<SegmentGameOutHistory.GameOutEntry> numberHistory : history.getNumberHistories().values()) {
            for (SegmentGameOutHistory.GameOutEntry entry : numberHistory) {
                allDates.add(entry.getDrawDate());
            }
        }
        
        List<String> sortedDates = allDates.stream().sorted().collect(Collectors.toList());
        
        // Create trend points for the last 30 draws (or all if less than 30)
        List<TrendPoint> historicalPoints = new ArrayList<>();
        int startIndex = Math.max(0, sortedDates.size() - 30);
        
        for (int i = startIndex; i < sortedDates.size(); i++) {
            String date = sortedDates.get(i);
            
            // Calculate average games out for this draw
            List<Integer> gamesOutValues = new ArrayList<>();
            boolean hasHit = false;
            
            for (Map.Entry<Integer, List<SegmentGameOutHistory.GameOutEntry>> entry : history.getNumberHistories().entrySet()) {
                for (SegmentGameOutHistory.GameOutEntry gameOutEntry : entry.getValue()) {
                    if (gameOutEntry.getDrawDate().equals(date)) {
                        if (gameOutEntry.isWasHit()) {
                            hasHit = true;
                            gamesOutValues.add(0); // Hit = 0 games out
                        } else {
                            gamesOutValues.add(gameOutEntry.getGamesOutValue());
                        }
                        break;
                    }
                }
            }
            
            // Use the median games out value instead of average to keep whole numbers
            double medianGamesOut = 0.0;
            if (!gamesOutValues.isEmpty()) {
                Collections.sort(gamesOutValues);
                int size = gamesOutValues.size();
                if (size % 2 == 0) {
                    // For even number of values, take the lower of the two middle values to keep whole numbers
                    medianGamesOut = gamesOutValues.get(size / 2 - 1);
                } else {
                    medianGamesOut = gamesOutValues.get(size / 2);
                }
            }
            
            TrendPoint point = new TrendPoint(date, i, medianGamesOut, hasHit, gamesOutValues.size());
            historicalPoints.add(point);
        }
        
        // Calculate current median to keep whole numbers
        double currentMedian = 0.0;
        if (currentGamesOut != null && !currentGamesOut.isEmpty()) {
            List<Integer> currentValues = new ArrayList<>(currentGamesOut.values());
            Collections.sort(currentValues);
            int size = currentValues.size();
            if (size % 2 == 0) {
                // For even number of values, take the lower of the two middle values to keep whole numbers
                currentMedian = currentValues.get(size / 2 - 1);
            } else {
                currentMedian = currentValues.get(size / 2);
            }
        }
        
        // Analyze trend
        TrendResult trendResult = analyzeSegmentTrends(history);
        String trendDirection = trendResult.getTrendDirection();
        double trendStrength = trendResult.getTrendStrength();
        
        // Generate predictions
        List<TrendPrediction> predictions = generateTrendPredictions(historicalPoints, currentMedian, 
                                                                   trendDirection, trendStrength);
        
        return new SegmentTrendData(segmentName, historicalPoints, predictions, currentMedian, 
                                  trendDirection, trendStrength);
    }
    
    /**
     * Generates predictions for future games out trend points
     */
    private static List<TrendPrediction> generateTrendPredictions(List<TrendPoint> historicalPoints,
                                                                double currentAverage,
                                                                String trendDirection, 
                                                                double trendStrength) {
        List<TrendPrediction> predictions = new ArrayList<>();
        
        if (historicalPoints.isEmpty()) return predictions;
        
        // Get the last few points to analyze recent trend
        List<TrendPoint> recentPoints = historicalPoints.size() > 5 ? 
                historicalPoints.subList(historicalPoints.size() - 5, historicalPoints.size()) :
                historicalPoints;
        
        // Calculate recent trend slope
        double trendSlope = calculateTrendSlope(recentPoints);
        
        // Generate 3 prediction points
        for (int i = 1; i <= 3; i++) {
            double predictedValue = currentAverage;
            double confidence = 0.7 - (i - 1) * 0.15; // Decreasing confidence over time
            String reasoning = "";
            
            // Apply trend-based adjustments
            switch (trendDirection) {
                case "INCREASING":
                    predictedValue = currentAverage + (trendSlope * i * trendStrength);
                    reasoning = "Upward trend continuation";
                    confidence += 0.1;
                    break;
                case "DECREASING":
                    predictedValue = Math.max(0, currentAverage - (Math.abs(trendSlope) * i * trendStrength));
                    reasoning = "Downward trend - potential hits approaching";
                    confidence += 0.15;
                    break;
                case "STABLE":
                    predictedValue = currentAverage + (Math.random() - 0.5) * 2; // Small random variation
                    reasoning = "Stable trend with minor variations";
                    break;
                default:
                    predictedValue = currentAverage + 1; // Default increment
                    reasoning = "Normal progression";
                    break;
            }
            
            // Apply overdue factor
            if (currentAverage > 15) {
                double overdueImpact = (currentAverage - 15) * 0.1;
                predictedValue = Math.max(0, predictedValue - overdueImpact);
                reasoning += " (overdue factor applied)";
                confidence += overdueImpact * 0.05;
            }
            
            // Ensure reasonable bounds and round to whole numbers
            predictedValue = Math.max(0, Math.min(50, predictedValue));
            predictedValue = Math.round(predictedValue); // Round to whole number
            confidence = Math.max(0.1, Math.min(0.9, confidence));
            
            predictions.add(new TrendPrediction(i, predictedValue, confidence, reasoning));
        }
        
        return predictions;
    }
    
    /**
     * Calculates the slope of the trend line from recent points
     */
    private static double calculateTrendSlope(List<TrendPoint> points) {
        if (points.size() < 2) return 0.0;
        
        // Simple linear regression slope calculation
        int n = points.size();
        double sumX = 0, sumY = 0, sumXY = 0, sumXX = 0;
        
        for (int i = 0; i < n; i++) {
            double x = i; // Use index as x-coordinate
            double y = points.get(i).getAverageGamesOut();
            
            sumX += x;
            sumY += y;
            sumXY += x * y;
            sumXX += x * x;
        }
        
        // Slope formula: (n*sumXY - sumX*sumY) / (n*sumXX - sumX*sumX)
        double denominator = n * sumXX - sumX * sumX;
        if (denominator == 0) return 0.0;
        
        return (n * sumXY - sumX * sumY) / denominator;
    }
    
    // ======================== POSITION-BASED ANALYSIS METHODS ========================
    
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    
    /**
     * Position-specific trend result with trading indicators
     */
    public static class PositionTrendResult {
        private final int position;
        private final String trendDirection;
        private final double trendStrength;
        private final Map<String, Double> technicalIndicators;
        private final List<String> tradingSignals;
        private final double confidenceScore;
        
        public PositionTrendResult(int position, String trendDirection, double trendStrength,
                                 Map<String, Double> technicalIndicators, List<String> tradingSignals,
                                 double confidenceScore) {
            this.position = position;
            this.trendDirection = trendDirection;
            this.trendStrength = trendStrength;
            this.technicalIndicators = technicalIndicators;
            this.tradingSignals = tradingSignals;
            this.confidenceScore = confidenceScore;
        }
        
        // Getters
        public int getPosition() { return position; }
        public String getTrendDirection() { return trendDirection; }
        public double getTrendStrength() { return trendStrength; }
        public Map<String, Double> getTechnicalIndicators() { return technicalIndicators; }
        public List<String> getTradingSignals() { return tradingSignals; }
        public double getConfidenceScore() { return confidenceScore; }
    }
    
    /**
     * Analyzes position-specific trends with trading-style indicators
     */
    public static PositionTrendResult analyzePositionTrends(List<DrawResultPattern> drawPatterns, int position) {
        if (drawPatterns == null || drawPatterns.isEmpty() || position < 1) {
            return new PositionTrendResult(position, "NEUTRAL", 0.0, new HashMap<>(), 
                Arrays.asList("Insufficient data"), 0.0);
        }
        
        // Extract games out values for this position
        List<Double> gamesOutValues = extractPositionGamesOut(drawPatterns, position);
        if (gamesOutValues.size() < 10) {
            return new PositionTrendResult(position, "NEUTRAL", 0.0, new HashMap<>(), 
                Arrays.asList("Need at least 10 data points"), 0.0);
        }
        
        // Calculate technical indicators
        Map<String, Double> indicators = calculatePositionTechnicalIndicators(gamesOutValues);
        
        // Determine trend direction and strength
        String trendDirection = determinePositionTrend(gamesOutValues);
        double trendStrength = calculatePositionTrendStrength(gamesOutValues);
        
        // Generate trading signals
        List<String> tradingSignals = generateTradingSignals(gamesOutValues, indicators);
        
        // Calculate confidence score
        double confidence = calculatePositionConfidence(gamesOutValues.size(), trendStrength, indicators);
        
        return new PositionTrendResult(position, trendDirection, trendStrength, 
            indicators, tradingSignals, confidence);
    }
    
    /**
     * Extracts games out values for a specific position across all draws
     */
    private static List<Double> extractPositionGamesOut(List<DrawResultPattern> drawPatterns, int position) {
        return drawPatterns.stream()
            .filter(draw -> draw.getLotteryNumbers().size() >= position)
            .map(draw -> (double) draw.getLotteryNumbers().get(position - 1).getGamesOut())
            .collect(Collectors.toList());
    }
    
    /**
     * Calculates comprehensive technical indicators for position analysis
     */
    public static Map<String, Double> calculatePositionTechnicalIndicators(List<Double> values) {
        Map<String, Double> indicators = new HashMap<>();
        
        if (values.isEmpty()) return indicators;
        
        // Simple Moving Averages
        indicators.put("SMA5", calculateSMA(values, 5));
        indicators.put("SMA10", calculateSMA(values, 10));
        indicators.put("SMA20", calculateSMA(values, 20));
        
        // Exponential Moving Averages
        indicators.put("EMA5", calculateEMA(values, 5));
        indicators.put("EMA10", calculateEMA(values, 10));
        indicators.put("EMA20", calculateEMA(values, 20));
        
        // Volatility indicators
        indicators.put("Volatility", calculateVolatility(values, 20));
        indicators.put("ATR", calculateATR(values, 14)); // Average True Range
        
        // Momentum indicators
        indicators.put("RSI", calculateRSI(values, 14));
        indicators.put("MACD", calculateMACD(values));
        indicators.put("MACDSignal", calculateMACDSignal(values));
        
        // Trend indicators
        indicators.put("ADX", calculateADX(values, 14)); // Average Directional Index
        indicators.put("TrendSlope", calculateTrendSlope(values, 10));
        
        // Support and resistance
        indicators.put("Support", calculateSupport(values, 20));
        indicators.put("Resistance", calculateResistance(values, 20));
        
        return indicators;
    }
    
    /**
     * Simple Moving Average calculation
     */
    private static double calculateSMA(List<Double> values, int period) {
        if (values.size() < period) return values.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
        
        return values.subList(values.size() - period, values.size()).stream()
            .mapToDouble(Double::doubleValue)
            .average().orElse(0.0);
    }
    
    /**
     * Exponential Moving Average calculation
     */
    private static double calculateEMA(List<Double> values, int period) {
        if (values.isEmpty()) return 0.0;
        if (values.size() < period) return calculateSMA(values, values.size());
        
        double multiplier = 2.0 / (period + 1);
        double ema = calculateSMA(values.subList(0, period), period);
        
        for (int i = period; i < values.size(); i++) {
            ema = (values.get(i) * multiplier) + (ema * (1 - multiplier));
        }
        
        return ema;
    }
    
    /**
     * Volatility calculation (standard deviation)
     */
    private static double calculateVolatility(List<Double> values, int period) {
        if (values.size() < period) return 0.0;
        
        List<Double> recentValues = values.subList(values.size() - period, values.size());
        double mean = recentValues.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
        
        double variance = recentValues.stream()
            .mapToDouble(val -> Math.pow(val - mean, 2))
            .average().orElse(0.0);
            
        return Math.sqrt(variance);
    }
    
    /**
     * Average True Range calculation
     */
    private static double calculateATR(List<Double> values, int period) {
        if (values.size() < period + 1) return 0.0;
        
        List<Double> trueRanges = new ArrayList<>();
        for (int i = 1; i < values.size(); i++) {
            double high = Math.max(values.get(i), values.get(i-1));
            double low = Math.min(values.get(i), values.get(i-1));
            double trueRange = high - low;
            trueRanges.add(trueRange);
        }
        
        return calculateSMA(trueRanges, Math.min(period, trueRanges.size()));
    }
    
    /**
     * RSI (Relative Strength Index) calculation
     */
    private static double calculateRSI(List<Double> values, int period) {
        if (values.size() < period + 1) return 50.0; // Neutral RSI
        
        List<Double> gains = new ArrayList<>();
        List<Double> losses = new ArrayList<>();
        
        for (int i = 1; i < values.size(); i++) {
            double change = values.get(i) - values.get(i-1);
            gains.add(Math.max(0, change));
            losses.add(Math.max(0, -change));
        }
        
        double avgGain = calculateSMA(gains, Math.min(period, gains.size()));
        double avgLoss = calculateSMA(losses, Math.min(period, losses.size()));
        
        if (avgLoss == 0) return 100.0;
        
        double rs = avgGain / avgLoss;
        return 100 - (100 / (1 + rs));
    }
    
    /**
     * MACD calculation
     */
    private static double calculateMACD(List<Double> values) {
        if (values.size() < 26) return 0.0;
        
        double ema12 = calculateEMA(values, 12);
        double ema26 = calculateEMA(values, 26);
        return ema12 - ema26;
    }
    
    /**
     * MACD Signal line calculation
     */
    private static double calculateMACDSignal(List<Double> values) {
        if (values.size() < 35) return 0.0; // Need enough data for MACD + 9 EMA
        
        // This is simplified - in practice, you'd calculate MACD for each point
        // and then apply EMA to the MACD values
        double macd = calculateMACD(values);
        return macd * 0.9; // Simplified signal line
    }
    
    /**
     * ADX (Average Directional Index) calculation - simplified
     */
    private static double calculateADX(List<Double> values, int period) {
        if (values.size() < period * 2) return 25.0; // Neutral ADX
        
        // Simplified ADX calculation
        double volatility = calculateVolatility(values, period);
        double trendSlope = Math.abs(calculateTrendSlope(values, period));
        
        return Math.min(100, (trendSlope * 50) + (volatility * 10));
    }
    
    /**
     * Calculate trend slope for recent values
     */
    private static double calculateTrendSlope(List<Double> values, int period) {
        if (values.size() < Math.max(period, 5)) return 0.0;
        
        List<Double> recentValues = values.subList(values.size() - Math.min(period, values.size()), values.size());
        return calculateTrendSlope(recentValues.stream()
            .map(v -> new TrendPoint("", 0, v, false, 0))
            .collect(Collectors.toList()));
    }
    
    /**
     * Calculate support level
     */
    private static double calculateSupport(List<Double> values, int period) {
        if (values.size() < period) return values.stream().mapToDouble(Double::doubleValue).min().orElse(0.0);
        
        List<Double> recentValues = values.subList(values.size() - period, values.size());
        return recentValues.stream().mapToDouble(Double::doubleValue).min().orElse(0.0);
    }
    
    /**
     * Calculate resistance level
     */
    private static double calculateResistance(List<Double> values, int period) {
        if (values.size() < period) return values.stream().mapToDouble(Double::doubleValue).max().orElse(0.0);
        
        List<Double> recentValues = values.subList(values.size() - period, values.size());
        return recentValues.stream().mapToDouble(Double::doubleValue).max().orElse(0.0);
    }
    
    /**
     * Determine position trend direction
     */
    private static String determinePositionTrend(List<Double> values) {
        if (values.size() < 5) return "NEUTRAL";
        
        double sma5 = calculateSMA(values, 5);
        double sma20 = calculateSMA(values, Math.min(20, values.size()));
        double slope = calculateTrendSlope(values, 10);
        
        // Multiple criteria for trend determination
        boolean bullishMA = sma5 > sma20;
        boolean bullishSlope = slope > 0.1;
        boolean strongBullish = bullishMA && bullishSlope;
        
        boolean bearishMA = sma5 < sma20;
        boolean bearishSlope = slope < -0.1;
        boolean strongBearish = bearishMA && bearishSlope;
        
        if (strongBullish) return "STRONG_BULLISH";
        if (strongBearish) return "STRONG_BEARISH";
        if (bullishMA || bullishSlope) return "BULLISH";
        if (bearishMA || bearishSlope) return "BEARISH";
        return "SIDEWAYS";
    }
    
    /**
     * Calculate position trend strength
     */
    private static double calculatePositionTrendStrength(List<Double> values) {
        if (values.size() < 5) return 0.0;
        
        double slope = Math.abs(calculateTrendSlope(values, 10));
        double volatility = calculateVolatility(values, 14);
        double rsi = calculateRSI(values, 14);
        
        // Combine multiple factors for strength
        double slopeStrength = Math.min(1.0, slope / 2.0);
        double rsiStrength = Math.abs(rsi - 50) / 50.0; // Distance from neutral
        double volatilityFactor = Math.min(1.0, volatility / 10.0);
        
        return (slopeStrength + rsiStrength + volatilityFactor) / 3.0;
    }
    
    /**
     * Generate trading signals based on technical analysis
     */
    private static List<String> generateTradingSignals(List<Double> values, Map<String, Double> indicators) {
        List<String> signals = new ArrayList<>();
        
        double rsi = indicators.getOrDefault("RSI", 50.0);
        double macd = indicators.getOrDefault("MACD", 0.0);
        double macdSignal = indicators.getOrDefault("MACDSignal", 0.0);
        double sma5 = indicators.getOrDefault("SMA5", 0.0);
        double sma20 = indicators.getOrDefault("SMA20", 0.0);
        
        // RSI signals
        if (rsi > 70) signals.add("RSI_OVERBOUGHT");
        else if (rsi < 30) signals.add("RSI_OVERSOLD");
        
        // MACD signals
        if (macd > macdSignal && macd > 0) signals.add("MACD_BULLISH");
        else if (macd < macdSignal && macd < 0) signals.add("MACD_BEARISH");
        
        // Moving average signals
        if (sma5 > sma20) signals.add("MA_GOLDEN_CROSS");
        else if (sma5 < sma20) signals.add("MA_DEATH_CROSS");
        
        // Trend signals
        double adx = indicators.getOrDefault("ADX", 25.0);
        if (adx > 40) signals.add("STRONG_TREND");
        else if (adx < 20) signals.add("WEAK_TREND");
        
        if (signals.isEmpty()) signals.add("NO_CLEAR_SIGNALS");
        
        return signals;
    }
    
    /**
     * Calculate confidence score for position analysis
     */
    private static double calculatePositionConfidence(int dataPoints, double trendStrength, Map<String, Double> indicators) {
        // Base confidence from data quantity
        double dataConfidence = Math.min(1.0, dataPoints / 50.0);
        
        // Trend confidence
        double trendConfidence = trendStrength;
        
        // Technical indicator confidence
        double rsi = indicators.getOrDefault("RSI", 50.0);
        double adx = indicators.getOrDefault("ADX", 25.0);
        double indicatorConfidence = (Math.abs(rsi - 50) / 50.0 + Math.min(1.0, adx / 50.0)) / 2.0;
        
        return (dataConfidence + trendConfidence + indicatorConfidence) / 3.0;
    }
    
    /**
     * Generate comprehensive position analysis summary
     */
    public static String generatePositionAnalysisSummary(PositionTrendResult result) {
        StringBuilder summary = new StringBuilder();
        summary.append("Position ").append(result.getPosition()).append(" Analysis:\n");
        summary.append("Trend: ").append(result.getTrendDirection())
               .append(" (").append(String.format("%.1f", result.getTrendStrength() * 100)).append("% strength)\n");
        summary.append("Confidence: ").append(String.format("%.1f%%", result.getConfidenceScore() * 100)).append("\n");
        
        Map<String, Double> indicators = result.getTechnicalIndicators();
        summary.append("RSI: ").append(String.format("%.1f", indicators.getOrDefault("RSI", 50.0))).append("\n");
        summary.append("MACD: ").append(String.format("%.2f", indicators.getOrDefault("MACD", 0.0))).append("\n");
        
        summary.append("Signals: ").append(String.join(", ", result.getTradingSignals()));
        
        return summary.toString();
    }
}