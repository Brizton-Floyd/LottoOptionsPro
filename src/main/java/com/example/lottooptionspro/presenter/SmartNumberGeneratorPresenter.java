package com.example.lottooptionspro.presenter;

import com.example.lottooptionspro.model.smart.*;
import com.example.lottooptionspro.service.SmartNumberGenerationService;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.stream.JsonReader;
import javafx.application.Platform;
import javafx.concurrent.Task;
import reactor.core.Disposable;
import reactor.core.publisher.Mono;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringReader;
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
            
            // Try to parse as JSON with lenient mode, but handle malformed JSON gracefully
            JsonObject jsonData;
            try {
                JsonReader reader = new JsonReader(new StringReader(data));
                reader.setLenient(true);
                com.google.gson.JsonElement element = JsonParser.parseReader(reader);
                
                if (!element.isJsonObject()) {
                    System.err.println("SSE data is not a JSON object: " + data);
                    handleRawProgressData(data);
                    return;
                }
                
                jsonData = element.getAsJsonObject();
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
            // Enhanced error detection - only show error dialog for actual errors
            boolean isActualError = true;
            
            // Check if this looks like progress data that happens to contain "error"
            String lowerData = data.toLowerCase();
            if (lowerData.contains("progress") || 
                lowerData.contains("generating") || 
                lowerData.contains("processing") ||
                lowerData.contains("analyzing") ||
                lowerData.contains("completed") ||
                lowerData.matches(".*\\d+.*") || // Contains numbers (likely progress)
                data.startsWith("data:") || // SSE data format
                data.trim().length() < 10) { // Very short messages are likely progress
                isActualError = false;
            }
            
            if (isActualError) {
                view.updateProgress(-1, "Error in generation");
                Platform.runLater(() -> {
                    view.showAlert("Generation Error", "Generation encountered an error: " + data);
                    resetGenerationState();
                });
            } else {
                // Treat as progress data even though it contains "error"
                view.updateProgress(-1, data.length() > 50 ? data.substring(0, 50) + "..." : data);
            }
        } else {
            // Generic progress update
            view.updateProgress(-1, data.length() > 50 ? data.substring(0, 50) + "..." : data);
        }
    }

    private void handleQualityUpdate(String data) {
        try {
            JsonReader reader = new JsonReader(new StringReader(data));
            reader.setLenient(true);
            com.google.gson.JsonElement element = JsonParser.parseReader(reader);
            
            if (!element.isJsonObject()) {
                System.err.println("Quality data is not a JSON object: " + data);
                return;
            }
            
            JsonObject jsonData = element.getAsJsonObject();
            
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
            JsonReader reader = new JsonReader(new StringReader(data));
            reader.setLenient(true);
            com.google.gson.JsonElement element = JsonParser.parseReader(reader);
            
            if (!element.isJsonObject()) {
                System.err.println("Error data is not a JSON object: " + data);
                view.showAlert("Generation Error", "Generation failed: " + data);
                resetGenerationState();
                return;
            }
            
            JsonObject jsonData = element.getAsJsonObject();
            
            // Check if this is actually an error or just progress data with eventType="error"
            if (jsonData.has("error") && jsonData.get("error").isJsonNull()) {
                // This is actually progress data, not a real error
                System.out.println("Received progress data with eventType='error' but error field is null - treating as progress");
                handleProgressUpdate(data);
                return;
            }
            
            // Check if it has progress-like fields (sessionId, status, attempts, etc.)
            if (jsonData.has("sessionId") && jsonData.has("status") && jsonData.has("attempts")) {
                String status = jsonData.has("status") ? jsonData.get("status").getAsString() : "";
                if ("running".equals(status) || "processing".equals(status)) {
                    // This is progress data, not an error
                    System.out.println("Received progress-like data with eventType='error' - treating as progress");
                    handleProgressUpdate(data);
                    return;
                }
            }
            
            // Only show error if we have an actual error message or the error field is not null
            if (jsonData.has("error") && !jsonData.get("error").isJsonNull()) {
                String errorMessage = jsonData.get("error").getAsString();
                view.showAlert("Generation Error", errorMessage);
                resetGenerationState();
            } else {
                // Fallback to message field if no error field
                String errorMessage = jsonData.has("message") ? 
                    jsonData.get("message").getAsString() : "Unknown generation error";
                
                // Only show as error if the message sounds like an error
                if (errorMessage.toLowerCase().contains("error") || 
                    errorMessage.toLowerCase().contains("failed") ||
                    errorMessage.toLowerCase().contains("exception")) {
                    view.showAlert("Generation Error", errorMessage);
                    resetGenerationState();
                } else {
                    // Treat as progress update
                    System.out.println("Message doesn't sound like error - treating as progress: " + errorMessage);
                    handleProgressUpdate(data);
                }
            }
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
        
        // Always show the results first - the UI will show the Load Full Analysis button if needed
        showFinalResults(result);
    }
    
    private void fetchFullAnalysisData(TicketGenerationResult sampleResult) {
        String fullAnalysisEndpoint = sampleResult.getFullAnalysisEndpoint();
        
        // Replace {sessionId} placeholder with actual session ID
        final String fullAnalysisUrl = fullAnalysisEndpoint.contains("{sessionId}") 
            ? fullAnalysisEndpoint.replace("{sessionId}", currentSessionId)
            : fullAnalysisEndpoint;
        
        Platform.runLater(() -> {
            view.updateProgress(-1, "Computing full historical analysis...");
        });
        
        // Start polling for full analysis data
        pollForFullAnalysis(fullAnalysisUrl, sampleResult, 1);
    }
    
    private void pollForFullAnalysis(String fullAnalysisUrl, TicketGenerationResult sampleResult, int attempt) {
        final int MAX_ATTEMPTS = 20; // Maximum 20 attempts (up to 2 minutes)
        final int POLL_INTERVAL_MS = 6000; // Poll every 6 seconds
        
        System.out.println("=== POLLING FOR FULL ANALYSIS (Attempt " + attempt + "/" + MAX_ATTEMPTS + ") ===");
        
        Platform.runLater(() -> {
            view.updateProgress(-1, "Computing full analysis... (attempt " + attempt + "/" + MAX_ATTEMPTS + ")");
        });
        
        service.getFullAnalysisData(fullAnalysisUrl)
                .subscribe(
                        fullHistorical -> {
                            System.out.println("=== POLLING RESPONSE RECEIVED (Attempt " + attempt + ") ===");
                            System.out.println("Full Historical Performance Object: " + (fullHistorical != null ? "NOT NULL" : "NULL"));
                            
                            // Check if historical performance data is ready and valid
                            boolean isDataReady = fullHistorical != null && 
                                                 fullHistorical.getAnalysisType() != null && 
                                                 !"SAMPLE".equals(fullHistorical.getAnalysisType());
                            
                            System.out.println("Historical Performance Data Ready: " + isDataReady);
                            if (fullHistorical != null) {
                                System.out.println("Analysis Type: " + fullHistorical.getAnalysisType());
                            }
                            
                            if (isDataReady) {
                                // SUCCESS: Full analysis is ready
                                System.out.println("=== FULL ANALYSIS COMPUTATION COMPLETED ===");
                                Platform.runLater(() -> view.showGenerationProgress(false));
                                
                                System.out.println("Full Analysis Type: " + fullHistorical.getAnalysisType());
                                if (fullHistorical.getAnalysisScope() != null) {
                                    System.out.println("Full Analysis Scope - Years: " + fullHistorical.getAnalysisScope().getYearsSpanned());
                                    System.out.println("Full Analysis Scope - Draws: " + fullHistorical.getAnalysisScope().getHistoricalDraws());
                                    System.out.println("Full Analysis Scope - Tickets Analyzed: " + fullHistorical.getAnalysisScope().getTicketsAnalyzed());
                                }
                                if (fullHistorical.getWinSummary() != null) {
                                    System.out.println("Full Analysis Total Wins: " + fullHistorical.getWinSummary().getTotalWins());
                                    System.out.println("Full Analysis Jackpot Wins: " + fullHistorical.getWinSummary().getJackpotWins());
                                }
                                if (fullHistorical.getInsights() != null) {
                                    System.out.println("Full Analysis Insights Count: " + fullHistorical.getInsights().size());
                                }
                                
                                // Merge full analysis data
                                System.out.println("Merging full analysis data with sample result...");
                                sampleResult.setHistoricalPerformance(fullHistorical);
                                // Note: DroughtInformation is not part of the HistoricalPerformance response from backend
                                
                                // Debug what we're setting
                                HistoricalPerformance mergedHistorical = sampleResult.getHistoricalPerformance();
                                System.out.println("MERGED Analysis Type: " + mergedHistorical.getAnalysisType());
                                
                                // Reset button state and hide it since we now have full data
                                Platform.runLater(() -> {
                                    view.setLoadFullAnalysisButtonLoading(false);
                                    view.showLoadFullAnalysisButton(false);
                                });
                                
                                System.out.println("=== CALLING showFinalResults WITH FULL DATA ===");
                                showFinalResults(sampleResult);
                                
                            } else {
                                // Data not ready yet - continue polling if we haven't reached max attempts
                                if (attempt < MAX_ATTEMPTS) {
                                    System.out.println("Full analysis still computing... will retry in " + (POLL_INTERVAL_MS/1000) + " seconds");
                                    
                                    // Schedule next polling attempt
                                    java.util.concurrent.ScheduledExecutorService scheduler = 
                                        java.util.concurrent.Executors.newSingleThreadScheduledExecutor();
                                    scheduler.schedule(() -> {
                                        pollForFullAnalysis(fullAnalysisUrl, sampleResult, attempt + 1);
                                        scheduler.shutdown();
                                    }, POLL_INTERVAL_MS, java.util.concurrent.TimeUnit.MILLISECONDS);
                                    
                                } else {
                                    // Max attempts reached - show timeout error
                                    System.out.println("=== POLLING TIMEOUT REACHED ===");
                                    System.out.println("Full analysis computation took longer than expected");
                                    
                                    Platform.runLater(() -> {
                                        view.showGenerationProgress(false);
                                        view.setLoadFullAnalysisButtonLoading(false);
                                        view.showAlert("Full Analysis Timeout", 
                                            "The full analysis is taking longer than expected to compute.\n" +
                                            "This may be due to high server load or complex analysis requirements.\n" +
                                            "Please try again later or contact support if the issue persists.\n" +
                                            "Displaying sample analysis instead.");
                                    });
                                    
                                    // Show sample results as fallback
                                    showFinalResults(sampleResult);
                                }
                            }
                        },
                        error -> {
                            System.err.println("Polling attempt " + attempt + " failed: " + error.getMessage());
                            
                            // On API error, retry if we haven't reached max attempts
                            if (attempt < MAX_ATTEMPTS) {
                                System.out.println("API error occurred, will retry in " + (POLL_INTERVAL_MS/1000) + " seconds");
                                
                                // Schedule next polling attempt
                                java.util.concurrent.ScheduledExecutorService scheduler = 
                                    java.util.concurrent.Executors.newSingleThreadScheduledExecutor();
                                scheduler.schedule(() -> {
                                    pollForFullAnalysis(fullAnalysisUrl, sampleResult, attempt + 1);
                                    scheduler.shutdown();
                                }, POLL_INTERVAL_MS, java.util.concurrent.TimeUnit.MILLISECONDS);
                                
                            } else {
                                // Max attempts reached - show error
                                System.err.println("All polling attempts failed. Last error: " + error.getMessage());
                                error.printStackTrace();
                                
                                Platform.runLater(() -> {
                                    view.showGenerationProgress(false);
                                    view.setLoadFullAnalysisButtonLoading(false);
                                    view.showAlert("Full Analysis Error", 
                                        "Failed to load full analysis after " + MAX_ATTEMPTS + " attempts.\n" + 
                                        "Last error: " + error.getMessage() + 
                                        "\nDisplaying sample analysis instead.");
                                });
                                showFinalResults(sampleResult);
                            }
                        }
                );
    }
    
    private void showFinalResults(TicketGenerationResult result) {
        // Debug: Print the entire result structure to understand what fields are available
        System.out.println("=== TICKET GENERATION RESULT DEBUG ===");
        System.out.println("Session ID: " + result.getSessionId());
        System.out.println("Full Analysis Endpoint: " + result.getFullAnalysisEndpoint());
        if (result.getHistoricalPerformance() != null) {
            System.out.println("Analysis Type: " + result.getHistoricalPerformance().getAnalysisType());
        }
        System.out.println("=======================================");
        
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

    public void loadFullAnalysis() {
        System.out.println("=== PRESENTER loadFullAnalysis CALLED ===");
        System.out.println("currentResult: " + (currentResult != null ? "NOT NULL" : "NULL"));
        
        if (currentResult == null || currentResult.getFullAnalysisEndpoint() == null) {
            String endpoint = currentResult != null ? currentResult.getFullAnalysisEndpoint() : "N/A";
            System.out.println("ERROR: No full analysis endpoint available. Endpoint: " + endpoint);
            view.showAlert("Error", "No full analysis endpoint available. Endpoint: " + endpoint);
            return;
        }
        
        System.out.println("Full analysis endpoint: " + currentResult.getFullAnalysisEndpoint());
        System.out.println("Setting button to loading state...");
        
        // Set button to loading state
        view.setLoadFullAnalysisButtonLoading(true);
        
        // Fetch full analysis data
        System.out.println("Calling fetchFullAnalysisData...");
        fetchFullAnalysisData(currentResult);
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