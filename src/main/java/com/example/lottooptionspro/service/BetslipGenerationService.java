package com.example.lottooptionspro.service;

import com.example.lottooptionspro.models.BetslipTemplate;
import com.example.lottooptionspro.models.GlobalOption;
import com.example.lottooptionspro.models.PlayPanel;
import com.google.gson.Gson;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class BetslipGenerationService {

    private static final float PAGE_PADDING = 20f;
    private static final float SCISSOR_LINE_SPACING = 20f;

    public static class PdfGenerationResult {
        public final List<BufferedImage> images;
        public final BetslipTemplate template;

        public PdfGenerationResult(List<BufferedImage> images, BetslipTemplate template) {
            this.images = images;
            this.template = template;
        }
    }
    
    // Cache for global option selection per generation request
    private String cachedGlobalOptionSelection = null;

    public Mono<PdfGenerationResult> generatePdf(List<int[]> allNumberSets, String stateName, String gameName) {
        // Clear any cached selection from previous generation
        cachedGlobalOptionSelection = null;
        
        return Mono.fromCallable(() -> loadTemplate(stateName, gameName))
                .subscribeOn(Schedulers.boundedElastic())
                .flatMap(templateOptional -> {
                    if (templateOptional.isEmpty()) {
                        return Mono.error(new IOException("Betslip template not found for " + gameName));
                    }
                    return Mono.just(templateOptional.get());
                })
                .flatMap(template -> {
                    try {
                        // Get global option selection once before processing any images
                        if (template.getGlobalOptions() != null && !template.getGlobalOptions().isEmpty()) {
                            cachedGlobalOptionSelection = getSelectedGlobalOption(template);
                        }
                        
                        BufferedImage baseImage = ImageIO.read(new File(template.getImagePath()));
                        return createMultiPanelMarkedImages(allNumberSets, template, baseImage)
                                .map(images -> new PdfGenerationResult(images, template));
                    } catch (IOException e) {
                        return Mono.error(e);
                    }
                });
    }

    private Mono<List<BufferedImage>> createMultiPanelMarkedImages(List<int[]> allNumberSets, BetslipTemplate template, BufferedImage baseImage) {
        int panelsPerSlip = template.getPlayPanels().size();
        if (panelsPerSlip == 0) {
            return Mono.error(new IOException("Template has no defined panels."));
        }

        List<List<int[]>> partitionedNumberSets = new ArrayList<>();
        for (int i = 0; i < allNumberSets.size(); i += panelsPerSlip) {
            partitionedNumberSets.add(allNumberSets.subList(i, Math.min(i + panelsPerSlip, allNumberSets.size())));
        }

        return Flux.fromIterable(partitionedNumberSets)
                .parallel()
                .runOn(Schedulers.parallel())
                .map(chunk -> createDetachedMarkedImage(chunk, template, baseImage))
                .sequential()
                .collectList();
    }

    private BufferedImage createDetachedMarkedImage(List<int[]> numberSetsForSlip, BetslipTemplate template, BufferedImage baseImage) {
        BufferedImage newImage = new BufferedImage(baseImage.getWidth(), baseImage.getHeight(), baseImage.getType());
        java.awt.Graphics2D g2d = newImage.createGraphics();
        g2d.drawImage(baseImage, 0, 0, null);
        g2d.setColor(java.awt.Color.BLACK);

        int markWidth = template.getMark().getWidth();
        int markHeight = template.getMark().getHeight();

        for (int i = 0; i < numberSetsForSlip.size(); i++) {
            List<Integer> numbers = Arrays.stream(numberSetsForSlip.get(i)).boxed().collect(Collectors.toList());
            if (i < template.getPlayPanels().size()) {
                PlayPanel panel = template.getPlayPanels().get(i);
                panel.getMainNumbers().forEach((numberKey, coordinate) -> {
                    if (numbers.contains(Integer.parseInt(numberKey))) {
                        int x = coordinate.getX() - markWidth / 2;
                        int y = coordinate.getY() - markHeight / 2;
                        g2d.fillRect(x, y, markWidth, markHeight);
                    }
                });
            }
        }
        
        // Mark global options (e.g., Cash, Annuity, etc.)
        if (template.getGlobalOptions() != null && !template.getGlobalOptions().isEmpty() && cachedGlobalOptionSelection != null) {
            markGlobalOptions(g2d, template, cachedGlobalOptionSelection);
        }
        
        g2d.dispose();
        return newImage;
    }
    
    private void markGlobalOptions(java.awt.Graphics2D g2d, BetslipTemplate template, String selectedOption) {
        if (selectedOption == null) {
            return; // User cancelled selection
        }
        
        for (GlobalOption option : template.getGlobalOptions()) {
            if (selectedOption.equalsIgnoreCase(option.getName().trim())) {
                int x = (int) (option.getX() - option.getWidth() / 2);
                int y = (int) (option.getY() - option.getHeight() / 2);
                int width = (int) option.getWidth();
                int height = (int) option.getHeight();
                
                System.out.println("DEBUG: Marking global option '" + option.getName() + "' at (" + x + "," + y + ") size " + width + "x" + height);
                g2d.fillRect(x, y, width, height);
                break; // Only mark one option
            }
        }
    }
    
    private String getSelectedGlobalOption(BetslipTemplate template) {
        if (template.getGlobalOptions().isEmpty()) {
            return null;
        }
        
        if (template.getGlobalOptions().size() == 1) {
            // If there's only one option, auto-select it
            String optionName = template.getGlobalOptions().get(0).getName().trim();
            System.out.println("DEBUG: Auto-selecting single global option: " + optionName);
            return optionName;
        }
        
        // Multiple options - prompt user for selection via JavaFX dialog
        return promptUserForGlobalOption(template.getGlobalOptions());
    }
    
    private String promptUserForGlobalOption(java.util.List<GlobalOption> options) {
        // This will be called on a background thread, so we need to use Platform.runLater
        // and use a synchronization mechanism to wait for user selection
        final String[] selectedOption = {null};
        final java.util.concurrent.CountDownLatch latch = new java.util.concurrent.CountDownLatch(1);
        
        javafx.application.Platform.runLater(() -> {
            try {
                java.util.List<String> choices = options.stream()
                    .map(option -> option.getName().trim())
                    .collect(java.util.stream.Collectors.toList());
                
                javafx.scene.control.ChoiceDialog<String> dialog = new javafx.scene.control.ChoiceDialog<>(choices.get(0), choices);
                dialog.setTitle("Global Option Selection");
                dialog.setHeaderText("Multiple global options found on this betslip template:");
                dialog.setContentText("Please choose which option to mark:");
                
                java.util.Optional<String> result = dialog.showAndWait();
                selectedOption[0] = result.orElse(null);
                
            } catch (Exception e) {
                System.err.println("Error showing global option dialog: " + e.getMessage());
                e.printStackTrace();
                // Default to first option if dialog fails
                selectedOption[0] = options.get(0).getName().trim();
            } finally {
                latch.countDown();
            }
        });
        
        try {
            // Wait for user selection (with 30 second timeout)
            if (latch.await(30, java.util.concurrent.TimeUnit.SECONDS)) {
                System.out.println("DEBUG: User selected global option: " + selectedOption[0]);
                return selectedOption[0];
            } else {
                System.out.println("DEBUG: Selection timeout, defaulting to first option: " + options.get(0).getName().trim());
                return options.get(0).getName().trim();
            }
        } catch (InterruptedException e) {
            System.err.println("Interrupted while waiting for user selection, defaulting to first option");
            return options.get(0).getName().trim();
        }
    }

    private Optional<BetslipTemplate> loadTemplate(String stateName, String gameName) {
        String resourcePath = getResourcePath(stateName, gameName);
        try (InputStreamReader reader = new InputStreamReader(this.getClass().getResourceAsStream(resourcePath))) {
            Gson gson = new Gson();
            return Optional.of(gson.fromJson(reader, BetslipTemplate.class));
        } catch (Exception e) {
            System.err.println("Error loading template resource: " + resourcePath);
            e.printStackTrace();
            return Optional.empty();
        }
    }

    private String getResourcePath(String stateName, String gameName) {
        // Try multiple naming conventions
        String[] possiblePaths = {
            // Original convention: /images/StateName/GameName.json
            "/images/" + stateName + "/" + gameName.replaceAll("\\s+", "") + ".json",
            // Lowercase with hyphen: /images/statename/statename-gamename.json  
            "/images/" + stateName + "/" + stateName.toLowerCase() + "-" + gameName.toLowerCase().replaceAll("\\s+", "") + ".json",
            // Just game name: /images/StateName/gamename.json
            "/images/" + stateName + "/" + gameName.toLowerCase().replaceAll("\\s+", "") + ".json"
        };
        
        for (String path : possiblePaths) {
            URL resource = this.getClass().getResource(path);
            if (resource != null) {
                return path;
            }
        }
        
        // Default to first convention if none found
        System.out.println("DEBUG: No template found for State: '" + stateName + "', Game: '" + gameName + "', trying default path: " + possiblePaths[0]);
        return possiblePaths[0];
    }

    public boolean hasTemplateForGame(String stateName, String gameName) {
        if (stateName == null || stateName.trim().isEmpty() || gameName == null || gameName.trim().isEmpty()) {
            return false;
        }
        String resourcePath = getResourcePath(stateName, gameName);
        try {
            URL resource = this.getClass().getResource(resourcePath);
            return resource != null;
        } catch (Exception e) {
            System.err.println("Error checking for template resource: " + resourcePath);
            e.printStackTrace();
            return false;
        }
    }
}
