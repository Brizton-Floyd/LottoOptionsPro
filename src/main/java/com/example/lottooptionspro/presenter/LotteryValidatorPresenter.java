package com.example.lottooptionspro.presenter;

import com.floyd.model.generatednumbers.GeneratedNumberData;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.*;
import java.util.stream.Collectors;

public class LotteryValidatorPresenter {

    private final LotteryValidatorView view;
    private List<List<Integer>> generatedNumbers;
    private List<PrizeLevelResult> prizeLevelResults;

    public LotteryValidatorPresenter(LotteryValidatorView view) {
        this.view = view;
    }

    public void loadGameFile() {
        File file = view.showOpenFileDialog();
        if (file != null) {
            try (FileInputStream fis = new FileInputStream(file);
                 ObjectInputStream ois = new ObjectInputStream(fis)) {
                GeneratedNumberData loadedData = (GeneratedNumberData) ois.readObject();
                this.generatedNumbers = loadedData.getGeneratedNumbers().stream()
                        .map(array -> Arrays.stream(array).boxed().collect(Collectors.toList()))
                        .collect(Collectors.toList());
                this.prizeLevelResults = loadedData.getPrizeLevelResults().stream()
                        .map(data -> new PrizeLevelResult(data.getCorrectNumbers(), 0))
                        .collect(Collectors.toList());

                if (generatedNumbers != null && !generatedNumbers.isEmpty()) {
                    view.createWinningNumberFields(generatedNumbers.get(0).size());
                    view.updateTicketTable(generatedNumbers);
                    view.updatePrizeTable(prizeLevelResults);
                }

            } catch (IOException | ClassNotFoundException e) {
                view.showAlert("Error", "Cannot load ticket data: " + e.getMessage());
            }
        }
    }

    public void validateNumbers() {
        if (generatedNumbers == null) {
            view.showAlert("Info", "Please load a game file first.");
            return;
        }

        List<Integer> winningNumbers;
        try {
            winningNumbers = view.getWinningNumbers().stream()
                                .map(Integer::parseInt)
                                .collect(Collectors.toList());
        } catch (NumberFormatException e) {
            view.showAlert("Error", "Invalid winning numbers. Please enter valid integers.");
            return;
        }

        List<List<Integer>> displayTickets = new ArrayList<>();
        for (List<Integer> ticket : generatedNumbers) {
            List<Integer> displayTicket = new ArrayList<>();
            for (Integer number : ticket) {
                if (winningNumbers.contains(number)) {
                    displayTicket.add(-Math.abs(number)); // Use negative to indicate bold
                } else {
                    displayTicket.add(Math.abs(number));
                }
            }
            displayTickets.add(displayTicket);
        }
        view.updateTicketTable(displayTickets);

        updatePrizeLevelResults(winningNumbers);
    }

    private void updatePrizeLevelResults(List<Integer> winningNumbers) {
        Map<Integer, Integer> hitCounts = new HashMap<>();
        for (List<Integer> ticket : generatedNumbers) {
            int matches = (int) ticket.stream().filter(winningNumbers::contains).count();
            hitCounts.put(matches, hitCounts.getOrDefault(matches, 0) + 1);
        }

        List<PrizeLevelResult> updatedResults = new ArrayList<>();
        for (PrizeLevelResult result : prizeLevelResults) {
            int hits = hitCounts.getOrDefault(result.getCorrectNumbers(), 0);
            updatedResults.add(new PrizeLevelResult(result.getCorrectNumbers(), hits));
        }
        view.updatePrizeTable(updatedResults);
    }

    public static class PrizeLevelResult {
        private final int correctNumbers;
        private final int hits;

        public PrizeLevelResult(int correctNumbers, int hits) {
            this.correctNumbers = correctNumbers;
            this.hits = hits;
        }

        public int getCorrectNumbers() { return correctNumbers; }
        public int getHits() { return hits; }
    }
}
