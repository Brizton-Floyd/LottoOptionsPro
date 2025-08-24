package com.example.lottooptionspro.presenter;

import com.example.lottooptionspro.controller.LotteryValidatorController;

import java.io.File;
import java.util.List;

public interface LotteryValidatorView {
    void createWinningNumberFields(int count);
    List<String> getWinningNumbers();
    void updateTicketTable(List<List<Integer>> tickets);
    void updatePrizeTable(List<LotteryValidatorPresenter.PrizeLevelResult> results);
    File showOpenFileDialog();
    void showAlert(String title, String message);
}
