package com.example.lottooptionspro.presenter;

import com.example.lottooptionspro.model.range.*;
import com.example.lottooptionspro.service.RangeAnalysisService;
import javafx.application.Platform;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.List;

@Component
public class RangeAnalysisPresenter {
    
    private static final Logger logger = LoggerFactory.getLogger(RangeAnalysisPresenter.class);
    
    private RangeAnalysisView view;
    private final RangeAnalysisService service;
    
    private String currentState;
    private String currentGame;
    private RangeAnalysisRequest currentRequest;
    private RangeAnalysisResponse currentResponse;
    
    @Autowired
    public RangeAnalysisPresenter(RangeAnalysisService service) {
        this.service = service;
    }
    
    public void setView(RangeAnalysisView view) {
        this.view = view;
    }
    
    public Mono<Void> initializeRangeAnalysis(String state, String game) {
        return Mono.fromRunnable(() -> {
            this.currentState = state;
            this.currentGame = game;
            
            Platform.runLater(() -> {
                if (view != null) {
                    view.setUpAnalysisControls(
                        service.getSupportedAnalysisTypes(),
                        service.getSupportedRangeSizesForGame(state, game),
                        state,
                        game
                    );
                }
            });
        });
    }
    
    public Mono<Void> performAnalysis(AnalysisType analysisType, int rangeSize, int maxDraws, List<Integer> positions) {
        if (view != null) {
            Platform.runLater(() -> {
                view.showLoadingIndicator(true);
                view.setAnalysisInProgress(true);
                view.clearRangeHighlights();
            });
        }
        
        RangeAnalysisRequest request = new RangeAnalysisRequest();
        request.setLotteryState(currentState);
        request.setLotteryGame(currentGame);
        request.setAnalysisType(analysisType);
        request.setRangeSize(rangeSize);
        request.setMaxDraws(maxDraws);
        request.setDrawPositions(positions);
        request.setIncludePerformanceMetrics(true);
        
        this.currentRequest = request;
        
        return service.performRangeAnalysis(request)
                .doOnNext(this::handleAnalysisResult)
                .doOnError(this::handleAnalysisError)
                .doFinally(signal -> Platform.runLater(() -> {
                    if (view != null) {
                        view.showLoadingIndicator(false);
                        view.setAnalysisInProgress(false);
                    }
                }))
                .then();
    }
    
    public Mono<Void> performDefaultAnalysis() {
        GameConfiguration config = service.getGameConfiguration(currentState, currentGame);
        return performAnalysis(AnalysisType.ACTUAL, 
                             config.getDefaultRangeSize(), 
                             config.getOptimalMaxDraws(), 
                             config.getDrawPositions());
    }
    
    public Mono<Void> refreshAnalysis() {
        if (currentRequest != null) {
            return performAnalysis(
                currentRequest.getAnalysisType(),
                currentRequest.getRangeSize(),
                currentRequest.getMaxDraws(),
                currentRequest.getDrawPositions()
            );
        } else {
            return performDefaultAnalysis();
        }
    }
    
    public void changeAnalysisType(AnalysisType newType) {
        if (currentRequest != null) {
            performAnalysis(
                newType,
                currentRequest.getRangeSize(),
                currentRequest.getMaxDraws(),
                currentRequest.getDrawPositions()
            ).subscribe();
        } else {
            GameConfiguration config = service.getGameConfiguration(currentState, currentGame);
            performAnalysis(newType, config.getDefaultRangeSize(), config.getOptimalMaxDraws(), config.getDrawPositions()).subscribe();
        }
    }
    
    public void changeRangeSize(int newRangeSize) {
        if (currentRequest != null) {
            performAnalysis(
                currentRequest.getAnalysisType(),
                newRangeSize,
                currentRequest.getMaxDraws(),
                currentRequest.getDrawPositions()
            ).subscribe();
        }
    }
    
    /**
     * Simplified method for range analysis with range size and positions
     * Used by the new simplified controller
     */
    public void performRangeAnalysis(String state, String game, int rangeSize, List<Integer> positions) {
        this.currentState = state;
        this.currentGame = game;
        
        // Use default values for simplified interface
        GameConfiguration config = service.getGameConfiguration(state, game);
        int maxDraws = config.getOptimalMaxDraws();
        AnalysisType analysisType = AnalysisType.ACTUAL; // Default to ACTUAL for simplified view
        
        performAnalysis(analysisType, rangeSize, maxDraws, positions).subscribe();
    }
    
    public void changeMaxDraws(int newMaxDraws) {
        if (currentRequest != null) {
            performAnalysis(
                currentRequest.getAnalysisType(),
                currentRequest.getRangeSize(),
                newMaxDraws,
                currentRequest.getDrawPositions()
            ).subscribe();
        }
    }
    
    public void changeSelectedPosition(int selectedPosition) {
        // This method updates the UI to focus on a specific position
        // It doesn't trigger a new analysis, just updates the position-specific charts
        if (view != null && currentResponse != null) {
            Platform.runLater(() -> {
                // Update position-specific analysis display
                view.setUpPositionAnalysisCharts(currentResponse.getPositionAnalysis());
            });
        }
    }
    
    public void highlightRange(String range) {
        if (view != null && currentResponse != null) {
            Platform.runLater(() -> view.highlightRange(range));
        }
    }
    
    public void showRangeDetails(String range) {
        if (currentResponse != null && currentResponse.hasPerformanceMetrics()) {
            PerformanceMetrics metrics = currentResponse.getPerformanceMetricsForRange(range);
            if (metrics != null) {
                Platform.runLater(() -> highlightRange(range));
                logger.info("Range {} details - Status: {}, Frequency: {:.2f}, Streak: {}", 
                           range, metrics.getStatus(), metrics.getHitFrequency(), metrics.getCurrentStreak());
            }
        }
    }
    
    public void exportResults(String format) {
        if (currentResponse != null) {
            logger.info("Exporting range analysis results in {} format", format);
            Platform.runLater(() -> {
                if (view != null) {
                    view.enableExportControls(false);
                }
            });
        }
    }
    
    public RangeAnalysisResponse getCurrentResponse() {
        return currentResponse;
    }
    
    public RangeAnalysisRequest getCurrentRequest() {
        return currentRequest;
    }
    
    public String getCurrentState() {
        return currentState;
    }
    
    public String getCurrentGame() {
        return currentGame;
    }
    
    public boolean hasAnalysisResults() {
        return currentResponse != null && currentResponse.hasData();
    }
    
    public boolean isServiceAvailable() {
        return service.isServiceAvailable();
    }
    
    private void handleAnalysisResult(RangeAnalysisResponse response) {
        this.currentResponse = response;
        
        Platform.runLater(() -> {
            if (view != null) {
                view.showAnalysisResults(response);
                view.updateAnalysisConfiguration(response.getGameConfiguration());
                
                if (response.hasData()) {
                    view.setUpRangeGrid(response.getRangeHeaders(), response.getPerformanceMetrics());
                    
                    if (response.hasPerformanceMetrics()) {
                        view.setUpPerformanceMetricsTable(response.getPerformanceMetrics());
                    }
                    
                    if (response.hasPositionAnalysis()) {
                        view.setUpPositionAnalysisCharts(response.getPositionAnalysis());
                    }
                    
                    view.setUpDrawResultsTimeline(response.getDrawResults());
                    view.enableExportControls(true);
                } else {
                    view.showNoDataMessage("No data available for the selected criteria");
                }
                
                view.refreshUI();
            }
        });
        
        logger.info("Range analysis completed successfully for {}:{} - {} draws analyzed", 
                   currentState, currentGame, response.getTotalDrawsAnalyzed());
    }
    
    private void handleAnalysisError(Throwable error) {
        logger.error("Range analysis failed for {}:{}", currentState, currentGame, error);
        
        Platform.runLater(() -> {
            if (view != null) {
                view.showDataError(error);
                view.enableExportControls(false);
            }
        });
    }
}