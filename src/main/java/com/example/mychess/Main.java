package com.example.mychess;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class Main extends Application {
    @Override
    public void start(Stage primaryStage) throws Exception {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/mychess/home.fxml"));
        Scene scene = new Scene(loader.load());
        primaryStage.setTitle("Welcome to MyChess");
        primaryStage.setScene(scene);
        primaryStage.setResizable(false); // You can set true if you want resizable
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
