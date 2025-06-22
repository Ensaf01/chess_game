package com.example.mychess;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;

public class RegisterController implements javafx.fxml.Initializable {

    @FXML
    private javafx.scene.layout.VBox registerCard;

    @FXML
    private TextField usernameField;

    @FXML
    private PasswordField passwordField;

    @FXML
    private PasswordField confirmPasswordField;

    @FXML
    private Label statusLabel;
    @Override
    public void initialize(java.net.URL location, java.util.ResourceBundle resources) {
        // Animate the register card
        registerCard.setOpacity(0);
        registerCard.setTranslateY(40);

        javafx.animation.FadeTransition fade = new javafx.animation.FadeTransition(javafx.util.Duration.seconds(1), registerCard);
        fade.setFromValue(0);
        fade.setToValue(1);

        javafx.animation.TranslateTransition slide = new javafx.animation.TranslateTransition(javafx.util.Duration.seconds(1), registerCard);
        slide.setFromY(40);
        slide.setToY(0);

        fade.play();
        slide.play();
    }


    @FXML
    public void onRegisterButtonClick(ActionEvent event) {
        String username = usernameField.getText().trim();
        String password = passwordField.getText();
        String confirmPassword = confirmPasswordField.getText();

        if (username.isEmpty() || password.isEmpty()) {
            statusLabel.setText("Please enter username and password");
            return;
        }

        if (!password.equals(confirmPassword)) {
            statusLabel.setText("Passwords do not match");
            return;
        }

        try (Connection conn = DBUtil.getConnection()) {
            // Check if username exists
            var checkStmt = conn.prepareStatement("SELECT id FROM players WHERE username = ?");
            checkStmt.setString(1, username);
            var rs = checkStmt.executeQuery();
            if (rs.next()) {
                statusLabel.setText("Username already taken");
                return;
            }

            // Insert new user
            String sql = "INSERT INTO players (username, password) VALUES (?, ?)";
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, username);
                stmt.setString(2, password); // Ideally hash password in production
                int rows = stmt.executeUpdate();
                if (rows > 0) {
                    statusLabel.setText("Registration successful! Please login.");
                } else {
                    statusLabel.setText("Registration failed. Try again.");
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            statusLabel.setText("Error: " + e.getMessage());
        }
    }

    @FXML
    public void onBackToLoginClick(ActionEvent event) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/com/example/mychess/login.fxml"));
        Parent root = fxmlLoader.load();

        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        stage.setScene(new Scene(root));
        stage.setTitle("Login");

        stage.show();
    }
}
