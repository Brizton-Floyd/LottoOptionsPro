package com.example.lottooptionspro;

import com.example.lottooptionspro.controller.MainController;
import com.example.lottooptionspro.models.LotteryGameBetSlipCoordinates;
import javafx.application.Application;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import net.rgielen.fxweaver.core.FxWeaver;

import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.Point;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.*;
import java.util.List;

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
