package com.example.lottooptionspro.component;

import com.example.lottooptionspro.model.dashboard.PositionTrendData;
import com.example.lottooptionspro.model.dashboard.PositionTrendPoint;
import javafx.scene.chart.*;
import javafx.scene.control.Tooltip;
import javafx.scene.Node;
import javafx.scene.paint.Color;

import java.util.List;
import java.util.Map;

public class TradingChartFactory {
    
    public enum ChartType {
        LINE_CHART,
        BAR_CHART,
        AREA_CHART,
        CANDLESTICK_CHART
    }
    
    private static final String BULLISH_COLOR = "#4CAF50";
    private static final String BEARISH_COLOR = "#F44336";
    private static final String NEUTRAL_COLOR = "#9E9E9E";
    
    public static Chart createPositionChart(ChartType chartType, PositionTrendData trendData) {
        switch (chartType) {
            case LINE_CHART:
                return createLineChart(trendData);
            case BAR_CHART:
                return createBarChart(trendData);
            case AREA_CHART:
                return createAreaChart(trendData);
            case CANDLESTICK_CHART:
                return createCandlestickChart(trendData);
            default:
                return createLineChart(trendData);
        }
    }
    
    private static LineChart<String, Number> createLineChart(PositionTrendData trendData) {
        CategoryAxis xAxis = new CategoryAxis();
        xAxis.setLabel("Time Period");
        
        NumberAxis yAxis = new NumberAxis();
        yAxis.setLabel("Games Out Value");
        yAxis.setAutoRanging(true);
        
        LineChart<String, Number> chart = new LineChart<>(xAxis, yAxis);
        chart.setTitle("Position " + trendData.getPosition() + " - Trend Analysis");
        chart.setCreateSymbols(true);
        chart.setLegendVisible(true);
        
        if (trendData.hasValidData()) {
            populateLineChart(chart, trendData);
        }
        
        return chart;
    }
    
    private static BarChart<String, Number> createBarChart(PositionTrendData trendData) {
        CategoryAxis xAxis = new CategoryAxis();
        xAxis.setLabel("Time Period");
        
        NumberAxis yAxis = new NumberAxis();
        yAxis.setLabel("Games Out Value");
        yAxis.setAutoRanging(true);
        
        BarChart<String, Number> chart = new BarChart<>(xAxis, yAxis);
        chart.setTitle("Position " + trendData.getPosition() + " - Bar Analysis");
        chart.setLegendVisible(true);
        chart.setCategoryGap(5);
        chart.setBarGap(2);
        
        if (trendData.hasValidData()) {
            populateBarChart(chart, trendData);
        }
        
        return chart;
    }
    
    private static AreaChart<String, Number> createAreaChart(PositionTrendData trendData) {
        CategoryAxis xAxis = new CategoryAxis();
        xAxis.setLabel("Time Period");
        
        NumberAxis yAxis = new NumberAxis();
        yAxis.setLabel("Games Out Value");
        yAxis.setAutoRanging(true);
        
        AreaChart<String, Number> chart = new AreaChart<>(xAxis, yAxis);
        chart.setTitle("Position " + trendData.getPosition() + " - Area Analysis");
        chart.setCreateSymbols(false);
        chart.setLegendVisible(true);
        
        if (trendData.hasValidData()) {
            populateAreaChart(chart, trendData);
        }
        
        return chart;
    }
    
    private static Chart createCandlestickChart(PositionTrendData trendData) {
        // For now, create a specialized bar chart that simulates candlesticks
        // In a full implementation, you'd create a custom candlestick chart
        return createOHLCBarChart(trendData);
    }
    
    private static BarChart<String, Number> createOHLCBarChart(PositionTrendData trendData) {
        CategoryAxis xAxis = new CategoryAxis();
        xAxis.setLabel("Time Period");
        
        NumberAxis yAxis = new NumberAxis();
        yAxis.setLabel("Games Out Value");
        yAxis.setAutoRanging(true);
        
        BarChart<String, Number> chart = new BarChart<>(xAxis, yAxis);
        chart.setTitle("Position " + trendData.getPosition() + " - OHLC Analysis");
        chart.setLegendVisible(true);
        chart.setCategoryGap(8);
        chart.setBarGap(1);
        
        if (trendData.hasValidData()) {
            populateOHLCChart(chart, trendData);
        }
        
        return chart;
    }
    
    private static void populateLineChart(LineChart<String, Number> chart, PositionTrendData trendData) {
        List<PositionTrendPoint> points = trendData.getTrendPoints();
        
        // Main close line
        XYChart.Series<String, Number> closeSeries = new XYChart.Series<>();
        closeSeries.setName("Games Out (Close)");
        
        // Moving averages
        XYChart.Series<String, Number> ma5Series = new XYChart.Series<>();
        ma5Series.setName("MA5");
        
        XYChart.Series<String, Number> ma10Series = new XYChart.Series<>();
        ma10Series.setName("MA10");
        
        for (int i = 0; i < points.size(); i++) {
            PositionTrendPoint point = points.get(i);
            String timeLabel = "T" + (i + 1);
            
            // Close value
            XYChart.Data<String, Number> closeData = new XYChart.Data<>(timeLabel, point.getClose());
            closeSeries.getData().add(closeData);
            
            // Moving averages
            Map<String, Double> ma = point.getMovingAverages();
            if (ma.containsKey("MA5")) {
                ma5Series.getData().add(new XYChart.Data<>(timeLabel, ma.get("MA5")));
            }
            if (ma.containsKey("MA10")) {
                ma10Series.getData().add(new XYChart.Data<>(timeLabel, ma.get("MA10")));
            }
        }
        
        chart.getData().addAll(closeSeries, ma5Series, ma10Series);
        
        // Add tooltips and styling
        styleLineChart(chart, points);
    }
    
    private static void populateBarChart(BarChart<String, Number> chart, PositionTrendData trendData) {
        List<PositionTrendPoint> points = trendData.getTrendPoints();
        
        XYChart.Series<String, Number> barSeries = new XYChart.Series<>();
        barSeries.setName("Games Out Range");
        
        for (int i = 0; i < points.size(); i++) {
            PositionTrendPoint point = points.get(i);
            String timeLabel = "T" + (i + 1);
            
            // Use range (high - low) for bar height
            double range = point.getRange();
            XYChart.Data<String, Number> barData = new XYChart.Data<>(timeLabel, range);
            barSeries.getData().add(barData);
        }
        
        chart.getData().add(barSeries);
        styleBarChart(chart, points);
    }
    
    private static void populateAreaChart(AreaChart<String, Number> chart, PositionTrendData trendData) {
        List<PositionTrendPoint> points = trendData.getTrendPoints();
        
        XYChart.Series<String, Number> areaSeries = new XYChart.Series<>();
        areaSeries.setName("Games Out Trend");
        
        for (int i = 0; i < points.size(); i++) {
            PositionTrendPoint point = points.get(i);
            String timeLabel = "T" + (i + 1);
            
            XYChart.Data<String, Number> areaData = new XYChart.Data<>(timeLabel, point.getClose());
            areaSeries.getData().add(areaData);
        }
        
        chart.getData().add(areaSeries);
        styleAreaChart(chart, points);
    }
    
    private static void populateOHLCChart(BarChart<String, Number> chart, PositionTrendData trendData) {
        List<PositionTrendPoint> points = trendData.getTrendPoints();
        
        // Create separate series for High, Low, Open, Close
        XYChart.Series<String, Number> highSeries = new XYChart.Series<>();
        highSeries.setName("High");
        
        XYChart.Series<String, Number> lowSeries = new XYChart.Series<>();
        lowSeries.setName("Low");
        
        XYChart.Series<String, Number> closeSeries = new XYChart.Series<>();
        closeSeries.setName("Close");
        
        for (int i = 0; i < points.size(); i++) {
            PositionTrendPoint point = points.get(i);
            String timeLabel = "T" + (i + 1);
            
            highSeries.getData().add(new XYChart.Data<>(timeLabel, point.getHigh()));
            lowSeries.getData().add(new XYChart.Data<>(timeLabel, point.getLow()));
            closeSeries.getData().add(new XYChart.Data<>(timeLabel, point.getClose()));
        }
        
        chart.getData().addAll(highSeries, lowSeries, closeSeries);
        styleOHLCChart(chart, points);
    }
    
    private static void styleLineChart(LineChart<String, Number> chart, List<PositionTrendPoint> points) {
        if (chart.getData().isEmpty()) return;
        
        // Style main line
        XYChart.Series<String, Number> mainSeries = chart.getData().get(0);
        for (int i = 0; i < mainSeries.getData().size() && i < points.size(); i++) {
            XYChart.Data<String, Number> data = mainSeries.getData().get(i);
            PositionTrendPoint point = points.get(i);
            Node node = data.getNode();
            
            if (node != null) {
                String color = getTrendColor(point);
                node.setStyle("-fx-background-color: " + color + "; -fx-background-radius: 4px;");
                
                Tooltip tooltip = createDetailedTooltip(point, i + 1);
                Tooltip.install(node, tooltip);
            }
        }
        
        // Style moving average lines
        if (chart.getData().size() > 1) {
            styleMALine(chart.getData().get(1), "#FF9800", true); // MA5
        }
        if (chart.getData().size() > 2) {
            styleMALine(chart.getData().get(2), "#9C27B0", true); // MA10
        }
    }
    
    private static void styleBarChart(BarChart<String, Number> chart, List<PositionTrendPoint> points) {
        if (chart.getData().isEmpty()) return;
        
        XYChart.Series<String, Number> barSeries = chart.getData().get(0);
        for (int i = 0; i < barSeries.getData().size() && i < points.size(); i++) {
            XYChart.Data<String, Number> data = barSeries.getData().get(i);
            PositionTrendPoint point = points.get(i);
            Node node = data.getNode();
            
            if (node != null) {
                String color = getTrendColor(point);
                double opacity = Math.max(0.4, point.getHitFrequency());
                node.setStyle("-fx-bar-fill: " + color + "; -fx-opacity: " + opacity + ";");
                
                Tooltip tooltip = createDetailedTooltip(point, i + 1);
                Tooltip.install(node, tooltip);
            }
        }
    }
    
    private static void styleAreaChart(AreaChart<String, Number> chart, List<PositionTrendPoint> points) {
        if (chart.getData().isEmpty()) return;
        
        XYChart.Series<String, Number> areaSeries = chart.getData().get(0);
        Node seriesNode = areaSeries.getNode();
        
        if (seriesNode != null) {
            // Style the area fill
            seriesNode.setStyle("-fx-fill: linear-gradient(to bottom, rgba(76, 175, 80, 0.3), rgba(76, 175, 80, 0.1));");
        }
        
        // Add tooltips to data points
        for (int i = 0; i < areaSeries.getData().size() && i < points.size(); i++) {
            XYChart.Data<String, Number> data = areaSeries.getData().get(i);
            PositionTrendPoint point = points.get(i);
            Node node = data.getNode();
            
            if (node != null) {
                Tooltip tooltip = createDetailedTooltip(point, i + 1);
                Tooltip.install(node, tooltip);
            }
        }
    }
    
    private static void styleOHLCChart(BarChart<String, Number> chart, List<PositionTrendPoint> points) {
        if (chart.getData().size() < 3) return;
        
        // Style High, Low, Close series with different colors
        XYChart.Series<String, Number> highSeries = chart.getData().get(0);
        XYChart.Series<String, Number> lowSeries = chart.getData().get(1);
        XYChart.Series<String, Number> closeSeries = chart.getData().get(2);
        
        styleOHLCSeries(highSeries, "#FF5722", points); // High - Red
        styleOHLCSeries(lowSeries, "#2196F3", points);  // Low - Blue
        styleOHLCSeries(closeSeries, "#4CAF50", points); // Close - Green
    }
    
    private static void styleOHLCSeries(XYChart.Series<String, Number> series, String color, List<PositionTrendPoint> points) {
        for (int i = 0; i < series.getData().size() && i < points.size(); i++) {
            XYChart.Data<String, Number> data = series.getData().get(i);
            PositionTrendPoint point = points.get(i);
            Node node = data.getNode();
            
            if (node != null) {
                node.setStyle("-fx-bar-fill: " + color + ";");
                
                Tooltip tooltip = createDetailedTooltip(point, i + 1);
                Tooltip.install(node, tooltip);
            }
        }
    }
    
    private static void styleMALine(XYChart.Series<String, Number> series, String color, boolean dashed) {
        Node seriesNode = series.getNode();
        if (seriesNode != null) {
            String dashArray = dashed ? "-fx-stroke-dash-array: 5 5;" : "";
            seriesNode.setStyle("-fx-stroke: " + color + "; -fx-stroke-width: 2px; " + dashArray);
        }
        
        // Hide symbols for MA lines
        for (XYChart.Data<String, Number> data : series.getData()) {
            Node node = data.getNode();
            if (node != null) {
                node.setVisible(false);
            }
        }
    }
    
    private static String getTrendColor(PositionTrendPoint point) {
        if (point.isBullish()) return BULLISH_COLOR;
        if (point.isBearish()) return BEARISH_COLOR;
        return NEUTRAL_COLOR;
    }
    
    private static Tooltip createDetailedTooltip(PositionTrendPoint point, int period) {
        StringBuilder tooltip = new StringBuilder();
        tooltip.append("Period T").append(period).append("\n");
        tooltip.append("OHLC: ").append(String.format("%.1f/%.1f/%.1f/%.1f", 
            point.getOpen(), point.getHigh(), point.getLow(), point.getClose())).append("\n");
        tooltip.append("Range: ").append(String.format("%.1f", point.getRange())).append("\n");
        tooltip.append("Hit Count: ").append(point.getHitCount()).append("\n");
        tooltip.append("Hit Freq: ").append(String.format("%.1f%%", point.getHitFrequency() * 100)).append("\n");
        tooltip.append("Trend: ").append(point.getTrendDirection());
        
        if (!point.getNumbersInPosition().isEmpty()) {
            tooltip.append("\nNumbers: ").append(point.getNumbersInPosition().toString());
        }
        
        return new Tooltip(tooltip.toString());
    }
    
    public static Chart createVolumeChart(PositionTrendData trendData) {
        CategoryAxis xAxis = new CategoryAxis();
        xAxis.setLabel("Time Period");
        
        NumberAxis yAxis = new NumberAxis();
        yAxis.setLabel("Hit Frequency %");
        yAxis.setAutoRanging(true);
        
        BarChart<String, Number> volumeChart = new BarChart<>(xAxis, yAxis);
        volumeChart.setTitle("Hit Frequency Volume");
        volumeChart.setLegendVisible(false);
        volumeChart.setCategoryGap(2);
        volumeChart.setBarGap(1);
        
        if (trendData.hasValidData()) {
            XYChart.Series<String, Number> volumeSeries = new XYChart.Series<>();
            List<PositionTrendPoint> points = trendData.getTrendPoints();
            
            for (int i = 0; i < points.size(); i++) {
                PositionTrendPoint point = points.get(i);
                String timeLabel = "T" + (i + 1);
                double frequency = point.getHitFrequency() * 100;
                
                volumeSeries.getData().add(new XYChart.Data<>(timeLabel, frequency));
            }
            
            volumeChart.getData().add(volumeSeries);
            
            // Style volume bars
            for (int i = 0; i < volumeSeries.getData().size() && i < points.size(); i++) {
                XYChart.Data<String, Number> data = volumeSeries.getData().get(i);
                PositionTrendPoint point = points.get(i);
                Node node = data.getNode();
                
                if (node != null) {
                    String color = point.getHitCount() > 0 ? BULLISH_COLOR : "#2196F3";
                    node.setStyle("-fx-bar-fill: " + color + ";");
                    
                    Tooltip tooltip = new Tooltip(String.format("T%d: %.1f%% hit frequency (%d hits)",
                        i + 1, point.getHitFrequency() * 100, point.getHitCount()));
                    Tooltip.install(node, tooltip);
                }
            }
        }
        
        return volumeChart;
    }
}