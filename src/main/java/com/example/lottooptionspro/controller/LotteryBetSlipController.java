package com.example.lottooptionspro.controller;

import com.example.lottooptionspro.models.LotteryGameBetSlipCoordinates;
import com.example.lottooptionspro.util.ImageResizer;
import com.itextpdf.io.image.ImageData;
import com.itextpdf.io.image.ImageDataFactory;
import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.kernel.pdf.canvas.PdfCanvas;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.AreaBreak;
import com.itextpdf.layout.element.Image;
import javafx.fxml.FXML;
import javafx.print.PrinterJob;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.image.ImageView;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import net.rgielen.fxweaver.core.FxmlView;
import org.apache.commons.lang3.StringUtils;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.springframework.stereotype.Component;

import javax.imageio.ImageIO;
import javax.imageio.stream.ImageInputStream;
import java.awt.*;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.*;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Component
@FxmlView("/com.example.lottooptionspro/controller/BetSlipView.fxml")
public class LotteryBetSlipController {
    private List<List<int[]>> numbers;
    private Stage stage;
    private PDDocument document;
    private PDFRenderer renderer;

    @FXML
    private VBox mainContainer;
    @FXML
    private VBox betslipsContainer;
    @FXML
    private Button printButton;
    @FXML
    private Button extractColorButton;

    @FXML
    public void initialize() {
        this.stage = new Stage();
        stage.setScene(new Scene(mainContainer));

        printButton.setOnAction(e -> printPDF());
        extractColorButton.setOnAction(e -> extractColorAndRegenerate());
    }

    public void setData(List<List<int[]>> partitionedNumbers, String stateName, String gameName) {
        this.numbers = partitionedNumbers;
        processAndStoreImages(stateName, gameName);
    }

    private void processAndStoreImages(String stateName, String gameName) {
        try {
            String gameNameNoWhiteSpace = StringUtils.deleteWhitespace(gameName);
            String filePath = "Serialized Files/" + stateName + "/" + gameNameNoWhiteSpace + ".ser"; // Update with actual path

            LotteryGameBetSlipCoordinates coordinates = readCoordinatesFromFile(filePath);
            String imagePath = "src/main/resources/images/" + stateName + "/Processed_" + gameName + ".jpg";

            File imageFile = new File(imagePath);
            if (!imageFile.exists()) {
                throw new IOException("File not found: " + imagePath);
            }
            BufferedImage originalImage = ImageResizer.resizeImageBasedOnTrueSize(ImageIO.read(imageFile), 8.5, 3.5);

            Map<Integer, Map<String, Point>> mainBallCoordinates = coordinates.getMainBallCoordinates();
            Map<Integer, Map<String, Point>> bonusBallCoordinates = coordinates.getBonusBallCoordinates();
            // Use CompletableFuture for parallel processing
            List<CompletableFuture<BufferedImage>> futures = numbers.stream()
                    .map(numberList -> CompletableFuture.supplyAsync(() ->
                            processImage(originalImage, numberList, mainBallCoordinates, bonusBallCoordinates,
                                    coordinates.getJackpotOptionCoordinate(), coordinates.getMarkingSize())
                    ))
                    .collect(Collectors.toList());

            // Wait for all futures to complete and collect results
            List<BufferedImage> bufferedImages = futures.stream()
                    .map(CompletableFuture::join)
                    .collect(Collectors.toList());

            saveImagesToPDF(bufferedImages, stateName, gameName);

        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
    }
    private BufferedImage processImage(BufferedImage originalImage, List<int[]> numberList,
                                       Map<Integer, Map<String, Point>> mainBallCoordinates,
                                       Map<Integer, Map<String, Point>> bonusBallCoordinates,
                                       Point jackpotOptionCoordinate, double markingSize) {
        try {
            BufferedImage bufferedImage = new BufferedImage(originalImage.getWidth(), originalImage.getHeight(), originalImage.getType());
            synchronized (bufferedImage) {
                Graphics2D graphics = bufferedImage.createGraphics();
                graphics.drawImage(originalImage, 0, 0, null);
                graphics.setColor(Color.BLACK);

                if (bonusBallCoordinates != null && !bonusBallCoordinates.isEmpty()) {
                    graphics.fill(new Rectangle2D.Double(jackpotOptionCoordinate.x, jackpotOptionCoordinate.y, markingSize, markingSize));
                }

                IntStream.range(0, numberList.size()).forEach(panel -> {
                    int[] numbers = numberList.get(panel);
                    for (int number : numbers) {
                        int x = mainBallCoordinates.get(panel).get(String.valueOf(number)).x;
                        int y = mainBallCoordinates.get(panel).get(String.valueOf(number)).y;
                        graphics.fill(new Rectangle2D.Double(x, y, markingSize, markingSize));
                    }

                    if (bonusBallCoordinates != null && !bonusBallCoordinates.isEmpty()) {
                        int maxBonusNumber = getMaxBonusNumber(bonusBallCoordinates.get(0).keySet());
                        ThreadLocalRandom rnd = ThreadLocalRandom.current();
                        int bonusNumber = rnd.nextInt(maxBonusNumber) + 1;
                        int x = bonusBallCoordinates.get(panel).get(String.valueOf(bonusNumber)).x;
                        int y = bonusBallCoordinates.get(panel).get(String.valueOf(bonusNumber)).y;
                        graphics.fill(new Rectangle2D.Double(x, y, markingSize, markingSize));
                    }
                });

                graphics.dispose();
            }
            return bufferedImage;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private int getMaxBonusNumber(Set<String> numbers) {
        return numbers.stream()
                .mapToInt(Integer::parseInt)
                .max()
                .orElse(Integer.MIN_VALUE);
    }


    public void saveImagesToPDF(List<BufferedImage> bufferedImages, String stateName, String gameName) throws IOException {
        String dest = String.format("output/%s_%s_betslips.pdf", stateName, gameName);
        File outputDir = new File("output");
        if (!outputDir.exists()) {
            outputDir.mkdirs();
        }

        try (PdfWriter writer = new PdfWriter(dest);
             PdfDocument pdfDoc = new PdfDocument(writer);
             Document document = new Document(pdfDoc, PageSize.LETTER.rotate())) {

            // Set margins to 0
            document.setMargins(0, 40, 0, 40);

            float pageWidth = pdfDoc.getDefaultPageSize().getWidth();
            float pageHeight = pdfDoc.getDefaultPageSize().getHeight();
            float availableWidth = pageWidth - 80; // Subtract left and right margins
            float imageWidth = availableWidth / 3;
            float imageHeight = pageHeight;

            int imagesPerPage = 3;
            int imageCount = 0;

            for (BufferedImage bufferedImage : bufferedImages) {
                if (imageCount % imagesPerPage == 0 && imageCount != 0) {
                    document.add(new AreaBreak());
                }

                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                ImageIO.write(bufferedImage, "jpg", baos);
                ImageData imageData = ImageDataFactory.create(baos.toByteArray());
                Image pdfImage = new Image(imageData);

                // Calculate position for the image, accounting for left margin
                float x = 40 + (imageCount % imagesPerPage) * imageWidth;
                float y = pageHeight - imageHeight;

                pdfImage.setFixedPosition(x, y);
                pdfImage.setWidth(imageWidth);
                pdfImage.setHeight(imageHeight);

                document.add(pdfImage);

                // Add red dotted border around the image
//                PdfCanvas canvas = new PdfCanvas(pdfDoc.getLastPage());
//                canvas.setStrokeColor(ColorConstants.RED)
//                        .setLineDash(3, 3)
//                        .rectangle(x, y, imageWidth, imageHeight)
//                        .stroke();
                // Add vertical red dotted lines on the left and right side of each image
                PdfCanvas canvas = new PdfCanvas(pdfDoc.getLastPage());
                canvas.setStrokeColor(ColorConstants.RED)
                        .setLineDash(3, 3)
                        .moveTo(x, 0) // Left side of the image
                        .lineTo(x, pageHeight)
                        .stroke();
                canvas.setStrokeColor(ColorConstants.RED)
                        .setLineDash(3, 3)
                        .moveTo(x + imageWidth, 0) // Right side of the image
                        .lineTo(x + imageWidth, pageHeight)
                        .stroke();
                imageCount++;
            }

            // Add horizontal red dotted lines between rows of images
            for (int i = 1; i < (bufferedImages.size() + imagesPerPage - 1) / imagesPerPage; i++) {
                float y = pageHeight - i * imageHeight;
                PdfCanvas canvas = new PdfCanvas(pdfDoc.getLastPage());
                canvas.setStrokeColor(ColorConstants.RED)
                        .setLineDash(3, 3)
                        .moveTo(0, y)
                        .lineTo(pageWidth, y)
                        .stroke();
            }

        } catch (IOException e) {
            e.printStackTrace();
        }

        loadPDFForViewing(dest);
    }

    private LotteryGameBetSlipCoordinates readCoordinatesFromFile(String filePath) throws IOException, ClassNotFoundException {
        try (FileInputStream fis = new FileInputStream(filePath);
             ObjectInputStream ois = new ObjectInputStream(fis)) {
            return (LotteryGameBetSlipCoordinates) ois.readObject();
        }
    }
    public void show() {
        stage.show();
    }

    private void loadPDFForViewing(String pdfPath) {
//        try {
            File file = new File(pdfPath);
            if (!file.exists()) {
                System.out.println("File does not exist: " + pdfPath);
                return;
            }

            try (FileInputStream fis = new FileInputStream(file)) {
                document = PDDocument.load(fis);

                // PDF loaded successfully
                System.out.println("PDF loaded. Number of pages: " + document.getNumberOfPages());
                renderer = new PDFRenderer(document);
                displayPDFPage(0);
            } catch (IOException e) {
                System.err.println("Error loading PDF: " + e.getMessage());
                e.printStackTrace();
            }
    }

    private void displayPDFPage(int pageIndex) {
        try {
            BufferedImage image = renderer.renderImage(pageIndex);
            javafx.scene.image.Image fxImage = convertToFxImage(image);
            javafx.scene.image.ImageView imageView = new javafx.scene.image.ImageView(fxImage);
            betslipsContainer.getChildren().clear();
            betslipsContainer.getChildren().add(imageView);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static javafx.scene.image.Image convertToFxImage(BufferedImage image) {
        WritableImage wr = null;
        if (image != null) {
            wr = new WritableImage(image.getWidth(), image.getHeight());
            PixelWriter pw = wr.getPixelWriter();
            for (int x = 0; x < image.getWidth(); x++) {
                for (int y = 0; y < image.getHeight(); y++) {
                    pw.setArgb(x, y, image.getRGB(x, y));
                }
            }
        }
        return new ImageView(wr).getImage();
    }
    private void printPDF() {
        PrinterJob job = PrinterJob.createPrinterJob();
        if (job != null && job.showPrintDialog(stage)) {
            boolean success = job.printPage(betslipsContainer);
            if (success) {
                job.endJob();
            }
        }
    }

    private void extractColorAndRegenerate() {
        List<BufferedImage> extractedImages = new ArrayList<>();
        for (int i = 0; i < document.getNumberOfPages(); i++) {
            try {
                BufferedImage image = renderer.renderImage(i);
                BufferedImage extractedImage = extractColor(image);
                extractedImages.add(extractedImage);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        try {
            saveImagesToPDF(extractedImages, "ExtractedColor", "Game");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private BufferedImage extractColor(BufferedImage original) {
        int width = original.getWidth();
        int height = original.getHeight();
        BufferedImage result = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int rgb = original.getRGB(x, y);
                int r = (rgb >> 16) & 0xFF;
                int g = (rgb >> 8) & 0xFF;
                int b = rgb & 0xFF;

                // Simple color extraction: keep only strong colors
                if (r > 200 && g < 100 && b < 100) {
                    result.setRGB(x, y, rgb); // Keep red
                } else if (g > 200 && r < 100 && b < 100) {
                    result.setRGB(x, y, rgb); // Keep green
                } else if (b > 200 && r < 100 && g < 100) {
                    result.setRGB(x, y, rgb); // Keep blue
                } else {
                    result.setRGB(x, y, 0xFFFFFF); // Set to white
                }
            }
        }
        return result;
    }

}
