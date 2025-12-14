package com.example.lottooptionspro.model.range;

import java.util.List;
import java.util.Map;

public class RangeAnalysisResponse {
    private GameConfiguration gameConfiguration;
    private List<String> rangeHeaders;
    private int totalDrawsAnalyzed;
    private List<DrawResult> drawResults;
    private Map<String, PerformanceMetrics> performanceMetrics;
    private Map<String, PositionAnalysis> positionAnalysis;

    public RangeAnalysisResponse() {
    }

    public RangeAnalysisResponse(GameConfiguration gameConfiguration, List<String> rangeHeaders,
                                int totalDrawsAnalyzed, List<DrawResult> drawResults,
                                Map<String, PerformanceMetrics> performanceMetrics,
                                Map<String, PositionAnalysis> positionAnalysis) {
        this.gameConfiguration = gameConfiguration;
        this.rangeHeaders = rangeHeaders;
        this.totalDrawsAnalyzed = totalDrawsAnalyzed;
        this.drawResults = drawResults;
        this.performanceMetrics = performanceMetrics;
        this.positionAnalysis = positionAnalysis;
    }

    public GameConfiguration getGameConfiguration() {
        return gameConfiguration;
    }

    public void setGameConfiguration(GameConfiguration gameConfiguration) {
        this.gameConfiguration = gameConfiguration;
    }

    public List<String> getRangeHeaders() {
        return rangeHeaders;
    }

    public void setRangeHeaders(List<String> rangeHeaders) {
        this.rangeHeaders = rangeHeaders;
    }

    public int getTotalDrawsAnalyzed() {
        return totalDrawsAnalyzed;
    }

    public void setTotalDrawsAnalyzed(int totalDrawsAnalyzed) {
        this.totalDrawsAnalyzed = totalDrawsAnalyzed;
    }

    public List<DrawResult> getDrawResults() {
        return drawResults;
    }

    public void setDrawResults(List<DrawResult> drawResults) {
        this.drawResults = drawResults;
    }

    public Map<String, PerformanceMetrics> getPerformanceMetrics() {
        return performanceMetrics;
    }

    public void setPerformanceMetrics(Map<String, PerformanceMetrics> performanceMetrics) {
        this.performanceMetrics = performanceMetrics;
    }

    public Map<String, PositionAnalysis> getPositionAnalysis() {
        return positionAnalysis;
    }

    public void setPositionAnalysis(Map<String, PositionAnalysis> positionAnalysis) {
        this.positionAnalysis = positionAnalysis;
    }

    public boolean hasData() {
        return drawResults != null && !drawResults.isEmpty();
    }

    public boolean hasPerformanceMetrics() {
        return performanceMetrics != null && !performanceMetrics.isEmpty();
    }

    public boolean hasPositionAnalysis() {
        return positionAnalysis != null && !positionAnalysis.isEmpty();
    }

    public PerformanceMetrics getPerformanceMetricsForRange(String range) {
        return performanceMetrics != null ? performanceMetrics.get(range) : null;
    }

    public PositionAnalysis getPositionAnalysisForPosition(String position) {
        return positionAnalysis != null ? positionAnalysis.get(position) : null;
    }

    public static class GameConfiguration {
        private String state;
        private String game;
        private String numberRange;
        private int totalNumbers;
        private AnalysisType analysisType;
        private int rangeSize;
        private List<Integer> analyzedPositions;

        public GameConfiguration() {
        }

        public GameConfiguration(String state, String game, String numberRange, int totalNumbers,
                                AnalysisType analysisType, int rangeSize, List<Integer> analyzedPositions) {
            this.state = state;
            this.game = game;
            this.numberRange = numberRange;
            this.totalNumbers = totalNumbers;
            this.analysisType = analysisType;
            this.rangeSize = rangeSize;
            this.analyzedPositions = analyzedPositions;
        }

        public String getState() {
            return state;
        }

        public void setState(String state) {
            this.state = state;
        }

        public String getGame() {
            return game;
        }

        public void setGame(String game) {
            this.game = game;
        }

        public String getNumberRange() {
            return numberRange;
        }

        public void setNumberRange(String numberRange) {
            this.numberRange = numberRange;
        }

        public int getTotalNumbers() {
            return totalNumbers;
        }

        public void setTotalNumbers(int totalNumbers) {
            this.totalNumbers = totalNumbers;
        }

        public AnalysisType getAnalysisType() {
            return analysisType;
        }

        public void setAnalysisType(AnalysisType analysisType) {
            this.analysisType = analysisType;
        }

        public int getRangeSize() {
            return rangeSize;
        }

        public void setRangeSize(int rangeSize) {
            this.rangeSize = rangeSize;
        }

        public List<Integer> getAnalyzedPositions() {
            return analyzedPositions;
        }

        public void setAnalyzedPositions(List<Integer> analyzedPositions) {
            this.analyzedPositions = analyzedPositions;
        }
    }
}