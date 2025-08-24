package com.example.lottooptionspro.presenter;

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
import javafx.print.PrinterJob;
import javafx.scene.image.ImageView;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import org.apache.commons.lang3.StringUtils;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class LotteryBetSlipPresenter {
    private final LotteryBetSlipView view;
    private List<List<int[]>> numbers;
    private PDDocument document;
    private PDFRenderer renderer;
    private String stateName;
    private String gameName;

    public LotteryBetSlipPresenter(LotteryBetSlipView view) {
        this.view = view;
    }

    public void setData(List<List<int[]>> partitionedNumbers, String stateName, String gameName) {
        this.numbers = partitionedNumbers;
        this.stateName = stateName;
        this.gameName = gameName;
        processAndStoreImages();
    }

    private void processAndStoreImages() {
        try {
            String gameNameNoWhiteSpace = StringUtils.deleteWhitespace(gameName);
            String filePath = "Serialized Files/" + stateName + "/" + gameNameNoWhiteSpace + ".ser";
            LotteryGameBetSlipCoordinates coordinates = readCoordinatesFromFile(filePath);
            String imagePath = "src/main/resources/images/" + stateName + "/Processed_" + gameName + ".jpg";

            File imageFile = new File(imagePath);
            if (!imageFile.exists()) {
                throw new IOException("File not found: " + imagePath);
            }
            BufferedImage originalImage = ImageResizer.resizeImageBasedOnTrueSize(ImageIO.read(imageFile), 8.5, 3.5);

            Map<Integer, Map<String, Point>> mainBallCoordinates = coordinates.getMainBallCoordinates();
            Map<Integer, Map<String, Point>> bonusBallCoordinates = coordinates.getBonusBallCoordinates();

            List<CompletableFuture<BufferedImage>> futures = numbers.stream()
                    .map(numberList -> CompletableFuture.supplyAsync(() ->
                            processImage(originalImage, numberList, mainBallCoordinates, bonusBallCoordinates,
                                    coordinates.getJackpotOptionCoordinate(), coordinates.getMarkingSize())
                    ))
                    .collect(Collectors.toList());

            List<BufferedImage> bufferedImages = futures.stream()
                    .map(CompletableFuture::join)
                    .collect(Collectors.toList());

            saveImagesToPDF(bufferedImages, stateName, gameName);

        } catch (IOException | ClassNotFoundException e) {
            view.showError(e.getMessage());
            e.printStackTrace();
        }
    }

    private BufferedImage processImage(BufferedImage originalImage, List<int[]> numberList,
                                       Map<Integer, Map<String, Point>> mainBallCoordinates,
                                       Map<Integer, Map<String, Point>> bonusBallCoordinates,
                                       Point jackpotOptionCoordinate, double markingSize) {
        try {
            BufferedImage bufferedImage = new BufferedImage(originalImage.getWidth(), originalImage.getHeight(), originalImage.getType());
            Graphics2D graphics = bufferedImage.createGraphics();
            graphics.drawImage(originalImage, 0, 0, null);
            graphics.setColor(Color.BLACK);

            if (jackpotOptionCoordinate != null) {
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
            return bufferedImage;
        } catch (Exception e) {
            view.showError(e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    private int getMaxBonusNumber(Set<String> numbers) {
        return numbers.stream().mapToInt(Integer::parseInt).max().orElse(Integer.MIN_VALUE);
    }

    public void saveImagesToPDF(List<BufferedImage> bufferedImages, String stateName, String gameName) throws IOException {
        String dest = String.format("output/%s_%s_betslips.pdf", stateName, gameName);
        File outputDir = new File("output");
        if (!outputDir.exists()) {
            outputDir.mkdirs();
        }

        try (PdfWriter writer = new PdfWriter(dest); PdfDocument pdfDoc = new PdfDocument(writer); Document document = new Document(pdfDoc, PageSize.LETTER.rotate())) {
            document.setMargins(0, 50, 0, 50);
            float pageWidth = pdfDoc.getDefaultPageSize().getWidth();
            float pageHeight = pdfDoc.getDefaultPageSize().getHeight();
            float availableWidth = pageWidth - 100;
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

                float x = 50 + (imageCount % imagesPerPage) * imageWidth;
                float y = pageHeight - imageHeight;

                pdfImage.setFixedPosition(x, y);
                pdfImage.setWidth(imageWidth);
                pdfImage.setHeight(imageHeight);

                document.add(pdfImage);

                PdfCanvas canvas = new PdfCanvas(pdfDoc.getLastPage());
                canvas.setStrokeColor(ColorConstants.RED).setLineDash(3, 3).moveTo(x, 0).lineTo(x, pageHeight).stroke();
                canvas.setStrokeColor(ColorConstants.RED).setLineDash(3, 3).moveTo(x + imageWidth, 0).lineTo(x + imageWidth, pageHeight).stroke();
                imageCount++;
            }

            for (int i = 1; i < (bufferedImages.size() + imagesPerPage - 1) / imagesPerPage; i++) {
                float y = pageHeight - i * imageHeight;
                PdfCanvas canvas = new PdfCanvas(pdfDoc.getLastPage());
                canvas.setStrokeColor(ColorConstants.RED).setLineDash(3, 3).moveTo(0, y).lineTo(pageWidth, y).stroke();
            }
        } catch (IOException e) {
            view.showError(e.getMessage());
            e.printStackTrace();
        }

        loadPDFForViewing(dest);
    }

    private LotteryGameBetSlipCoordinates readCoordinatesFromFile(String filePath) throws IOException, ClassNotFoundException {
        try (FileInputStream fis = new FileInputStream(filePath); ObjectInputStream ois = new ObjectInputStream(fis)) {
            return (LotteryGameBetSlipCoordinates) ois.readObject();
        }
    }

    private void loadPDFForViewing(String pdfPath) {
        File file = new File(pdfPath);
        if (!file.exists()) {
            view.showError("File does not exist: " + pdfPath);
            return;
        }

        try (FileInputStream fis = new FileInputStream(file)) {
            document = PDDocument.load(fis);
            renderer = new PDFRenderer(document);
            displayPDFPage(0);
        } catch (IOException e) {
            view.showError("Error loading PDF: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void displayPDFPage(int pageIndex) {
        try {
            BufferedImage image = renderer.renderImage(pageIndex);
            javafx.scene.image.Image fxImage = convertToFxImage(image);
            view.displayPdfPage(fxImage);
        } catch (IOException e) {
            view.showError(e.getMessage());
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

    public void printPDF() {
        PrinterJob job = PrinterJob.createPrinterJob();
        if (job != null && job.showPrintDialog(view.getStage())) {
            boolean success = job.printPage(view.getContainerNodeForPrinting());
            if (success) {
                job.endJob();
            }
        }
    }

    public void extractColorAndRegenerate() {
        if (document == null) {
            view.showError("No document loaded to extract color from.");
            return;
        }
        try {
            List<BufferedImage> extractedImages = new ArrayList<>();
            for (int i = 0; i < document.getNumberOfPages(); i++) {
                BufferedImage image = renderer.renderImage(i);
                BufferedImage extractedImage = extractColor(image);
                extractedImages.add(extractedImage);
            }
            saveImagesToPDF(extractedImages, stateName + "_ExtractedColor", gameName);
        } catch (IOException e) {
            view.showError("Error during color extraction: " + e.getMessage());
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
