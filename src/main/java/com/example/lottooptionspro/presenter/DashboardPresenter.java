package com.example.lottooptionspro.presenter;

import com.example.lottooptionspro.service.DashboardService;
import com.floyd.model.response.DashboardResponse;
import javafx.application.Platform;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
public class DashboardPresenter {

    private DashboardView view;
    private final DashboardService service;

    @Autowired
    public DashboardPresenter(DashboardService service) {
        this.service = service;
    }

    public void setView(DashboardView view) {
        this.view = view;
    }

    public Mono<Void> loadDashboardData(String stateName, String gameName) {
        return service.getDashboardData(stateName.toUpperCase(), gameName)
                .flatMap(this::updateUiWithDashboardData)
                .doOnError(view::showDataError)
                .then();
    }

    private Mono<Void> updateUiWithDashboardData(DashboardResponse dashboardResponse) {
        return Mono.fromRunnable(() -> Platform.runLater(() -> {
            if (view == null) return;
            view.setUpDrawPatternTable(dashboardResponse.getDrawResultPatterns());
            view.setUpProbabilityPanes(dashboardResponse);
            view.setUpLegend();
        }));
    }
}
