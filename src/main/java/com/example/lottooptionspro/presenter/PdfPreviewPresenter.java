package com.example.lottooptionspro.presenter;

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

import javax.imageio.ImageIO;
import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
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
    private List<BufferedImage> currentImages;

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
                currentImages = new ArrayList<>(originalColorImages);
                return currentImages.stream()
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

    public void onColorModeChanged(boolean isGrayscale) {
        if (originalColorImages == null) return;

        Task<List<Image>> conversionTask = new Task<>() {
            @Override
            protected List<Image> call() {
                if (isGrayscale) {
                    currentImages = originalColorImages.stream()
                            .map(PdfPreviewPresenter.this::convertToGrayscale)
                            .collect(Collectors.toList());
                } else {
                    currentImages = new ArrayList<>(originalColorImages);
                }
                return currentImages.stream()
                        .map(img -> SwingFXUtils.toFXImage(img, null))
                        .collect(Collectors.toList());
            }
        };

        conversionTask.setOnSucceeded(event -> view.displayPdfPages(conversionTask.getValue()));
        conversionTask.setOnFailed(event -> view.showError("Failed to convert color mode."));

        new Thread(conversionTask).start();
    }

    public void save() {
        String initialFileName = "betslips.pdf";
        File file = view.showSavePdfDialog(initialFileName);
        if (file == null) return;

        view.showProgress(true);
        Task<Void> saveTask = new Task<>() {
            @Override
            protected Void call() throws Exception {
                try (PDDocument docToSave = createPdfFromBufferedImages(currentImages)) {
                    docToSave.save(file);
                }
                return null;
            }
        };

        saveTask.setOnSucceeded(event -> {
            view.showProgress(false);
            view.closeView();
        });
        saveTask.setOnFailed(event -> {
            view.showProgress(false);
            view.showError("Failed to save PDF: " + saveTask.getException().getMessage());
        });

        new Thread(saveTask).start();
    }

    private BufferedImage convertToGrayscale(BufferedImage source) {
        BufferedImage grayImage = new BufferedImage(source.getWidth(), source.getHeight(), BufferedImage.TYPE_BYTE_GRAY);
        Graphics2D g2d = grayImage.createGraphics();
        g2d.drawImage(source, 0, 0, null);
        g2d.dispose();
        return grayImage;
    }

    private PDDocument createPdfFromBufferedImages(List<BufferedImage> markedImages) throws IOException {
        PDDocument document = new PDDocument();
        final PDRectangle pageSize = new PDRectangle(PDRectangle.LETTER.getHeight(), PDRectangle.LETTER.getWidth());

        for (int i = 0; i < markedImages.size(); i += 3) {
            PDPage page = new PDPage(pageSize);
            document.addPage(page);
            PDPageContentStream contentStream = new PDPageContentStream(document, page);

            float availablePageHeight = pageSize.getHeight() - 40f;
            float slotHeight = (availablePageHeight - (2 * 20f)) / 3;

            for (int j = 0; j < 3 && (i + j) < markedImages.size(); j++) {
                BufferedImage awtImage = markedImages.get(i + j);
                PDImageXObject pdImage = convertToPdfImage(awtImage, document);

                float slotWidth = pageSize.getWidth() - 40f;
                float scale = Math.min(slotWidth / pdImage.getWidth(), slotHeight / pdImage.getHeight());
                float scaledWidth = pdImage.getWidth() * scale;
                float scaledHeight = pdImage.getHeight() * scale;

                float slotStartY = pageSize.getHeight() - 20f - (j * (slotHeight + 20f));
                float yPos = slotStartY - scaledHeight;
                float xPos = (pageSize.getWidth() - scaledWidth) / 2;

                contentStream.drawImage(pdImage, xPos, yPos, scaledWidth, scaledHeight);

                if (j < 2) {
                    float lineY = yPos - 10f;
                    contentStream.setLineDashPattern(new float[]{10, 5}, 0);
                    contentStream.setStrokingColor(java.awt.Color.RED);
                    contentStream.moveTo(20f, lineY);
                    contentStream.lineTo(pageSize.getWidth() - 20f, lineY);
                    contentStream.stroke();
                }
            }
            contentStream.close();
        }
        return document;
    }

    private PDImageXObject convertToPdfImage(BufferedImage awtImage, PDDocument document) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(awtImage, "png", out);
        out.flush();
        ByteArrayInputStream in = new ByteArrayInputStream(out.toByteArray());
        return PDImageXObject.createFromByteArray(document, in.readAllBytes(), "png");
    }

    public void cancel() {
        view.closeView();
    }
}
