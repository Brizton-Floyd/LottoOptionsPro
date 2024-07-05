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
import com.itextpdf.kernel.pdf.canvas.draw.DottedLine;
import com.itextpdf.kernel.pdf.canvas.draw.SolidLine;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.AreaBreak;
import com.itextpdf.layout.element.LineSeparator;

import javafx.fxml.FXML;
import javafx.scene.Scene;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import net.rgielen.fxweaver.core.FxmlView;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import com.itextpdf.layout.element.Image;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
@FxmlView("/com.example.lottooptionspro/controller/BetSlipView.fxml")
public class LotteryBetSlipController {
    private List<List<int[]>> numbers;
    private Stage stage;
    @FXML
    private VBox betslipsContainer;

    @FXML
    public void initialize() {
        this.stage = new Stage();
        stage.setScene(new Scene(betslipsContainer));
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
            String imagePath = "src/main/resources/images/" + stateName + "/" + gameName + ".jpg";

            File imageFile = new File(imagePath);
            if (!imageFile.exists()) {
                throw new IOException("File not found: " + imagePath);
            }

            Map<Integer, Map<String, Point>> mainBallCoordinates = coordinates.getMainBallCoordinates();
            Map<Integer, Map<String, Point>> bonusBallCoordinates = coordinates.getBonusBallCoordinates();

            List<BufferedImage> bufferedImages = new ArrayList<>();
            int count = 0;
            for (List<int[]> numberList : numbers) {
                BufferedImage bufferedImage = ImageResizer.resizeImageBasedOnTrueSize(ImageIO.read(imageFile), 8.5, 3.5);
                Graphics2D graphics = bufferedImage.createGraphics();
                graphics.setColor(Color.BLACK);

                int panel = 0;
                for (int[] numbers : numberList) {
                    for (int number : numbers) {
                        int x = mainBallCoordinates.get(panel).get(String.valueOf(number)).x;
                        int y = mainBallCoordinates.get(panel).get(String.valueOf(number)).y;
                        graphics.fillRect(x, y, 13, 13);
                    }

                    if (!bonusBallCoordinates.isEmpty()) {
//                        int x = mainBallCoordinates.get(panel).get(String.valueOf(number)).x;
//                        int y = mainBallCoordinates.get(panel).get(String.valueOf(number)).y;
//                        graphics.fillRect(x, y, 13, 13);
                    }
                    panel++;
                }

//                ImageIO.write(bufferedImage, "jpg", new File("src/main/resources/images/Texas/cash_five_marked_image" + count++ + ".jpg"));
                graphics.dispose();
                bufferedImages.add(bufferedImage);
            }

            saveImagesToPDF(bufferedImages, stateName, gameName);

        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
    }
    public void saveImagesToPDF(List<BufferedImage> bufferedImages, String stateName, String gameName) throws IOException {
        String dest = String.format("output/%s_%s_betslips.pdf", stateName, gameName);
        File outputDir = new File("output");
        if (!outputDir.exists()) {
            outputDir.mkdirs();
        }

        try (PdfWriter writer = new PdfWriter(dest);
             PdfDocument pdfDoc = new PdfDocument(writer);
             Document document = new Document(pdfDoc, PageSize.A4.rotate())) {

            // Set margins to 0
            document.setMargins(0, 0, 0, 0);

            float pageWidth = pdfDoc.getDefaultPageSize().getWidth();
            float pageHeight = pdfDoc.getDefaultPageSize().getHeight();
            float imageWidth = pageWidth / 3;
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

                // Calculate position for the image
                float x = (imageCount % imagesPerPage) * imageWidth;
                float y = pageHeight - imageHeight;

                pdfImage.setFixedPosition(x, y);
                pdfImage.setWidth(imageWidth);
                pdfImage.setHeight(imageHeight);

                document.add(pdfImage);

                // Add vertical red dotted line after each image (except the last one on the page)
                if ((imageCount + 1) % imagesPerPage != 0) {
                    PdfCanvas canvas = new PdfCanvas(pdfDoc.getLastPage());
                    canvas.setStrokeColor(ColorConstants.RED)
                            .setLineDash(3, 3)
                            .moveTo(x + imageWidth, 0)
                            .lineTo(x + imageWidth, pageHeight)
                            .stroke();
                }

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
}
