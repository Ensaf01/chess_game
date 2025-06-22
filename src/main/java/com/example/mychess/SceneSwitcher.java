package com.example.mychess;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.event.ActionEvent;
import javafx.scene.Node;

import java.util.Objects;

public class SceneSwitcher {
    public static void switchTo(ActionEvent event, String fxmlFile) {
        try {
            Parent root = FXMLLoader.load(Objects.requireNonNull(SceneSwitcher.class.getResource(fxmlFile)));
            Stage stage = (Stage)((Node)event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
