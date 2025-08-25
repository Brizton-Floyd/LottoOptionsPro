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
import java.awt.*;
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

    public Mono<PDDocument> generatePdf(List<int[]> allNumberSets, String stateName, String gameName) {
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
                                .flatMap(this::createPdfFromBufferedImages);
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
        Graphics2D g2d = newImage.createGraphics();
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

    public Mono<PDDocument> createPdfFromBufferedImages(List<BufferedImage> markedImages) {
        return Mono.fromCallable(() -> {
            PDDocument document = new PDDocument();
            PDPage currentPage = null;
            PDPageContentStream contentStream = null;
            float currentX = PAGE_PADDING;
            final PDRectangle pageSize = new PDRectangle(PDRectangle.LETTER.getHeight(), PDRectangle.LETTER.getWidth());

            for (BufferedImage awtImage : markedImages) {
                PDImageXObject pdImage = convertToPdfImage(awtImage, document);

                // Scale to fit page height, not a slot.
                float targetHeight = pageSize.getHeight() - (2 * PAGE_PADDING);
                float scale = targetHeight / pdImage.getHeight();
                float scaledWidth = pdImage.getWidth() * scale;

                if (currentPage == null || currentX + scaledWidth + PAGE_PADDING > pageSize.getWidth()) {
                    if (contentStream != null) {
                        contentStream.close();
                    }
                    currentPage = new PDPage(pageSize);
                    document.addPage(currentPage);
                    contentStream = new PDPageContentStream(document, currentPage);
                    currentX = PAGE_PADDING;
                }

                // Center the image vertically
                float yPos = (pageSize.getHeight() - targetHeight) / 2;
                contentStream.drawImage(pdImage, currentX, yPos, scaledWidth, targetHeight);
                currentX += scaledWidth;

                if (currentX + SCISSOR_LINE_SPACING <= pageSize.getWidth()) {
                    drawDashedLine(contentStream, currentX + (SCISSOR_LINE_SPACING / 2), PAGE_PADDING, currentX + (SCISSOR_LINE_SPACING / 2), pageSize.getHeight() - PAGE_PADDING);
                    currentX += SCISSOR_LINE_SPACING;
                }
            }

            if (contentStream != null) {
                contentStream.close();
            }
            return document;
        }).subscribeOn(Schedulers.boundedElastic());
    }

    private PDImageXObject convertToPdfImage(BufferedImage awtImage, PDDocument document) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(awtImage, "png", out);
        out.flush();
        ByteArrayInputStream in = new ByteArrayInputStream(out.toByteArray());
        return PDImageXObject.createFromByteArray(document, in.readAllBytes(), "png");
    }

    private void drawDashedLine(PDPageContentStream contentStream, float xStart, float yStart, float xEnd, float yEnd) throws IOException {
        contentStream.setLineDashPattern(new float[]{10, 5}, 0);
        contentStream.setStrokingColor(java.awt.Color.RED);
        contentStream.moveTo(xStart, yStart);
        contentStream.lineTo(xEnd, yEnd);
        contentStream.stroke();
        contentStream.setLineDashPattern(new float[]{}, 0);
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
}
