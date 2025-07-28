package com.copperpenguin96.spigotconfig;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;

public class ConfigApplication extends Application {
    @Override
    public void start(Stage stage) throws IOException {
        URL main = getClass().getResource("MainScreen.fxml");
        FXMLLoader fxmlLoader = new FXMLLoader(main);
        Scene scene = new Scene(fxmlLoader.load());
        stage.setTitle("Spigot Minecart Configuration");
        stage.setScene(scene);
        stage.show();


    }

    public static void main(String[] args) {
        launch();
    }
}