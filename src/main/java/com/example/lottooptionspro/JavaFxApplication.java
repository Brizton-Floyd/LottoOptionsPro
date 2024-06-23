package com.example.lottooptionspro;

import com.example.lottooptionspro.controller.MainController;
import com.example.lottooptionspro.models.LotteryGameBetSlipCoordinates;
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
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.URL;
import java.nio.file.Paths;
import java.util.ArrayList;
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
        String filePath = "Serialized Files/Texas/Powerball.ser";
        loadImageProgrammatically(filePath);
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
            LotteryGameBetSlipCoordinates coordinates = readCoordinatesFromFile(filePath);
            // Process the coordinates as needed
            System.out.println("Main Ball Coordinates: " + coordinates.getMainBallCoordinates());
            System.out.println("Bonus Ball Coordinates: " + coordinates.getBonusBallCoordinates());
            System.out.println("JackPot Coordinate: " + coordinates.getJackpotOptionCoordinate());
//            showAlert("Success", "Coordinates loaded successfully.");
        } catch (IOException | ClassNotFoundException e) {
//            showAlert("Error", "Cannot load coordinates: " + e.getMessage());
        }
    }

    public LotteryGameBetSlipCoordinates readCoordinatesFromFile(String filePath) throws IOException, ClassNotFoundException {
        try (FileInputStream fis = new FileInputStream(filePath);
             ObjectInputStream ois = new ObjectInputStream(fis)) {
            return (LotteryGameBetSlipCoordinates) ois.readObject();
        }
    }
}
