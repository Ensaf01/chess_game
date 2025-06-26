package com.example.mychess;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class DashboardController {

    @FXML private Label welcomeLabel;
    @FXML private Label statsLabel;
    @FXML private ListView<String> playersListView;

    public int loggedInUserId;
    public String loggedInUsername;

    public void initializeUser(int userId, String username) {
        this.loggedInUserId = userId;
        this.loggedInUsername = username;
        welcomeLabel.setText("Welcome, " + username + "!");

        loadPlayerStats();
        loadOtherPlayers();
    }

    private void loadPlayerStats() {
        try (Connection conn = DBUtil.getConnection()) {
            String sql = "SELECT wins, losses, category FROM players WHERE id = ?";
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setInt(1, loggedInUserId);
                ResultSet rs = stmt.executeQuery();
                if (rs.next()) {
                    int wins = rs.getInt("wins");
                    int losses = rs.getInt("losses");
                    String category = rs.getString("category");
                    statsLabel.setText("Wins: " + wins + ", Losses: " + losses + ", Category: " + category);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            statsLabel.setText("Failed to load stats.");
        }
    }

    private void loadOtherPlayers() {
        ObservableList<String> playerList = FXCollections.observableArrayList();

        try (Connection conn = DBUtil.getConnection()) {
            String sql = "SELECT username FROM players WHERE id != ?";
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setInt(1, loggedInUserId);
                ResultSet rs = stmt.executeQuery();
                while (rs.next()) {
                    playerList.add(rs.getString("username"));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            playersListView.getItems().add("Failed to load players.");
        }

        playersListView.setItems(playerList);
    }

    @FXML
    private void onSendRequestClick() {
        String selectedPlayer = playersListView.getSelectionModel().getSelectedItem();
        if (selectedPlayer == null) {
            new Alert(Alert.AlertType.WARNING, "Please select a player to challenge.").show();
            return;
        }

        try (Connection conn = DBUtil.getConnection()) {
            String query = "SELECT id FROM players WHERE username = ?";
            PreparedStatement stmt = conn.prepareStatement(query);
            stmt.setString(1, selectedPlayer);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                int receiverId = rs.getInt("id");

                // Insert game request
                String insert = "INSERT INTO game_requests (sender_id, receiver_id) VALUES (?, ?)";
                PreparedStatement ins = conn.prepareStatement(insert);
                ins.setInt(1, loggedInUserId);
                ins.setInt(2, receiverId);
                ins.executeUpdate();

                // Directly launch game for testing/demo purpose
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/mychess/game_view.fxml"));
                Parent root = loader.load();
                GameController controller = loader.getController();
                String logedInUsername = null;
                controller.setPlayers(logedInUsername, selectedPlayer, loggedInUserId, receiverId);


                Stage stage = (Stage) playersListView.getScene().getWindow();
                stage.setScene(new Scene(root, 900, 800));
                stage.setTitle("Chess Game");
                stage.show();

            }
        } catch (Exception e) {
            e.printStackTrace();
            new Alert(Alert.AlertType.ERROR, "Failed to send request.").show();
        }
    }

    @FXML
    private void onBackToLoginClick(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/mychess/login.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("Login");
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void onPlayWithMyselfClick(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/mychess/game_view.fxml"));
            Parent root = loader.load();

            GameController controller = loader.getController();

            // Pass same player ID for both white and black
            controller.setPlayers(loggedInUsername, loggedInUsername, loggedInUserId, loggedInUserId);


            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("Play With Myself");
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
