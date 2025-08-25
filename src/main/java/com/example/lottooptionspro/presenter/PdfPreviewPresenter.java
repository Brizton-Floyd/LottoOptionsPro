package com.example.lottooptionspro.presenter;

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
import org.apache.pdfbox.rendering.PDFRenderer;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import javax.imageio.ImageIO;
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

    public void setView(PdfPreviewView view) {
        this.view = view;
    }

    public void setDocument(PDDocument document) {
        renderInitialPages(document);
    }

    private void renderInitialPages(PDDocument document) {
        view.showProgress(true);
        Task<List<Image>> renderTask = new Task<>() {
            @Override
            protected List<Image> call() throws Exception {
                originalColorImages = new ArrayList<>();
                PDFRenderer renderer = new PDFRenderer(document);
                for (int i = 0; i < document.getNumberOfPages(); i++) {
                    originalColorImages.add(renderer.renderImageWithDPI(i, 150));
                }
                document.close();
                return originalColorImages.stream()
                        .map(img -> SwingFXUtils.toFXImage(img, null))
                        .collect(Collectors.toList());
            }
        };

        renderTask.setOnSucceeded(event -> {
            view.displayPdfPages(renderTask.getValue());
            view.showProgress(false);
        });

        renderTask.setOnFailed(event -> {
            view.showError("Failed to render PDF for preview: " + renderTask.getException().getMessage());
            view.showProgress(false);
        });

        new Thread(renderTask).start();
    }

    public void onColorModeChanged(String mode) {
        if (originalColorImages == null) return;

        Task<List<Image>> conversionTask = new Task<>() {
            @Override
            protected List<Image> call() {
                return originalColorImages.stream()
                        .map(img -> {
                            switch (mode) {
                                case "Black & White":
                                    return SwingFXUtils.toFXImage(convertToGrayscale(img), null);
                                case "Scanner-Ready B&W":
                                    return SwingFXUtils.toFXImage(ImageProcessor.convertToSelectiveBnW(img), null);
                                default: // Full Color
                                    return SwingFXUtils.toFXImage(img, null);
                            }
                        })
                        .collect(Collectors.toList());
            }
        };

        conversionTask.setOnSucceeded(event -> view.displayPdfPages(conversionTask.getValue()));
        conversionTask.setOnFailed(event -> view.showError("Failed to convert images."));
        new Thread(conversionTask).start();
    }

    public void save() {
        String colorMode = view.getSelectedColorMode();
        List<BufferedImage> imagesToSave = new ArrayList<>(originalColorImages);
        if ("Black & White".equals(colorMode)) {
            imagesToSave = originalColorImages.stream().map(this::convertToGrayscale).collect(Collectors.toList());
        } else if ("Scanner-Ready B&W".equals(colorMode)) {
            imagesToSave = originalColorImages.stream().map(ImageProcessor::convertToSelectiveBnW).collect(Collectors.toList());
        }

        String initialFileName = "betslips.pdf";
        File file = view.showSavePdfDialog(initialFileName);
        if (file == null) return;

        view.showProgress(true);
        createPdfFromBufferedImages(imagesToSave)
            .doFinally(signal -> Platform.runLater(() -> view.showProgress(false)))
            .subscribe(docToSave -> {
                try {
                    docToSave.save(file);
                    docToSave.close();
                    Platform.runLater(() -> view.closeView());
                } catch (IOException e) {
                    Platform.runLater(() -> view.showError("Failed to save PDF: " + e.getMessage()));
                }
            }, error -> Platform.runLater(() -> view.showError("Failed to save PDF: " + error.getMessage())));
    }

    private BufferedImage convertToGrayscale(BufferedImage source) {
        BufferedImage grayImage = new BufferedImage(source.getWidth(), source.getHeight(), BufferedImage.TYPE_BYTE_GRAY);
        Graphics2D g2d = grayImage.createGraphics();
        g2d.drawImage(source, 0, 0, null);
        g2d.dispose();
        return grayImage;
    }

    private Mono<PDDocument> createPdfFromBufferedImages(List<BufferedImage> markedImages) {
        return Mono.fromCallable(() -> {
            PDDocument document = new PDDocument();
            PDPage currentPage = null;
            PDPageContentStream contentStream = null;
            float currentX = PAGE_PADDING;
            final PDRectangle pageSize = new PDRectangle(PDRectangle.LETTER.getHeight(), PDRectangle.LETTER.getWidth());

            for (BufferedImage awtImage : markedImages) {
                PDImageXObject pdImage = convertToPdfImage(awtImage, document);

                // DEFINITIVE FIX: Scale to fit page height, not a slot.
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

                // Draw a vertical scissor line if there's space for another image.
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

    public void cancel() {
        view.closeView();
    }
}
