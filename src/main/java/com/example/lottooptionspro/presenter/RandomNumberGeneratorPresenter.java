package com.example.lottooptionspro.presenter;

import com.example.lottooptionspro.service.RandomNumberGeneratorService;
import com.floyd.model.generatednumbers.GeneratedNumberData;
import com.floyd.model.request.RandomNumberGeneratorRequest;
import javafx.application.Platform;

import java.io.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RandomNumberGeneratorPresenter {
    private final RandomNumberGeneratorView view;
    private final RandomNumberGeneratorService service;
    private GeneratedNumberData currentData;
    private String gameName, stateName;

    public RandomNumberGeneratorPresenter(RandomNumberGeneratorView view, RandomNumberGeneratorService service) {
        this.view = view;
        this.service = service;
    }

    public void setGameInfo(String stateName, String gameName) {
        this.stateName = stateName;
        this.gameName = gameName;
    }

    public void generateNumbers() {
        RandomNumberGeneratorRequest request = createRequest();
        if (request == null) return; // createRequest handles its own alerts

        service.generateNumbers(request)
                .doOnSubscribe(subscription -> Platform.runLater(() -> {
                    view.showProgress(true);
                    view.setContentDisabled(true);
                }))
                .doFinally(signalType -> Platform.runLater(() -> {
                    view.showProgress(false);
                    view.setContentDisabled(false);
                }))
                .subscribe(
                        this::handleGenerationSuccess,
                        this::handleGenerationError
                );
    }

    private void handleGenerationSuccess(GeneratedNumberData data) {
        this.currentData = data;
        Platform.runLater(this::updateUI);
    }

    private void handleGenerationError(Throwable error) {
        Platform.runLater(() -> {
            view.showAlert("Error", "Failed to generate numbers: " + error.getMessage());
            error.printStackTrace();
        });
    }

    private void updateUI() {
        if (currentData != null) {
            view.updateGeneratedNumbers(currentData.getGeneratedNumbers());
            view.updatePrizeLevelResults(currentData.getPrizeLevelResults());
            view.updateTotalTickets(String.valueOf(currentData.getTotalTickets()));
            view.updateEstimatedDays(String.format("%.2f", currentData.getEstimatedElapsedDaysForTargetedPrizeLevelWin()));
        }
    }

    public void generateBetslips() {
        // This logic is for a future chunk
    }

    private List<List<int[]>> partitionList(List<int[]> list, int size) {
        List<List<int[]>> partitions = new ArrayList<>();
        for (int i = 0; i < list.size(); i += size) {
            partitions.add(list.subList(i, Math.min(i + size, list.size())));
        }
        return partitions;
    }

    public void saveNumbers() {
        if (currentData != null) {
            String directoryPath = "Chosen Number Files/" + stateName;
            LocalDateTime now = LocalDateTime.now();
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MM-dd-yyyy");
            String formattedNow = now.format(formatter);
            String initialFileName = gameName + "_chosen_numbers_" + formattedNow + ".ser";

            File file = view.showSaveDialog(directoryPath, initialFileName);
            if (file != null) {
                try (FileOutputStream fos = new FileOutputStream(file);
                     ObjectOutputStream oos = new ObjectOutputStream(fos)) {
                    oos.writeObject(currentData);
                    view.showAlert("Success", "Numbers saved successfully.");
                } catch (IOException e) {
                    view.showAlert("Error", "Cannot save numbers: " + e.getMessage());
                    e.printStackTrace();
                }
            }
        } else {
            view.showAlert("Info", "No numbers generated to save.");
        }
    }

    private RandomNumberGeneratorRequest createRequest() {
        Map<String, String> generatorMapper = new HashMap<>();
        generatorMapper.put("Random 1", "LOW_ODD_LOW_EVEN_HIGH_ODD_HIGH_EVEN");
        generatorMapper.put("SecureRandom", "SecureRandom");
        generatorMapper.put("ThreadLocalRandom", "ThreadLocalRandom");

        RandomNumberGeneratorRequest request = new RandomNumberGeneratorRequest();
        request.setLotteryGame(this.gameName);
        request.setLotteryState(this.stateName);
        request.setNumberGenerator(generatorMapper.get(view.getSelectedRng()));
        request.setNumberSetsPerPattern(view.getNumberSetPerPattern());
        request.setDrawDaysPerWeek(view.getDrawDaysPerWeek());
        request.setTargetedPrizeLevel(view.getTargetedPrizeLevel());

        // Return null if any of the numeric fields failed parsing (view shows its own alert)
        if (view.getNumberSetPerPattern() == 0 || view.getTargetedPrizeLevel() == 0 || view.getDrawDaysPerWeek() == 0) {
            return null;
        }

        return request;
    }
}
