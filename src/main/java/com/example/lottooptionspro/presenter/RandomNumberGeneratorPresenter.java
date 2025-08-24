package com.example.lottooptionspro.presenter;

import com.example.lottooptionspro.service.RandomNumberGeneratorService;
import com.floyd.model.generatednumbers.GeneratedNumberData;
import com.floyd.model.request.RandomNumberGeneratorRequest;
import javafx.concurrent.Task;

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
        view.showProgress(true);
        view.setContentDisabled(true);

        Task<GeneratedNumberData> task = new Task<>() {
            @Override
            protected GeneratedNumberData call() throws Exception {
                return service.generateNumbers(createRequest()).block();
            }
        };

        task.setOnSucceeded(e -> {
            view.showProgress(false);
            view.setContentDisabled(false);
            currentData = task.getValue();
            updateUI();
        });

        task.setOnFailed(e -> {
            view.showProgress(false);
            view.setContentDisabled(false);
            view.showAlert("Error", "Failed to generate numbers: " + task.getException().getMessage());
        });

        new Thread(task).start();
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
        String initialDirectory = "Chosen Number Files/" + stateName;
        File file = view.showOpenDialog(initialDirectory);
        if (file != null) {
            try (FileInputStream fis = new FileInputStream(file);
                 ObjectInputStream ois = new ObjectInputStream(fis)) {
                GeneratedNumberData loadedData = (GeneratedNumberData) ois.readObject();
                List<List<int[]>> partitionedNumbers = processLoadedData(loadedData);
                view.openBetslipsWindow(partitionedNumbers, stateName, gameName);
            } catch (IOException | ClassNotFoundException e) {
                view.showAlert("Error", "Cannot load chosen numbers: " + e.getMessage());
            }
        }
    }

    private List<List<int[]>> processLoadedData(GeneratedNumberData loadedData) {
        List<int[]> generatedNumbers = loadedData.getGeneratedNumbers();
        return partitionList(generatedNumbers, 5); // Assuming 5 numbers per panel
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
        return request;
    }
}
