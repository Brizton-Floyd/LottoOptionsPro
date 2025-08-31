package com.example.lottooptionspro.presenter;

import com.example.lottooptionspro.model.smart.*;
import com.example.lottooptionspro.service.SmartNumberGenerationService;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import javafx.application.Platform;
import javafx.concurrent.Task;
import reactor.core.Disposable;
import reactor.core.publisher.Mono;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

public class SmartNumberGeneratorPresenter {
    
    private final SmartNumberGeneratorView view;
    private final SmartNumberGenerationService service;
    private final Gson gson;
    
    private String currentSessionId;
    private Disposable progressSubscription;
    private TicketGenerationResult currentResult;
    private LotteryConfiguration currentConfig;
    private long generationStartTime;
    private final AtomicReference<String> stateName = new AtomicReference<>();
    private final AtomicReference<String> gameName = new AtomicReference<>();

    public SmartNumberGeneratorPresenter(SmartNumberGeneratorView view, SmartNumberGenerationService service) {
        this.view = view;
        this.service = service;
        this.gson = new GsonBuilder().setPrettyPrinting().create();
    }

    public Mono<LotteryConfiguration> loadLotteryConfiguration(String stateName, String gameName) {
        this.stateName.set(stateName);
        this.gameName.set(gameName);
        
        String configId = service.deriveConfigId(stateName, gameName);
        
        // Test API connectivity first
        return service.testApiConnectivity()
                .doOnSuccess(result -> System.out.println("API connectivity test completed"))
                .doOnError(error -> System.err.println("API connectivity test failed: " + error.getMessage()))
                .onErrorReturn("Connection test failed")
                .then(service.getLotteryConfiguration(configId))
                .doOnSuccess(config -> {
                    this.currentConfig = config;
                    System.out.println("✅ Successfully loaded config for: " + configId);
                })
                .doOnError(error -> {
                    System.err.println("❌ Failed to load configuration for " + configId + ": " + error.getMessage());
                    error.printStackTrace();
                });
    }

    public void generateSmartTickets() {
        SmartGenerationRequest request = view.createGenerationRequest();
        if (request == null) {
            view.showAlert("Error", "Invalid generation parameters.");
            return;
        }

        // Validate request
        if (request.getNumberOfTickets() <= 0) {
            view.showAlert("Error", "Number of tickets must be greater than 0.");
            return;
        }

        if (request.getTargetTier() == null || request.getTargetTier().isEmpty()) {
            view.showAlert("Error", "Please select a target tier.");
            return;
        }

        generationStartTime = System.currentTimeMillis();
        
        System.out.println("Starting generation with request:");
        System.out.println("- Target Tier: " + request.getTargetTier());
        System.out.println("- Number of Tickets: " + request.getNumberOfTickets());
        System.out.println("- Lottery Config ID: " + request.getLotteryConfigId());
        System.out.println("- Strategy: " + request.getGenerationStrategy());

        service.startGeneration(request)
                .doOnSubscribe(subscription -> Platform.runLater(() -> {
                    view.showLoading(true);
                    view.setContentDisabled(true);
                    view.enableGenerationControls(false);
                }))
                .doOnSuccess(response -> System.out.println("Generation start successful: " + response.getSessionId()))
                .doOnError(error -> System.err.println("Generation start failed: " + error.getMessage()))
                .subscribe(
                        this::handleGenerationStart,
                        this::handleGenerationError
                );
    }

    private void handleGenerationStart(SmartGenerationResponse response) {
        this.currentSessionId = response.getSessionId();
        
        Platform.runLater(() -> {
            view.showLoading(false);
            view.showGenerationProgress(true);
            view.updateSessionInfo(currentSessionId);
            view.updateProgress(0.0, "Generation started...");
        });

        // Start monitoring progress
        startProgressMonitoring();
    }

    private void startProgressMonitoring() {
        if (currentSessionId == null) return;

        progressSubscription = service.getGenerationProgress(currentSessionId)
                .doOnNext(event -> {
                    Platform.runLater(() -> {
                        handleSSEEvent(event);
                    });
                })
                .doOnComplete(() -> {
                    System.out.println("SSE stream completed for session: " + currentSessionId);
                    // Don't automatically fetch results - wait for explicit completion signal
                    Platform.runLater(() -> {
                        view.updateProgress(-1, "Generation stream ended, checking final status...");
                        // Start polling to check if generation actually completed
                        startStatusPolling();
                    });
                })
                .doOnError(error -> {
                    System.err.println("SSE error for session " + currentSessionId + ": " + error.getMessage());
                    Platform.runLater(() -> {
                        // SSE failed, start fallback polling
                        view.updateProgress(-1, "Connection lost, switching to polling...");
                        startStatusPolling();
                    });
                })
                .subscribe();
    }

    private void handleSSEEvent(org.springframework.http.codec.ServerSentEvent<String> event) {
        try {
            String eventType = event.event();
            String data = event.data();
            
            if (data == null || data.trim().isEmpty()) {
                return;
            }

            switch (eventType != null ? eventType : "data") {
                case "progress":
                    handleProgressUpdate(data);
                    break;
                case "quality":
                    handleQualityUpdate(data);
                    break;
                case "complete":
                    handleGenerationComplete(data);
                    break;
                case "error":
                    handleGenerationSSEError(data);
                    break;
                default:
                    // Try to parse as generic progress update
                    handleProgressUpdate(data);
                    break;
            }
        } catch (Exception e) {
            System.err.println("Failed to handle SSE event: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void handleProgressUpdate(String data) {
        try {
            // Log the raw SSE data for debugging
            System.out.println("Raw SSE data: " + data);
            
            // Try to parse as JSON, but handle malformed JSON gracefully
            JsonObject jsonData;
            try {
                jsonData = JsonParser.parseString(data).getAsJsonObject();
            } catch (com.google.gson.JsonSyntaxException e) {
                System.err.println("Invalid JSON in SSE data, using fallback parsing: " + e.getMessage());
                handleRawProgressData(data);
                return;
            }
            
            if (jsonData.has("progress")) {
                int progress = jsonData.get("progress").getAsInt();
                String message = jsonData.has("message") ? jsonData.get("message").getAsString() : "Processing...";
                
                view.updateProgress(progress / 100.0, message);
                
                if (jsonData.has("ticketsGenerated") && jsonData.has("totalTickets")) {
                    int generated = jsonData.get("ticketsGenerated").getAsInt();
                    int total = jsonData.get("totalTickets").getAsInt();
                    view.updateProgress(progress / 100.0, 
                        String.format("%s (%d/%d tickets)", message, generated, total));
                }
            }
            
            double elapsedSeconds = (System.currentTimeMillis() - generationStartTime) / 1000.0;
            view.updateTimeElapsed(elapsedSeconds);
            
        } catch (Exception e) {
            System.err.println("Failed to parse progress data: " + e.getMessage());
            e.printStackTrace();
            // Fallback to simple progress update
            view.updateProgress(-1, "Processing...");
        }
    }

    private void handleRawProgressData(String data) {
        // Handle non-JSON SSE data (might be plain text status updates)
        System.out.println("Handling raw SSE data: " + data);
        
        if (data.toLowerCase().contains("complete") || data.toLowerCase().contains("finished")) {
            view.updateProgress(1.0, "Generation completed");
            // Generation completed according to SSE, fetch results now
            fetchFinalResults();
        } else if (data.toLowerCase().contains("error")) {
            view.updateProgress(-1, "Error in generation");
            Platform.runLater(() -> {
                view.showAlert("Generation Error", "Generation encountered an error: " + data);
                resetGenerationState();
            });
        } else {
            // Generic progress update
            view.updateProgress(-1, data.length() > 50 ? data.substring(0, 50) + "..." : data);
        }
    }

    private void handleQualityUpdate(String data) {
        try {
            JsonObject jsonData = JsonParser.parseString(data).getAsJsonObject();
            
            if (jsonData.has("qualityMetrics")) {
                QualityMetrics metrics = gson.fromJson(jsonData.get("qualityMetrics"), QualityMetrics.class);
                view.updateQualityMetrics(metrics);
            }
        } catch (Exception e) {
            System.err.println("Failed to parse quality data: " + e.getMessage());
        }
    }

    private void handleGenerationComplete(String data) {
        System.out.println("✅ Generation completed via SSE signal");
        view.updateProgress(1.0, "Generation completed, fetching results...");
        
        // Add small delay to ensure backend has processed the completion
        reactor.core.scheduler.Schedulers.boundedElastic().schedule(() -> {
            fetchFinalResults();
        }, 1, java.util.concurrent.TimeUnit.SECONDS);
    }

    private void handleGenerationSSEError(String data) {
        try {
            JsonObject jsonData = JsonParser.parseString(data).getAsJsonObject();
            String errorMessage = jsonData.has("message") ? 
                jsonData.get("message").getAsString() : "Unknown generation error";
            
            view.showAlert("Generation Error", errorMessage);
            resetGenerationState();
        } catch (Exception e) {
            view.showAlert("Generation Error", "Generation failed with unknown error");
            resetGenerationState();
        }
    }

    private void fetchFinalResults() {
        if (currentSessionId == null) {
            System.err.println("Cannot fetch results: currentSessionId is null");
            return;
        }

        System.out.println("Fetching final results for session: " + currentSessionId);
        
        service.getGenerationResult(currentSessionId)
                .doOnSubscribe(sub -> System.out.println("Subscribing to getGenerationResult for session: " + currentSessionId))
                .doOnSuccess(result -> System.out.println("Successfully received result for session: " + currentSessionId))
                .doOnError(error -> System.err.println("Error fetching results for session " + currentSessionId + ": " + error.getMessage()))
                .subscribe(
                        this::handleFinalResults,
                        this::handleFetchResultsError
                );
    }

    private void handleFinalResults(TicketGenerationResult result) {
        this.currentResult = result;
        
        Platform.runLater(() -> {
            view.showGenerationProgress(false);
            view.showResults(result);
            view.setContentDisabled(false);
            view.enableGenerationControls(true);
            
            double elapsedSeconds = (System.currentTimeMillis() - generationStartTime) / 1000.0;
            view.updateTimeElapsed(elapsedSeconds);
        });
    }

    private void handleGenerationError(Throwable error) {
        Platform.runLater(() -> {
            view.showAlert("Error", "Generation failed: " + error.getMessage());
            resetGenerationState();
            error.printStackTrace();
        });
    }

    private void handleFetchResultsError(Throwable error) {
        System.err.println("Failed to fetch final results: " + error.getMessage());
        
        Platform.runLater(() -> {
            if (error instanceof org.springframework.web.reactive.function.client.WebClientResponseException) {
                org.springframework.web.reactive.function.client.WebClientResponseException webError = 
                    (org.springframework.web.reactive.function.client.WebClientResponseException) error;
                
                String errorMsg = String.format("API Error %d: %s\nSession ID: %s\nEndpoint: %s", 
                    webError.getStatusCode().value(),
                    webError.getStatusText(),
                    currentSessionId,
                    webError.getRequest() != null ? webError.getRequest().getURI() : "unknown");
                
                view.showAlert("Generation Error", errorMsg);
                
                // If it's a 500 error, the session might still be running - try polling
                if (webError.getStatusCode().value() == 500) {
                    view.updateProgress(-1, "Server error, retrying...");
                    
                    // Wait 3 seconds and try again
                    reactor.core.scheduler.Schedulers.boundedElastic().schedule(() -> {
                        System.out.println("Retrying result fetch after 500 error...");
                        retryFetchResults();
                    }, 3, java.util.concurrent.TimeUnit.SECONDS);
                    return;
                }
            } else {
                view.showAlert("Error", "Failed to fetch generation results: " + error.getMessage());
            }
            
            resetGenerationState();
        });
    }
    
    private void retryFetchResults() {
        if (currentSessionId == null) return;
        
        System.out.println("Retry attempt for session: " + currentSessionId);
        
        service.getGenerationResult(currentSessionId)
                .subscribe(
                        result -> {
                            System.out.println("Retry successful for session: " + currentSessionId);
                            Platform.runLater(() -> handleFinalResults(result));
                        },
                        error -> {
                            System.err.println("Retry failed for session: " + currentSessionId);
                            Platform.runLater(() -> {
                                view.showAlert("Error", "Generation results unavailable. The session may have expired or the server is experiencing issues.");
                                resetGenerationState();
                            });
                        }
                );
    }

    public void cancelGeneration() {
        if (currentSessionId != null) {
            service.cancelGeneration(currentSessionId)
                    .subscribe(
                            success -> Platform.runLater(() -> {
                                view.showAlert("Info", "Generation cancelled successfully.");
                                resetGenerationState();
                            }),
                            error -> Platform.runLater(() -> {
                                view.showAlert("Error", "Failed to cancel generation: " + error.getMessage());
                            })
                    );
        }
    }

    private void resetGenerationState() {
        if (progressSubscription != null && !progressSubscription.isDisposed()) {
            progressSubscription.dispose();
            progressSubscription = null;
        }
        
        currentSessionId = null;
        view.showGenerationProgress(false);
        view.showLoading(false);
        view.setContentDisabled(false);
        view.enableGenerationControls(true);
    }

    private void startStatusPolling() {
        if (currentSessionId == null) return;
        
        System.out.println("Starting status polling for session: " + currentSessionId);
        
        // Poll the generation status to check completion
        Task<Void> statusPollingTask = new Task<Void>() {
            @Override
            protected Void call() throws Exception {
                final int maxAttempts = 30; // Poll for up to 60 seconds (30 * 2 seconds)
                final AtomicInteger attempts = new AtomicInteger(0);
                
                while (!isCancelled() && currentSessionId != null && attempts.get() < maxAttempts) {
                    try {
                        Thread.sleep(2000); // Poll every 2 seconds
                        attempts.incrementAndGet();
                        
                        System.out.println("Status poll attempt " + attempts.get() + " for session: " + currentSessionId);
                        
                        // Try to get the result - if successful, generation is done
                        service.getGenerationResult(currentSessionId)
                                .subscribe(
                                    result -> {
                                        if (result.getFinalStatus() != null && 
                                            ("COMPLETED".equals(result.getFinalStatus()) || 
                                             "FAILED".equals(result.getFinalStatus()))) {
                                            
                                            System.out.println("✅ Generation confirmed complete via polling: " + result.getFinalStatus());
                                            Platform.runLater(() -> handleFinalResults(result));
                                            cancel();
                                        } else {
                                            System.out.println("Generation still in progress, status: " + result.getFinalStatus());
                                            Platform.runLater(() -> 
                                                view.updateProgress(Math.min(0.9, attempts.get() * 0.05), 
                                                    "Generation in progress... (attempt " + attempts.get() + ")"));
                                        }
                                    },
                                    error -> {
                                        if (error instanceof org.springframework.web.reactive.function.client.WebClientResponseException) {
                                            org.springframework.web.reactive.function.client.WebClientResponseException webError = 
                                                (org.springframework.web.reactive.function.client.WebClientResponseException) error;
                                            
                                            if (webError.getStatusCode().value() == 404) {
                                                System.err.println("Session not found (404) - generation may have failed");
                                                Platform.runLater(() -> {
                                                    view.showAlert("Session Error", "Generation session not found. It may have expired or failed.");
                                                    resetGenerationState();
                                                });
                                                cancel();
                                            } else if (webError.getStatusCode().value() == 500) {
                                                System.err.println("Server error (500) on attempt " + attempts.get() + " - continuing to poll");
                                                Platform.runLater(() -> 
                                                    view.updateProgress(-1, "Server busy, retrying... (attempt " + attempts.get() + ")"));
                                            }
                                        } else {
                                            System.err.println("Polling error: " + error.getMessage());
                                            Platform.runLater(() -> 
                                                view.updateProgress(-1, "Checking status... (attempt " + attempts.get() + ")"));
                                        }
                                    }
                                );
                    } catch (InterruptedException e) {
                        break;
                    }
                }
                
                if (attempts.get() >= maxAttempts) {
                    Platform.runLater(() -> {
                        view.showAlert("Timeout", "Generation is taking longer than expected. Please check the server status.");
                        resetGenerationState();
                    });
                }
                
                return null;
            }
        };
        
        Thread pollingThread = new Thread(statusPollingTask);
        pollingThread.setDaemon(true);
        pollingThread.start();
    }

    public void exportCsv() {
        if (currentResult == null || currentResult.getTickets() == null) {
            view.showAlert("Info", "No tickets available to export.");
            return;
        }

        String directoryPath = "Smart Generated Tickets/" + stateName.get();
        LocalDateTime now = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MM-dd-yyyy_HH-mm");
        String timestamp = now.format(formatter);
        String initialFileName = String.format("%s_smart_tickets_%s.csv", gameName.get(), timestamp);

        File file = view.showSaveDialog(directoryPath, initialFileName);
        if (file != null) {
            try {
                exportTicketsToCsv(file);
                view.showAlert("Success", "Tickets exported to CSV successfully.");
            } catch (IOException e) {
                view.showAlert("Error", "Failed to export CSV: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    private void exportTicketsToCsv(File file) throws IOException {
        try (FileWriter writer = new FileWriter(file)) {
            // CSV Header
            writer.write("Ticket Number,Numbers,Quality Grade,Optimization Score\n");
            
            // Ticket data
            List<List<Integer>> tickets = currentResult.getTickets();
            QualityMetrics metrics = currentResult.getQualityMetrics();
            
            for (int i = 0; i < tickets.size(); i++) {
                List<Integer> ticket = tickets.get(i);
                String numbersStr = ticket.stream()
                        .map(String::valueOf)
                        .collect(java.util.stream.Collectors.joining(" "));
                
                writer.write(String.format("%d,\"%s\",%s,%.2f\n", 
                        i + 1, 
                        numbersStr,
                        metrics != null ? metrics.getQualityGrade() : "N/A",
                        metrics != null ? metrics.getOptimizationScore() : 0.0));
            }
        }
    }

    public void saveTickets() {
        if (currentResult == null) {
            view.showAlert("Info", "No tickets available to save.");
            return;
        }

        String directoryPath = "Smart Generated Tickets/" + stateName.get();
        LocalDateTime now = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MM-dd-yyyy_HH-mm");
        String timestamp = now.format(formatter);
        String initialFileName = String.format("%s_smart_tickets_%s.json", gameName.get(), timestamp);

        File file = view.showSaveDialog(directoryPath, initialFileName);
        if (file != null) {
            try {
                saveTicketsToJson(file);
                view.showAlert("Success", "Tickets saved successfully.");
            } catch (IOException e) {
                view.showAlert("Error", "Failed to save tickets: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    private void saveTicketsToJson(File file) throws IOException {
        try (FileWriter writer = new FileWriter(file)) {
            SmartTicketSaveData saveData = new SmartTicketSaveData();
            saveData.setStateName(stateName.get());
            saveData.setGameName(gameName.get());
            saveData.setGenerationResult(currentResult);
            saveData.setLotteryConfiguration(currentConfig);
            saveData.setSavedAt(LocalDateTime.now().toString());
            
            // Add validation info for betslip integration
            saveData.setBetslipIntegrationReady(validateBetslipIntegration());
            
            gson.toJson(saveData, writer);
        }
    }

    public String deriveConfigId(String stateName, String gameName) {
        return service.deriveConfigId(stateName, gameName);
    }

    private boolean validateBetslipIntegration() {
        if (currentResult == null || currentResult.getTickets() == null) {
            return false;
        }

        try {
            // Test the conversion that will be used for betslip generation
            int[][] ticketArrays = currentResult.getTicketsAsIntArrays();
            
            // Validate that all tickets have valid numbers
            for (int[] ticket : ticketArrays) {
                if (ticket == null || ticket.length == 0) {
                    System.err.println("Integration validation failed: empty ticket found");
                    return false;
                }
                
                // Check for valid number ranges (basic validation)
                for (int number : ticket) {
                    if (number < 1 || number > 99) { // General range check
                        System.err.println("Integration validation warning: number " + number + " outside typical range");
                    }
                }
            }
            
            System.out.println("Betslip integration validation passed: " + ticketArrays.length + " tickets ready");
            return true;
            
        } catch (Exception e) {
            System.err.println("Integration validation failed: " + e.getMessage());
            return false;
        }
    }

    private static class SmartTicketSaveData {
        private String stateName;
        private String gameName;
        private TicketGenerationResult generationResult;
        private LotteryConfiguration lotteryConfiguration;
        private String savedAt;
        private boolean betslipIntegrationReady;

        public String getStateName() { return stateName; }
        public void setStateName(String stateName) { this.stateName = stateName; }

        public String getGameName() { return gameName; }
        public void setGameName(String gameName) { this.gameName = gameName; }

        public TicketGenerationResult getGenerationResult() { return generationResult; }
        public void setGenerationResult(TicketGenerationResult generationResult) { this.generationResult = generationResult; }

        public LotteryConfiguration getLotteryConfiguration() { return lotteryConfiguration; }
        public void setLotteryConfiguration(LotteryConfiguration lotteryConfiguration) { this.lotteryConfiguration = lotteryConfiguration; }

        public String getSavedAt() { return savedAt; }
        public void setSavedAt(String savedAt) { this.savedAt = savedAt; }

        public boolean isBetslipIntegrationReady() { return betslipIntegrationReady; }
        public void setBetslipIntegrationReady(boolean betslipIntegrationReady) { this.betslipIntegrationReady = betslipIntegrationReady; }
    }

    public void testEndToEndIntegration() {
        System.out.println("=== Smart Generator Integration Test ===");
        System.out.println("State: " + stateName.get());
        System.out.println("Game: " + gameName.get());
        System.out.println("Config ID: " + deriveConfigId(stateName.get(), gameName.get()));
        
        if (currentConfig != null) {
            System.out.println("Lottery Config: " + currentConfig.getName());
            System.out.println("Draw Size: " + currentConfig.getDrawSize());
            System.out.println("Number Range: " + currentConfig.getNumberRange().getMin() + 
                             " to " + currentConfig.getNumberRange().getMax());
        }
        
        if (currentResult != null) {
            System.out.println("Generated Tickets: " + currentResult.getTicketCount());
            System.out.println("Betslip Integration Ready: " + validateBetslipIntegration());
            
            if (currentResult.getQualityMetrics() != null) {
                QualityMetrics metrics = currentResult.getQualityMetrics();
                System.out.println("Quality Grade: " + metrics.getQualityGrade());
                System.out.println("Optimization Score: " + metrics.getOptimizationScore() + "%");
            }
        }
        System.out.println("=== Integration Test Complete ===");
    }
}