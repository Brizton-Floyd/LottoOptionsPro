package com.example.lottooptionspro.presenter;

import javafx.application.Platform;
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

    private PdfPreviewView view;
    private List<BufferedImage> originalColorImages;

    public void setView(PdfPreviewView view) {
        this.view = view;
    }

    public void setDocument(PDDocument document) {
        renderInitialPages(document);
    }

    private void renderInitialPages(PDDocument document) {
        Mono.fromCallable(() -> {
            originalColorImages = new ArrayList<>();
            PDFRenderer renderer = new PDFRenderer(document);
            for (int i = 0; i < document.getNumberOfPages(); i++) {
                originalColorImages.add(renderer.renderImageWithDPI(i, 150));
            }
            return originalColorImages.stream()
                    .map(img -> (Image) SwingFXUtils.toFXImage(img, null))
                    .collect(Collectors.toList());
        })
        .doFinally(signal -> {
            try {
                document.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        })
        .subscribeOn(Schedulers.boundedElastic())
        .subscribe(
            images -> Platform.runLater(() -> view.displayPdfPages(images)),
            error -> Platform.runLater(() -> view.showError("Failed to render PDF: " + error.getMessage()))
        );
    }

    public void onColorModeChanged(boolean isGrayscale) {
        if (originalColorImages == null) return;

        List<Image> imagesToShow = originalColorImages.stream()
                .map(img -> {
                    if (isGrayscale) {
                        return SwingFXUtils.toFXImage(convertToGrayscale(img), null);
                    }
                    return SwingFXUtils.toFXImage(img, null);
                })
                .collect(Collectors.toList());

        Platform.runLater(() -> view.displayPdfPages(imagesToShow));
    }

    public void save() {
        String colorMode = view.getSelectedColorMode();
        boolean toGrayscale = "Black & White".equals(colorMode);

        List<BufferedImage> imagesToSave = new ArrayList<>(originalColorImages);
        if (toGrayscale) {
            imagesToSave = originalColorImages.stream()
                    .map(this::convertToGrayscale)
                    .collect(Collectors.toList());
        }

        String initialFileName = "betslips.pdf";
        File file = view.showSavePdfDialog(initialFileName);
        if (file == null) return;

        createPdfFromBufferedImages(imagesToSave)
            .doOnSuccess(docToSave -> {
                try {
                    docToSave.save(file);
                    docToSave.close();
                    Platform.runLater(() -> view.closeView());
                } catch (IOException e) {
                    Platform.runLater(() -> view.showError("Failed to save PDF: " + e.getMessage()));
                }
            })
            .subscribe();
    }

    private Mono<PDDocument> createPdfFromBufferedImages(List<BufferedImage> markedImages) {
        return Mono.fromCallable(() -> {
            PDDocument document = new PDDocument();
            final PDRectangle pageSize = new PDRectangle(PDRectangle.LETTER.getHeight(), PDRectangle.LETTER.getWidth());

            for (int i = 0; i < markedImages.size(); i += 3) {
                PDPage page = new PDPage(pageSize);
                document.addPage(page);
                PDPageContentStream contentStream = new PDPageContentStream(document, page);

                float availablePageHeight = pageSize.getHeight() - (2 * 20f);
                float slotHeight = (availablePageHeight - (2 * 20f)) / 3;

                for (int j = 0; j < 3 && (i + j) < markedImages.size(); j++) {
                    BufferedImage awtImage = markedImages.get(i + j);
                    PDImageXObject pdImage = convertToPdfImage(awtImage, document);

                    float slotWidth = pageSize.getWidth() - (2 * 20f);
                    float scale = Math.min(slotWidth / pdImage.getWidth(), slotHeight / pdImage.getHeight());
                    float scaledWidth = pdImage.getWidth() * scale;
                    float scaledHeight = pdImage.getHeight() * scale;

                    float slotStartY = pageSize.getHeight() - 20f - (j * (slotHeight + 20f));
                    float yPos = slotStartY - scaledHeight;
                    float xPos = (pageSize.getWidth() - scaledWidth) / 2;

                    contentStream.drawImage(pdImage, xPos, yPos, scaledWidth, scaledHeight);

                    if (j < 2) {
                        float lineY = yPos - (20f / 2);
                        drawDashedLine(contentStream, 20f, lineY, pageSize.getWidth() - 20f, lineY);
                    }
                }
                contentStream.close();
            }
            return document;
        }).subscribeOn(Schedulers.boundedElastic());
    }

    private BufferedImage convertToGrayscale(BufferedImage source) {
        BufferedImage grayImage = new BufferedImage(source.getWidth(), source.getHeight(), BufferedImage.TYPE_BYTE_GRAY);
        Graphics2D g2d = grayImage.createGraphics();
        g2d.drawImage(source, 0, 0, null);
        g2d.dispose();
        return grayImage;
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
