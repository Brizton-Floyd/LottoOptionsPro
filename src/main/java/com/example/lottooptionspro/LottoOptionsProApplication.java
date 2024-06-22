package com.example.lottooptionspro;

import javafx.application.Application;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
public class LottoOptionsProApplication {

	public static void main(String[] args) {
		System.setProperty("java.awt.headless", "false");
		Application.launch(JavaFxApplication.class, args);
	}

}
