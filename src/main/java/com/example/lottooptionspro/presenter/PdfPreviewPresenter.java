package com.example.lottooptionspro.presenter;

import com.example.lottooptionspro.models.BetslipTemplate;
import com.example.lottooptionspro.util.ImageProcessor;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.image.Image;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class PdfPreviewPresenter {

    private static final float PAGE_PADDING = 20f;
    private static final float SCISSOR_LINE_SPACING = 20f;

    private PdfPreviewView view;
    private List<BufferedImage> originalColorImages;
    private BetslipTemplate template;

    public void setView(PdfPreviewView view) {
        this.view = view;
    }

    public void setData(List<BufferedImage> images, BetslipTemplate template) {
        this.originalColorImages = images;
        this.template = template;
        renderInitialPages(images);
    }

    private void renderInitialPages(List<BufferedImage> images) {
        view.showProgress(true);
        List<Image> fxImages = images.stream()
                .map(img -> SwingFXUtils.toFXImage(img, null))
                .collect(Collectors.toList());
        view.displayPdfPages(fxImages);
        view.showProgress(false);
    }

    public void onColorModeChanged(String mode) {
        if (originalColorImages == null) return;

        view.showProgress(true);
        Task<List<Image>> conversionTask = new Task<>() {
            @Override
            protected List<Image> call() {
                return originalColorImages.stream()
                        .map(img -> {
                            switch (mode) {
                                case "Black & White":
                                    return SwingFXUtils.toFXImage(convertToGrayscale(img), null);
                                case "Scanner-Ready B&W":
                                    return SwingFXUtils.toFXImage(ImageProcessor.convertToSelectiveBnW(img, template), null);
                                default: // Full Color
                                    return SwingFXUtils.toFXImage(img, null);
                            }
                        })
                        .collect(Collectors.toList());
            }
        };

        conversionTask.setOnSucceeded(event -> {
            view.displayPdfPages(conversionTask.getValue());
            view.showProgress(false);
        });
        conversionTask.setOnFailed(event -> {
            view.showError("Failed to convert images.");
            view.showProgress(false);
        });
        new Thread(conversionTask).start();
    }

    public void save() {
        String colorMode = view.getSelectedColorMode();
        File file = view.showSavePdfDialog("betslips.pdf");
        if (file == null) return;

        view.showProgress(true);

        Task<List<BufferedImage>> processingTask = new Task<>() {
            @Override
            protected List<BufferedImage> call() {
                if ("Black & White".equals(colorMode)) {
                    return originalColorImages.stream().map(PdfPreviewPresenter.this::convertToGrayscale).collect(Collectors.toList());
                } else if ("Scanner-Ready B&W".equals(colorMode)) {
                    return originalColorImages.stream().map(img -> ImageProcessor.convertToSelectiveBnW(img, template)).collect(Collectors.toList());
                } else {
                    return new ArrayList<>(originalColorImages);
                }
            }
        };

        processingTask.setOnSucceeded(event -> {
            List<BufferedImage> processedImages = processingTask.getValue();
            createPdfFromBufferedImages(processedImages)
                    .doFinally(signal -> Platform.runLater(() -> view.showProgress(false)))
                    .subscribe(docToSave -> {
                        try {
                            docToSave.save(file);
                            docToSave.close();
                            Platform.runLater(view::closeView);
                        } catch (IOException e) {
                            Platform.runLater(() -> view.showError("Failed to save PDF: " + e.getMessage()));
                        }
                    }, error -> Platform.runLater(() -> view.showError("Failed to create PDF: " + error.getMessage())));
        });

        processingTask.setOnFailed(event -> {
            view.showError("Failed to process images for saving.");
            view.showProgress(false);
        });

        new Thread(processingTask).start();
    }

    private BufferedImage convertToGrayscale(BufferedImage source) {
        BufferedImage grayImage = new BufferedImage(source.getWidth(), source.getHeight(), BufferedImage.TYPE_BYTE_GRAY);
        java.awt.Graphics2D g2d = grayImage.createGraphics();
        g2d.drawImage(source, 0, 0, null);
        g2d.dispose();
        return grayImage;
    }

    private Mono<PDDocument> createPdfFromBufferedImages(List<BufferedImage> markedImages) {
        return Mono.fromCallable(() -> {
            PDDocument document = new PDDocument();
            final PDRectangle pageSize = new PDRectangle(PDRectangle.LETTER.getHeight(), PDRectangle.LETTER.getWidth()); // Landscape
            final int maxImagesPerPage = 3;
            final float targetHeight = pageSize.getHeight() - (2 * PAGE_PADDING);
            final float drawableWidth = pageSize.getWidth() - (2 * PAGE_PADDING);

            // --- PASS 1: Partition images into pages --- 
            List<List<BufferedImage>> pages = new ArrayList<>();
            List<BufferedImage> currentPageImages = new ArrayList<>();
            float currentWidthOnPage = 0;

            for (BufferedImage image : markedImages) {
                float scale = targetHeight / image.getHeight();
                float scaledWidth = image.getWidth() * scale;

                float spaceNeededForThisImage = currentPageImages.isEmpty() ? scaledWidth : SCISSOR_LINE_SPACING + scaledWidth;

                if (!currentPageImages.isEmpty() && (currentPageImages.size() >= maxImagesPerPage || currentWidthOnPage + spaceNeededForThisImage > drawableWidth)) {
                    pages.add(currentPageImages);
                    currentPageImages = new ArrayList<>();
                    currentWidthOnPage = 0;
                }

                currentPageImages.add(image);
                currentWidthOnPage += currentPageImages.size() == 1 ? scaledWidth : SCISSOR_LINE_SPACING + scaledWidth;
            }
            if (!currentPageImages.isEmpty()) {
                pages.add(currentPageImages);
            }

            // --- PASS 2: Render the partitioned pages to the PDF --- 
            for (List<BufferedImage> pageImages : pages) {
                PDPage currentPage = new PDPage(pageSize);
                document.addPage(currentPage);
                try (PDPageContentStream contentStream = new PDPageContentStream(document, currentPage)) {
                    float currentX = PAGE_PADDING;
                    float yPos = (pageSize.getHeight() - targetHeight) / 2;

                    for (int i = 0; i < pageImages.size(); i++) {
                        BufferedImage awtImage = pageImages.get(i);
                        PDImageXObject pdImage = convertToPdfImage(awtImage, document);
                        float scale = targetHeight / pdImage.getHeight();
                        float scaledWidth = pdImage.getWidth() * scale;

                        if (i > 0) {
                            currentX += SCISSOR_LINE_SPACING;
                            drawDashedLine(contentStream, currentX - (SCISSOR_LINE_SPACING / 2), PAGE_PADDING, currentX - (SCISSOR_LINE_SPACING / 2), pageSize.getHeight() - PAGE_PADDING);
                        }

                        contentStream.drawImage(pdImage, currentX, yPos, scaledWidth, targetHeight);
                        currentX += scaledWidth;
                    }
                }
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

    public void cancel() {
        view.closeView();
    }
}
