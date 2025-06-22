package com.example.mychess;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class DashboardController {

    @FXML private Label welcomeLabel;
    @FXML private Label statsLabel;
    @FXML private ListView<String> playersListView;

    private int loggedInUserId;
    private String loggedInUsername;

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
            Alert alert = new Alert(Alert.AlertType.WARNING, "Please select a player to challenge.");
            alert.show();
            return;
        }

        try (Connection conn = DBUtil.getConnection()) {
            // Get receiver ID
            String query = "SELECT id FROM players WHERE username = ?";
            try (PreparedStatement stmt = conn.prepareStatement(query)) {
                stmt.setString(1, selectedPlayer);
                ResultSet rs = stmt.executeQuery();
                if (rs.next()) {
                    int receiverId = rs.getInt("id");

                    String insert = "INSERT INTO game_requests (sender_id, receiver_id) VALUES (?, ?)";
                    try (PreparedStatement ins = conn.prepareStatement(insert)) {
                        ins.setInt(1, loggedInUserId);
                        ins.setInt(2, receiverId);
                        ins.executeUpdate();
                        new Alert(Alert.AlertType.INFORMATION, "Game request sent to " + selectedPlayer).show();
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            new Alert(Alert.AlertType.ERROR, "Failed to send request.").show();
        }
    }
}
