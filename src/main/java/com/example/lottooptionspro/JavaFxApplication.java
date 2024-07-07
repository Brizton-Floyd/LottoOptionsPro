package com.example.lottooptionspro;

import com.example.lottooptionspro.controller.MainController;
import com.example.lottooptionspro.models.LotteryGameBetSlipCoordinates;
import com.example.lottooptionspro.util.ImageResizer;
import javafx.application.Application;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import net.rgielen.fxweaver.core.FxWeaver;
import net.sourceforge.tess4j.*;

import org.bytedeco.opencv.opencv_core.*;
import org.bytedeco.opencv.opencv_imgproc.*;

import static org.bytedeco.opencv.global.opencv_core.*;
import static org.bytedeco.opencv.global.opencv_imgcodecs.*;
import static org.bytedeco.opencv.global.opencv_imgproc.*;

import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.Point;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.URL;
import java.nio.file.Paths;
import java.util.*;
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
        String filePath = "Serialized Files/Texas/CashFive.ser";
//        loadImageProgrammatically(filePath);
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

    @Override
    public void stop() {
        this.applicationContext.close();
    }

    // New method to load the image programmatically
    public void loadImageProgrammatically(String filePath) {
        try {
            List<List<Integer>> drawResults = generateLotteryDraws(5);
            LotteryGameBetSlipCoordinates coordinates = readCoordinatesFromFile(filePath);
            String imagePath = "src/main/resources/images/Texas/Cash Five.jpg";

            File imageFile = new File(imagePath);
            if (!imageFile.exists()) {
                throw new IOException("File not found: " + imagePath);
            }

            BufferedImage bufferedImage = ImageResizer.resizeImageBasedOnTrueSize(ImageIO.read(imageFile), 8.5, 3.5);
            Graphics2D graphics = bufferedImage.createGraphics();
            graphics.setColor(Color.BLACK);


            int idx = 0;
            int[] bonusNumbers = {23,1,5,10,7};
            for (List<Integer> drawResult : drawResults) {
                System.out.println(drawResult);
                for (Integer num : drawResult) {
                    Map<String, Point> dataPoints = coordinates.getMainBallCoordinates().get(idx);
                    Point point = dataPoints.get(String.valueOf(num));
                    int x = point.x;
                    int y = point.y;
                    graphics.fillRect(x, y, 13, 13);
                }

                idx++;
            }


            graphics.dispose();
            ImageIO.write(bufferedImage, "jpg", new File("src/main/resources/images/Texas/cash_five_marked_image.jpg"));
//            extractColor();
        } catch (IOException | ClassNotFoundException e) {
//            showAlert("Error", "Cannot load coordinates: " + e.getMessage());
        }
    }
    public static List<List<Integer>> generateLotteryDraws(int numberOfDraws) {
        List<List<Integer>> allDraws = new ArrayList<>();
        Random random = new Random();

        for (int i = 0; i < numberOfDraws; i++) {
            Set<Integer> drawSet = new HashSet<>();
            while (drawSet.size() < 5) {
                int number = random.nextInt(35) + 1;
                drawSet.add(number);
            }
            ArrayList<Integer> integers = new ArrayList<>(drawSet);
            Collections.sort(integers);
            allDraws.add(integers);
        }


        return allDraws;
    }

    public LotteryGameBetSlipCoordinates readCoordinatesFromFile(String filePath) throws IOException, ClassNotFoundException {
        try (FileInputStream fis = new FileInputStream(filePath);
             ObjectInputStream ois = new ObjectInputStream(fis)) {
            return (LotteryGameBetSlipCoordinates) ois.readObject();
        }
    }
}
