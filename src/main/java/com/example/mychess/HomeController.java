package com.example.mychess;

import javafx.event.ActionEvent;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.scene.Node;

public class HomeController {

    public void onLogin(ActionEvent event) throws Exception {
        Parent root = FXMLLoader.load(getClass().getResource("/com/example/mychess/login.fxml"));
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        stage.setScene(new Scene(root));
        stage.setTitle("Login");
        stage.show();
    }

    public void onRegister(ActionEvent event) throws Exception {
        Parent root = FXMLLoader.load(getClass().getResource("/com/example/mychess/register.fxml"));
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        stage.setScene(new Scene(root));
        stage.setTitle("Register");
        stage.show();
    }
}
