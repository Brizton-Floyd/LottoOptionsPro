package com.example.lottooptionspro.component;

import com.example.lottooptionspro.model.dashboard.PositionTrendData;
import com.example.lottooptionspro.model.dashboard.PositionTrendPoint;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.chart.*;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Line;
import javafx.scene.shape.Rectangle;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TradingStyleChart extends VBox {
    
    private static final String BULLISH_COLOR = "#4CAF50";
    private static final String BEARISH_COLOR = "#F44336";
    private static final String NEUTRAL_COLOR = "#9E9E9E";
    private static final String VOLUME_COLOR = "#2196F3";
    private static final String MA_COLOR_5 = "#FF9800";
    private static final String MA_COLOR_10 = "#9C27B0";
    private static final String MA_COLOR_20 = "#00BCD4";
    
    private PositionTrendData currentTrendData;
    private LineChart<String, Number> mainChart;
    private BarChart<String, Number> volumeChart;
    private Label titleLabel;
    private Label summaryLabel;
    private HBox indicatorPanel;
    
    public TradingStyleChart() {
        this.getStyleClass().add("trading-chart");
        initializeChart();
    }
    
    private void initializeChart() {
        // Create title section
        VBox titleSection = createTitleSection();
        
        // Create main OHLC chart with technical indicators
        StackPane mainChartContainer = createMainChart();
        
        // Create volume chart
        StackPane volumeChartContainer = createVolumeChart();
        
        // Create indicator panel
        HBox indicatorPanelContainer = createIndicatorPanel();
        
        // Add all sections to main container
        this.getChildren().addAll(
            titleSection,
            mainChartContainer,
            volumeChartContainer,
            indicatorPanelContainer
        );
        
        // Set sizing constraints
        VBox.setVgrow(mainChartContainer, Priority.ALWAYS);
        this.setSpacing(5);
        this.setPadding(new Insets(10));
    }
    
    private VBox createTitleSection() {
        VBox titleSection = new VBox(5);
        titleSection.setAlignment(Pos.CENTER_LEFT);
        
        titleLabel = new Label("Position Trend Analysis");
        titleLabel.getStyleClass().add("trading-chart-title");
        
        summaryLabel = new Label("Select a position to view trend analysis");
        summaryLabel.getStyleClass().add("trading-chart-summary");
        
        titleSection.getChildren().addAll(titleLabel, summaryLabel);
        return titleSection;
    }
    
    private StackPane createMainChart() {
        // Create axes
        CategoryAxis xAxis = new CategoryAxis();
        xAxis.setLabel("Time Period");
        xAxis.setTickLabelRotation(45);
        
        NumberAxis yAxis = new NumberAxis();
        yAxis.setLabel("Games Out Value");
        yAxis.setAutoRanging(true);
        
        // Create line chart for OHLC visualization
        mainChart = new LineChart<>(xAxis, yAxis);
        mainChart.setTitle("Games Out Trend with Technical Indicators");
        mainChart.setLegendVisible(true);
        mainChart.setPrefHeight(400);
        mainChart.setMaxHeight(400);
        
        // Style the chart
        mainChart.getStyleClass().add("trading-main-chart");
        
        StackPane container = new StackPane(mainChart);
        container.getStyleClass().add("chart-container");
        
        return container;
    }
    
    private StackPane createVolumeChart() {
        // Create axes for volume chart
        CategoryAxis volumeXAxis = new CategoryAxis();
        volumeXAxis.setLabel("Time Period");
        volumeXAxis.setTickLabelRotation(45);
        
        NumberAxis volumeYAxis = new NumberAxis();
        volumeYAxis.setLabel("Hit Frequency");
        volumeYAxis.setAutoRanging(true);
        
        // Create volume bar chart
        volumeChart = new BarChart<>(volumeXAxis, volumeYAxis);
        volumeChart.setTitle("Hit Frequency (Volume)");
        volumeChart.setLegendVisible(false);
        volumeChart.setPrefHeight(150);
        volumeChart.setMaxHeight(150);
        volumeChart.setCategoryGap(2);
        volumeChart.setBarGap(1);
        
        volumeChart.getStyleClass().add("trading-volume-chart");
        
        StackPane container = new StackPane(volumeChart);
        container.getStyleClass().add("chart-container");
        
        return container;
    }
    
    private HBox createIndicatorPanel() {
        indicatorPanel = new HBox(15);
        indicatorPanel.setAlignment(Pos.CENTER_LEFT);
        indicatorPanel.setPadding(new Insets(10, 0, 0, 0));
        indicatorPanel.getStyleClass().add("indicator-panel");
        
        return indicatorPanel;
    }
    
    public void updateChart(PositionTrendData trendData) {
        if (trendData == null || !trendData.hasValidData()) {
            clearChart();
            return;
        }
        
        // Show loading state briefly for better UX
        Platform.runLater(() -> {
            this.setDisable(true);
            this.setOpacity(0.7);
        });
        
        // Update chart data
        this.currentTrendData = trendData;
        updateTitle(trendData);
        updateMainChart(trendData);
        updateVolumeChart(trendData);
        updateIndicatorPanel(trendData);
        
        // Remove loading state
        Platform.runLater(() -> {
            this.setDisable(false);
            this.setOpacity(1.0);
        });
    }
    
    private void updateTitle(PositionTrendData trendData) {
        titleLabel.setText("Position " + trendData.getPosition() + " - Trading Analysis");
        
        if (trendData.getAnalysisResult() != null) {
            String summary = String.format("%s Trend (%.1f%% strength) | %s | Confidence: %.1f%%",
                trendData.getAnalysisResult().getPrimaryTrend(),
                trendData.getAnalysisResult().getTrendStrength() * 100,
                trendData.getTimeRange(),
                trendData.getAnalysisResult().getConfidenceScore() * 100);
            summaryLabel.setText(summary);
        }
    }
    
    private void updateMainChart(PositionTrendData trendData) {
        mainChart.getData().clear();
        
        List<PositionTrendPoint> points = trendData.getTrendPoints();
        if (points.isEmpty()) return;
        
        // Create OHLC series (using close values as main line)
        XYChart.Series<String, Number> ohlcSeries = new XYChart.Series<>();
        ohlcSeries.setName("Games Out (Close)");
        
        // Create moving average series
        XYChart.Series<String, Number> ma5Series = new XYChart.Series<>();
        ma5Series.setName("MA5");
        
        XYChart.Series<String, Number> ma10Series = new XYChart.Series<>();
        ma10Series.setName("MA10");
        
        XYChart.Series<String, Number> ma20Series = new XYChart.Series<>();
        ma20Series.setName("MA20");
        
        // Populate data points
        for (int i = 0; i < points.size(); i++) {
            PositionTrendPoint point = points.get(i);
            String timeLabel = "T" + (i + 1); // Time period label
            
            // Add OHLC close value
            XYChart.Data<String, Number> closeData = new XYChart.Data<>(timeLabel, point.getClose());
            ohlcSeries.getData().add(closeData);
            
            // Add moving averages if available
            Map<String, Double> ma = point.getMovingAverages();
            if (ma.containsKey("MA5")) {
                ma5Series.getData().add(new XYChart.Data<>(timeLabel, ma.get("MA5")));
            }
            if (ma.containsKey("MA10")) {
                ma10Series.getData().add(new XYChart.Data<>(timeLabel, ma.get("MA10")));
            }
            if (ma.containsKey("MA20")) {
                ma20Series.getData().add(new XYChart.Data<>(timeLabel, ma.get("MA20")));
            }
        }
        
        // Add series to chart
        mainChart.getData().addAll(ohlcSeries, ma5Series, ma10Series, ma20Series);
        
        // Style the series after they're added
        styleMainChartSeries(points);
        
        // Add OHLC visual indicators
        addOHLCVisualElements(points);
    }
    
    private void styleMainChartSeries(List<PositionTrendPoint> points) {
        // Style main OHLC series
        if (!mainChart.getData().isEmpty()) {
            XYChart.Series<String, Number> ohlcSeries = mainChart.getData().get(0);
            Node seriesNode = ohlcSeries.getNode();
            if (seriesNode != null) {
                seriesNode.setStyle("-fx-stroke: " + NEUTRAL_COLOR + "; -fx-stroke-width: 2px;");
            }
            
            // Style individual data points based on bullish/bearish
            for (int i = 0; i < ohlcSeries.getData().size() && i < points.size(); i++) {
                XYChart.Data<String, Number> data = ohlcSeries.getData().get(i);
                PositionTrendPoint point = points.get(i);
                Node node = data.getNode();
                
                if (node != null) {
                    String color = point.isBullish() ? BULLISH_COLOR : 
                                  point.isBearish() ? BEARISH_COLOR : NEUTRAL_COLOR;
                    node.setStyle("-fx-background-color: " + color + "; -fx-background-radius: 3px;");
                    
                    // Add tooltip with OHLC data
                    Tooltip tooltip = createOHLCTooltip(point);
                    Tooltip.install(node, tooltip);
                }
            }
        }
        
        // Style moving average series
        if (mainChart.getData().size() > 1) {
            styleSeriesLine(mainChart.getData().get(1), MA_COLOR_5, 1); // MA5
        }
        if (mainChart.getData().size() > 2) {
            styleSeriesLine(mainChart.getData().get(2), MA_COLOR_10, 1); // MA10
        }
        if (mainChart.getData().size() > 3) {
            styleSeriesLine(mainChart.getData().get(3), MA_COLOR_20, 2); // MA20
        }
    }
    
    private void styleSeriesLine(XYChart.Series<String, Number> series, String color, int width) {
        Node seriesNode = series.getNode();
        if (seriesNode != null) {
            seriesNode.setStyle("-fx-stroke: " + color + "; -fx-stroke-width: " + width + "px; -fx-stroke-dash-array: 5 5;");
        }
        
        // Hide data point symbols for MA lines
        for (XYChart.Data<String, Number> data : series.getData()) {
            Node node = data.getNode();
            if (node != null) {
                node.setVisible(false);
            }
        }
    }
    
    private void addOHLCVisualElements(List<PositionTrendPoint> points) {
        // This would add candlestick or OHLC bar visual elements
        // For now, we'll use the styled line chart with colored points
        // In a full implementation, you'd overlay custom shapes for true OHLC bars
    }
    
    private Tooltip createOHLCTooltip(PositionTrendPoint point) {
        StringBuilder tooltipText = new StringBuilder();
        tooltipText.append("OHLC Data:\n");
        tooltipText.append("Open: ").append(String.format("%.1f", point.getOpen())).append("\n");
        tooltipText.append("High: ").append(String.format("%.1f", point.getHigh())).append("\n");
        tooltipText.append("Low: ").append(String.format("%.1f", point.getLow())).append("\n");
        tooltipText.append("Close: ").append(String.format("%.1f", point.getClose())).append("\n");
        tooltipText.append("Hit Count: ").append(point.getHitCount()).append("\n");
        tooltipText.append("Hit Frequency: ").append(String.format("%.1f%%", point.getHitFrequency() * 100));
        
        if (!point.getTrendDirection().equals("NEUTRAL")) {
            tooltipText.append("\nTrend: ").append(point.getTrendDirection());
        }
        
        return new Tooltip(tooltipText.toString());
    }
    
    private void updateVolumeChart(PositionTrendData trendData) {
        volumeChart.getData().clear();
        
        List<PositionTrendPoint> points = trendData.getTrendPoints();
        if (points.isEmpty()) return;
        
        XYChart.Series<String, Number> volumeSeries = new XYChart.Series<>();
        volumeSeries.setName("Hit Frequency");
        
        for (int i = 0; i < points.size(); i++) {
            PositionTrendPoint point = points.get(i);
            String timeLabel = "T" + (i + 1);
            double frequency = point.getHitFrequency() * 100; // Convert to percentage
            
            XYChart.Data<String, Number> volumeData = new XYChart.Data<>(timeLabel, frequency);
            volumeSeries.getData().add(volumeData);
        }
        
        volumeChart.getData().add(volumeSeries);
        
        // Style volume bars
        styleVolumeChart(points);
    }
    
    private void styleVolumeChart(List<PositionTrendPoint> points) {
        if (volumeChart.getData().isEmpty()) return;
        
        XYChart.Series<String, Number> volumeSeries = volumeChart.getData().get(0);
        
        for (int i = 0; i < volumeSeries.getData().size() && i < points.size(); i++) {
            XYChart.Data<String, Number> data = volumeSeries.getData().get(i);
            PositionTrendPoint point = points.get(i);
            Node node = data.getNode();
            
            if (node != null) {
                // Color volume bars based on hit activity
                String color = point.getHitCount() > 0 ? BULLISH_COLOR : VOLUME_COLOR;
                double opacity = Math.max(0.3, point.getHitFrequency());
                
                node.setStyle("-fx-bar-fill: " + color + "; -fx-opacity: " + opacity + ";");
                
                // Add volume tooltip
                Tooltip tooltip = new Tooltip(String.format("Period T%d\nHit Frequency: %.1f%%\nHit Count: %d",
                    i + 1, point.getHitFrequency() * 100, point.getHitCount()));
                Tooltip.install(node, tooltip);
            }
        }
    }
    
    private void updateIndicatorPanel(PositionTrendData trendData) {
        indicatorPanel.getChildren().clear();
        
        if (trendData.getAnalysisResult() == null) return;
        
        Map<String, Double> indicators = trendData.getTechnicalIndicators();
        
        // RSI Indicator
        if (indicators.containsKey("RSI")) {
            Label rsiLabel = createIndicatorLabel("RSI", indicators.get("RSI"), 0, 100);
            indicatorPanel.getChildren().add(rsiLabel);
        }
        
        // MACD Indicator
        if (indicators.containsKey("MACD")) {
            Label macdLabel = createIndicatorLabel("MACD", indicators.get("MACD"), -10, 10);
            indicatorPanel.getChildren().add(macdLabel);
        }
        
        // ADX Indicator
        if (indicators.containsKey("ADX")) {
            Label adxLabel = createIndicatorLabel("ADX", indicators.get("ADX"), 0, 100);
            indicatorPanel.getChildren().add(adxLabel);
        }
        
        // Volatility Indicator
        if (indicators.containsKey("Volatility")) {
            Label volatilityLabel = createIndicatorLabel("Volatility", indicators.get("Volatility"), 0, 20);
            indicatorPanel.getChildren().add(volatilityLabel);
        }
        
        // Trading Signals
        if (!trendData.getAnalysisResult().getInsights().isEmpty()) {
            Label signalsLabel = new Label("Signals: " + String.join(", ", 
                trendData.getAnalysisResult().getInsights()));
            signalsLabel.getStyleClass().add("trading-signals");
            indicatorPanel.getChildren().add(signalsLabel);
        }
    }
    
    private Label createIndicatorLabel(String name, double value, double min, double max) {
        String formattedValue = String.format("%.1f", value);
        String color = getIndicatorColor(value, min, max);
        
        Label label = new Label(name + ": " + formattedValue);
        label.getStyleClass().add("technical-indicator");
        label.setStyle("-fx-text-fill: " + color + ";");
        
        return label;
    }
    
    private String getIndicatorColor(double value, double min, double max) {
        double range = max - min;
        double normalized = (value - min) / range;
        
        if (normalized > 0.7) return BEARISH_COLOR;    // High values (overbought/high volatility)
        if (normalized < 0.3) return BULLISH_COLOR;    // Low values (oversold/low volatility)
        return NEUTRAL_COLOR;                          // Neutral range
    }
    
    private void clearChart() {
        if (mainChart != null) mainChart.getData().clear();
        if (volumeChart != null) volumeChart.getData().clear();
        if (indicatorPanel != null) indicatorPanel.getChildren().clear();
        
        titleLabel.setText("Position Trend Analysis");
        summaryLabel.setText("No data available");
    }
    
    public PositionTrendData getCurrentTrendData() {
        return currentTrendData;
    }
    
    public void setChartTitle(String title) {
        if (titleLabel != null) {
            titleLabel.setText(title);
        }
    }
    
    public void refreshChart() {
        if (currentTrendData != null) {
            updateChart(currentTrendData);
        }
    }
}