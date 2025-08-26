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
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
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
        updatePreview();
    }

    public void onColorModeChanged(String mode) {
        updatePreview();
    }

    private void updatePreview() {
        if (originalColorImages == null) return;

        view.showProgress(true);
        String colorMode = view.getSelectedColorMode();

        Task<List<Image>> previewTask = new Task<>() {
            @Override
            protected List<Image> call() {
                List<BufferedImage> processedImages = processImagesForColorMode(colorMode);
                List<BufferedImage> pagePreviews = createCompositePageImages(processedImages);
                return pagePreviews.stream()
                        .map(img -> SwingFXUtils.toFXImage(img, null))
                        .collect(Collectors.toList());
            }
        };

        previewTask.setOnSucceeded(e -> {
            view.displayPdfPages(previewTask.getValue());
            view.showProgress(false);
        });
        previewTask.setOnFailed(e -> {
            view.showError("Failed to update preview.");
            view.showProgress(false);
        });

        new Thread(previewTask).start();
    }

    public void save() {
        String colorMode = view.getSelectedColorMode();
        File file = view.showSavePdfDialog("betslips.pdf");
        if (file == null) return;

        view.showProgress(true);

        Task<List<BufferedImage>> processingTask = new Task<>() {
            @Override
            protected List<BufferedImage> call() {
                return processImagesForColorMode(colorMode);
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

    private List<BufferedImage> processImagesForColorMode(String colorMode) {
        if ("Black & White".equals(colorMode)) {
            return originalColorImages.stream().map(this::convertToGrayscale).collect(Collectors.toList());
        } else if ("Scanner-Ready B&W".equals(colorMode)) {
            return originalColorImages.stream().map(img -> ImageProcessor.convertToSelectiveBnW(img, template)).collect(Collectors.toList());
        } else {
            return new ArrayList<>(originalColorImages);
        }
    }

    private BufferedImage convertToGrayscale(BufferedImage source) {
        BufferedImage grayImage = new BufferedImage(source.getWidth(), source.getHeight(), BufferedImage.TYPE_BYTE_GRAY);
        Graphics2D g2d = grayImage.createGraphics();
        g2d.drawImage(source, 0, 0, null);
        g2d.dispose();
        return grayImage;
    }

    private List<BufferedImage> createCompositePageImages(List<BufferedImage> images) {
        List<BufferedImage> pagePreviews = new ArrayList<>();
        if (images.isEmpty()) {
            return pagePreviews;
        }

        final PDRectangle pageSize = new PDRectangle(PDRectangle.LETTER.getHeight(), PDRectangle.LETTER.getWidth());
        final int IMAGES_PER_PAGE = 3;

        List<List<BufferedImage>> pages = partitionImages(images, IMAGES_PER_PAGE);

        for (List<BufferedImage> pageImages : pages) {
            BufferedImage pagePreview = new BufferedImage((int) pageSize.getWidth(), (int) pageSize.getHeight(), BufferedImage.TYPE_INT_ARGB);
            Graphics2D g2d = pagePreview.createGraphics();
            g2d.setColor(Color.WHITE);
            g2d.fillRect(0, 0, (int) pageSize.getWidth(), (int) pageSize.getHeight());

            renderPageLayout(g2d, pageImages, pageSize);

            g2d.dispose();
            pagePreviews.add(pagePreview);
        }
        return pagePreviews;
    }

    private Mono<PDDocument> createPdfFromBufferedImages(List<BufferedImage> markedImages) {
        return Mono.fromCallable(() -> {
            PDDocument document = new PDDocument();
            if (markedImages.isEmpty()) {
                return document;
            }

            final PDRectangle pageSize = new PDRectangle(PDRectangle.LETTER.getHeight(), PDRectangle.LETTER.getWidth());
            final int IMAGES_PER_PAGE = 3;

            List<List<BufferedImage>> pages = partitionImages(markedImages, IMAGES_PER_PAGE);

            for (List<BufferedImage> pageImages : pages) {
                PDPage currentPage = new PDPage(pageSize);
                document.addPage(currentPage);
                try (PDPageContentStream contentStream = new PDPageContentStream(document, currentPage)) {
                    renderPageLayout(contentStream, pageImages, pageSize, document);
                }
            }
            return document;
        }).subscribeOn(Schedulers.boundedElastic());
    }

    private List<List<BufferedImage>> partitionImages(List<BufferedImage> images, int imagesPerPage) {
        List<List<BufferedImage>> pages = new ArrayList<>();
        for (int i = 0; i < images.size(); i += imagesPerPage) {
            pages.add(images.subList(i, Math.min(i + imagesPerPage, images.size())));
        }
        return pages;
    }

    private void renderPageLayout(Graphics2D g2d, List<BufferedImage> pageImages, PDRectangle pageSize) {
        PageLayoutParams layout = new PageLayoutParams(pageImages, pageSize);
        float currentX = layout.startX;

        // Pass 1: Draw the images
        for (BufferedImage awtImage : pageImages) {
            g2d.drawImage(awtImage, (int) currentX, (int) layout.yPos, (int) layout.imageWidth, (int) layout.imageHeight, null);
            currentX += layout.imageWidth + SCISSOR_LINE_SPACING;
        }

        // Pass 2: Draw the lines on top
        drawSolidLine(g2d, layout.startX, PAGE_PADDING, layout.startX, pageSize.getHeight() - PAGE_PADDING);
        drawSolidLine(g2d, layout.startX + layout.totalContentWidth, PAGE_PADDING, layout.startX + layout.totalContentWidth, pageSize.getHeight() - PAGE_PADDING);

        currentX = layout.startX + layout.imageWidth;
        for (int i = 0; i < pageImages.size() - 1; i++) {
            float lineX = currentX + (SCISSOR_LINE_SPACING / 2);
            drawDashedLine(g2d, lineX, PAGE_PADDING, lineX, pageSize.getHeight() - PAGE_PADDING);
            currentX += layout.imageWidth + SCISSOR_LINE_SPACING;
        }
    }

    private void renderPageLayout(PDPageContentStream contentStream, List<BufferedImage> pageImages, PDRectangle pageSize, PDDocument document) throws IOException {
        PageLayoutParams layout = new PageLayoutParams(pageImages, pageSize);
        float currentX = layout.startX;

        for (int i = 0; i < pageImages.size(); i++) {
            BufferedImage awtImage = pageImages.get(i);
            PDImageXObject pdImage = convertToPdfImage(awtImage, document);

            contentStream.drawImage(pdImage, currentX, layout.yPos, layout.imageWidth, layout.imageHeight);
            currentX += layout.imageWidth;

            if (i < pageImages.size() - 1) {
                currentX += SCISSOR_LINE_SPACING;
                float lineX = currentX - (SCISSOR_LINE_SPACING / 2);
                drawDashedLine(contentStream, lineX, PAGE_PADDING, lineX, pageSize.getHeight() - PAGE_PADDING);
            }
        }
    }

    private static class PageLayoutParams {
        final float imageWidth;
        final float imageHeight;
        final float startX;
        final float yPos;
        final float totalContentWidth;

        PageLayoutParams(List<BufferedImage> pageImages, PDRectangle pageSize) {
            float availableWidthForImages = pageSize.getWidth() - (2 * PAGE_PADDING) - ((pageImages.size() - 1) * SCISSOR_LINE_SPACING);
            float calculatedWidth = availableWidthForImages / pageImages.size();

            BufferedImage firstImage = pageImages.get(0);
            float aspectRatio = (float) firstImage.getHeight() / firstImage.getWidth();
            float calculatedHeight = calculatedWidth * aspectRatio;

            if (calculatedHeight > pageSize.getHeight() - (2 * PAGE_PADDING)) {
                this.imageHeight = pageSize.getHeight() - (2 * PAGE_PADDING);
                this.imageWidth = this.imageHeight / aspectRatio;
                this.yPos = PAGE_PADDING;
            } else {
                this.imageHeight = calculatedHeight;
                this.imageWidth = calculatedWidth;
                this.yPos = (pageSize.getHeight() - this.imageHeight) / 2;
            }

            float totalImagesWidth = pageImages.size() * this.imageWidth;
            float totalSpacingWidth = Math.max(0, pageImages.size() - 1) * SCISSOR_LINE_SPACING;
            this.totalContentWidth = totalImagesWidth + totalSpacingWidth;
            this.startX = (pageSize.getWidth() - this.totalContentWidth) / 2;
        }
    }

    private PDImageXObject convertToPdfImage(BufferedImage awtImage, PDDocument document) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(awtImage, "png", out);
        out.flush();
        ByteArrayInputStream in = new ByteArrayInputStream(out.toByteArray());
        return PDImageXObject.createFromByteArray(document, in.readAllBytes(), "png");
    }

    private void drawSolidLine(Graphics2D g2d, float xStart, float yStart, float xEnd, float yEnd) {
        g2d.setStroke(new BasicStroke(1));
        g2d.setColor(Color.RED);
        g2d.drawLine((int) xStart, (int) yStart, (int) xEnd, (int) yEnd);
    }

    private void drawDashedLine(Graphics2D g2d, float xStart, float yStart, float xEnd, float yEnd) {
        g2d.setStroke(new BasicStroke(1, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10.0f, new float[]{10, 5}, 0.0f));
        g2d.setColor(Color.RED);
        g2d.drawLine((int) xStart, (int) yStart, (int) xEnd, (int) yEnd);
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
