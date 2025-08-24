package com.example.lottooptionspro.presenter;

import com.floyd.model.dashboard.DrawResultPattern;
import com.floyd.model.response.DashboardResponse;

import java.util.List;

public interface DashboardView {
    void setUpDrawPatternTable(List<DrawResultPattern> drawResultPatterns);

    void setUpProbabilityPanes(DashboardResponse dashboardResponse);

    void setUpLegend();

    void showDataError(Throwable error);
}
