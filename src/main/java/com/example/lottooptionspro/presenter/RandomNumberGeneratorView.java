package com.example.lottooptionspro.presenter;

import com.floyd.model.generatednumbers.PrizeLevelResult;

import java.io.File;
import java.util.List;

public interface RandomNumberGeneratorView {
    String getSelectedRng();
    int getNumberSetPerPattern();
    int getTargetedPrizeLevel();
    int getDrawDaysPerWeek();

    void showProgress(boolean show);
    void setContentDisabled(boolean disabled);

    void updateGeneratedNumbers(List<int[]> numbers);
    void updatePrizeLevelResults(List<PrizeLevelResult> results);
    void updateTotalTickets(String total);
    void updateEstimatedDays(String days);

    void openBetslipsWindow(List<List<int[]>> partitionedNumbers, String stateName, String gameName);

    File showSaveDialog(String initialDirectory, String initialFileName);
    File showOpenDialog(String initialDirectory);
    File showSavePdfDialog(String initialFileName);

    void showAlert(String title, String message);
}
