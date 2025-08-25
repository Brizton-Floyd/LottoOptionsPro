package com.example.lottooptionspro.service;

import com.example.lottooptionspro.models.BetslipTemplate;
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

    public Mono<PdfGenerationResult> generatePdf(List<int[]> allNumberSets, String stateName, String gameName) {
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
        g2d.dispose();
        return newImage;
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
        String sanitizedGameName = gameName.replaceAll("\\s+", "");
        return "/images/" + stateName + "/" + sanitizedGameName + ".json";
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
