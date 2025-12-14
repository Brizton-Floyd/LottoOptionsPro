package com.example.lottooptionspro.presenter;

import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.image.Image;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class PrintPreviewPresenter {

    private PrintPreviewView view;
    private List<BufferedImage> originalImages;

    public void setView(PrintPreviewView view) {
        this.view = view;
    }

    public void setData(List<BufferedImage> images) {
        this.originalImages = images;
        displayPreview();
    }

    private void displayPreview() {
        if (originalImages == null || originalImages.isEmpty()) {
            view.showError("No images to preview for printing.");
            return;
        }

        view.showProgress(true);

        Task<List<Image>> previewTask = new Task<>() {
            @Override
            protected List<Image> call() {
                // Create composite pages like the PDF version
                List<BufferedImage> pagePreviews = createCompositePageImages(originalImages);
                return pagePreviews.stream()
                        .map(img -> SwingFXUtils.toFXImage(img, null))
                        .collect(Collectors.toList());
            }
        };

        previewTask.setOnSucceeded(e -> {
            view.displayPrintPages(previewTask.getValue());
            view.showProgress(false);
        });

        previewTask.setOnFailed(e -> {
            view.showError("Failed to generate print preview.");
            view.showProgress(false);
        });

        new Thread(previewTask).start();
    }

    private List<BufferedImage> createCompositePageImages(List<BufferedImage> images) {
        List<BufferedImage> pagePreviews = new ArrayList<>();
        if (images.isEmpty()) {
            return pagePreviews;
        }

        final org.apache.pdfbox.pdmodel.common.PDRectangle pageSize = new org.apache.pdfbox.pdmodel.common.PDRectangle(
            org.apache.pdfbox.pdmodel.common.PDRectangle.LETTER.getHeight(), 
            org.apache.pdfbox.pdmodel.common.PDRectangle.LETTER.getWidth()
        );
        final int IMAGES_PER_PAGE = 3;

        List<List<BufferedImage>> pages = partitionImages(images, IMAGES_PER_PAGE);

        for (List<BufferedImage> pageImages : pages) {
            BufferedImage pagePreview = new BufferedImage((int) pageSize.getWidth(), (int) pageSize.getHeight(), BufferedImage.TYPE_INT_ARGB);
            java.awt.Graphics2D g2d = pagePreview.createGraphics();
            
            // Enable anti-aliasing for better quality
            g2d.setRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING, java.awt.RenderingHints.VALUE_ANTIALIAS_ON);
            g2d.setRenderingHint(java.awt.RenderingHints.KEY_RENDERING, java.awt.RenderingHints.VALUE_RENDER_QUALITY);
            
            g2d.setColor(java.awt.Color.WHITE);
            g2d.fillRect(0, 0, (int) pageSize.getWidth(), (int) pageSize.getHeight());

            renderPageLayout(g2d, pageImages, pageSize);

            g2d.dispose();
            pagePreviews.add(pagePreview);
        }
        return pagePreviews;
    }

    private List<List<BufferedImage>> partitionImages(List<BufferedImage> images, int imagesPerPage) {
        List<List<BufferedImage>> pages = new ArrayList<>();
        for (int i = 0; i < images.size(); i += imagesPerPage) {
            pages.add(images.subList(i, Math.min(i + imagesPerPage, images.size())));
        }
        return pages;
    }

    private void renderPageLayout(java.awt.Graphics2D g2d, List<BufferedImage> pageImages, org.apache.pdfbox.pdmodel.common.PDRectangle pageSize) {
        PageLayoutParams layout = new PageLayoutParams(pageImages, pageSize);
        float currentX = layout.startX;

        // Pass 1: Draw the images
        for (BufferedImage awtImage : pageImages) {
            g2d.drawImage(awtImage, (int) currentX, (int) layout.yPos, (int) layout.imageWidth, (int) layout.imageHeight, null);
            currentX += layout.imageWidth + 20f; // SCISSOR_LINE_SPACING
        }

        // Pass 2: Draw the border lines
        drawSolidLine(g2d, layout.startX, 20f, layout.startX, pageSize.getHeight() - 20f);
        drawSolidLine(g2d, layout.startX + layout.totalContentWidth, 20f, layout.startX + layout.totalContentWidth, pageSize.getHeight() - 20f);

        // Pass 3: Draw scissor lines between images
        currentX = layout.startX + layout.imageWidth;
        for (int i = 0; i < pageImages.size() - 1; i++) {
            float lineX = currentX + (20f / 2); // SCISSOR_LINE_SPACING / 2
            drawDashedLine(g2d, lineX, 20f, lineX, pageSize.getHeight() - 20f);
            currentX += layout.imageWidth + 20f;
        }
    }

    private static class PageLayoutParams {
        final float imageWidth;
        final float imageHeight;
        final float startX;
        final float yPos;
        final float totalContentWidth;

        PageLayoutParams(List<BufferedImage> pageImages, org.apache.pdfbox.pdmodel.common.PDRectangle pageSize) {
            float availableWidthForImages = pageSize.getWidth() - (2 * 20f) - ((pageImages.size() - 1) * 20f);
            float calculatedWidth = availableWidthForImages / pageImages.size();

            BufferedImage firstImage = pageImages.get(0);
            float aspectRatio = (float) firstImage.getHeight() / firstImage.getWidth();
            float calculatedHeight = calculatedWidth * aspectRatio;

            if (calculatedHeight > pageSize.getHeight() - (2 * 20f)) {
                this.imageHeight = pageSize.getHeight() - (2 * 20f);
                this.imageWidth = this.imageHeight / aspectRatio;
                this.yPos = 20f;
            } else {
                this.imageHeight = calculatedHeight;
                this.imageWidth = calculatedWidth;
                this.yPos = (pageSize.getHeight() - this.imageHeight) / 2;
            }

            float totalImagesWidth = pageImages.size() * this.imageWidth;
            float totalSpacingWidth = Math.max(0, pageImages.size() - 1) * 20f;
            this.totalContentWidth = totalImagesWidth + totalSpacingWidth;
            this.startX = (pageSize.getWidth() - this.totalContentWidth) / 2;
        }
    }

    private void drawSolidLine(java.awt.Graphics2D g2d, float xStart, float yStart, float xEnd, float yEnd) {
        g2d.setStroke(new java.awt.BasicStroke(1));
        g2d.setColor(java.awt.Color.RED);
        g2d.drawLine((int) xStart, (int) yStart, (int) xEnd, (int) yEnd);
    }

    private void drawDashedLine(java.awt.Graphics2D g2d, float xStart, float yStart, float xEnd, float yEnd) {
        g2d.setStroke(new java.awt.BasicStroke(1, java.awt.BasicStroke.CAP_BUTT, java.awt.BasicStroke.JOIN_MITER, 10.0f, new float[]{10, 5}, 0.0f));
        g2d.setColor(java.awt.Color.RED);
        g2d.drawLine((int) xStart, (int) yStart, (int) xEnd, (int) yEnd);
    }

    public void executePrint() {
        if (originalImages == null || originalImages.isEmpty()) {
            view.showError("No images to print.");
            return;
        }

        view.showProgress(true);

        executeActualPrint()
                .doFinally(signal -> Platform.runLater(() -> view.showProgress(false)))
                .subscribe(success -> {
                    Platform.runLater(() -> {
                        if (success) {
                            view.showSuccess("Print job completed successfully!");
                            view.closeView();
                        } else {
                            view.showError("Print job failed or was cancelled.");
                        }
                    });
                }, error -> Platform.runLater(() -> view.showError("Failed to print: " + error.getMessage())));
    }

    private Mono<Boolean> executeActualPrint() {
        return Mono.fromCallable(() -> {
            // Small delay to ensure UI is updated
            Thread.sleep(100);
            
            // Show print dialog and execute printing
            return view.showPrintDialog();
        }).subscribeOn(Schedulers.boundedElastic());
    }

    public void cancel() {
        view.closeView();
    }
}