package com.example.lottooptionspro;

import com.example.lottooptionspro.controller.MainController;
import javafx.application.Application;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import net.rgielen.fxweaver.core.FxWeaver;
import net.sourceforge.tess4j.*;
import net.sourceforge.tess4j.util.LoadLibs;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Paths;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class JavaFxApplication extends Application {

    private ConfigurableApplicationContext applicationContext;

    @Override
    public void init() {
        String[] args = getParameters().getRaw().toArray(new String[0]);
        this.applicationContext = new SpringApplicationBuilder()
                .sources(LottoOptionsProApplication.class)
                .run(args);
    }

    @Override
    public void start(Stage stage) {
        FxWeaver fxWeaver = applicationContext.getBean(FxWeaver.class);
        Parent root = fxWeaver.loadView(MainController.class);
        Scene scene = new Scene(root);
        stage.setScene(scene);
        stage.setResizable(false);
        stage.setMinWidth(1280);
        stage.setMinHeight(720);
        stage.show();
        stage.setTitle("LottoOptionsPro");
        stage.show();
    }

    // Convert BufferedImage to Mat
    public static Mat bufferedImageToMat(BufferedImage bi) {
        Mat mat = new Mat(bi.getHeight(), bi.getWidth(), CvType.CV_8UC3);
        byte[] data = ((DataBufferByte) bi.getRaster().getDataBuffer()).getData();
        mat.put(0, 0, data);
        return mat;
    }

    // Convert Mat to BufferedImage
    public static BufferedImage matToBufferedImage(Mat mat) {
        int type = BufferedImage.TYPE_BYTE_GRAY;
        if (mat.channels() > 1) {
            type = BufferedImage.TYPE_3BYTE_BGR;
        }
        BufferedImage image = new BufferedImage(mat.width(), mat.height(), type);
        byte[] data = new byte[mat.rows() * mat.cols() * (int) (mat.elemSize())];
        mat.get(0, 0, data);
        image.getRaster().setDataElements(0, 0, mat.cols(), mat.rows(), data);
        return image;
    }

    private List<Word> detectNumbers(BufferedImage image) throws IOException {
        ITesseract tesseract = new Tesseract();
//        tesseract.setOcrEngineMode(1);
//        tesseract.setPageSegMode(6);
        tesseract.setDatapath("tessdata");
        tesseract.setLanguage("eng");
        tesseract.setTessVariable("tessedit_char_whitelist", "0123456789");

//        try {
////            String s = tesseract.doOCR(image);
//        } catch (TesseractException e) {
//            throw new RuntimeException(e);
//        }

        try {
            String s = tesseract.doOCR(image);
            return tesseract.getWords(image, ITessAPI.TessPageIteratorLevel.RIL_BLOCK);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public static BufferedImage markNumbers(BufferedImage image, List<Word> words) {
        Graphics2D g2d = image.createGraphics();
        g2d.setColor(Color.RED);
        g2d.setStroke(new BasicStroke(2));

        for (Word word : words) {
            if (word.getText().matches("\\d+")) { // Check if the word is a number
                Rectangle rect = word.getBoundingBox();
                g2d.drawRect(rect.x, rect.y, rect.width, rect.height);
            }
        }

        g2d.dispose();
        return image;
    }
    @Override
    public void stop() {
        this.applicationContext.close();
    }
}
