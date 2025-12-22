package com.example.lottooptionspro.presenter;

import com.example.lottooptionspro.model.strategyengine.StrategyEngineRequest;
import com.example.lottooptionspro.model.strategyengine.StrategyEngineResponse;
import com.example.lottooptionspro.service.StrategyEngineService;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
public class StrategyEnginePresenter {
    
    private final StrategyEngineService service;
    private StrategyEngineView view;
    
    public StrategyEnginePresenter(StrategyEngineService service) {
        this.service = service;
    }
    
    public void setView(StrategyEngineView view) {
        this.view = view;
    }
    
    public Mono<StrategyEngineResponse> generateStrategy(StrategyEngineRequest request) {
        return service.generatePools(request);
    }
    
    public Mono<StrategyEngineResponse> generateStrategy(String gameId, String lotteryState, 
                                                         Integer poolSize, Integer setCount, 
                                                         String strategyBias) {
        return service.generatePools(gameId, lotteryState, poolSize, setCount, strategyBias);
    }
    
    public interface StrategyEngineView {
        void displayEngineConstants(double averageSkip, int longShotThreshold, int coldRuleLimit);
        void displayHistoricalContext(int drawsAnalyzed, double hitRate, double avgCoverage);
        void displayTier1Anchors(java.util.List<Integer> numbers, java.util.Map<String, String> patterns);
        void displayGeneratedSets(java.util.List<com.example.lottooptionspro.model.strategyengine.GeneratedSet> sets);
        void displayExclusionReport(java.util.List<com.example.lottooptionspro.model.strategyengine.ExclusionReport> exclusions);
        void showError(String message);
        void showLoading(boolean show);
    }
}
