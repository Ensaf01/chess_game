package com.example.mychess;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.scene.Node;
import javafx.scene.control.ListView;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class HomeController {

    @FXML
    private ListView<String> proPlayerListView; // Link with FXML

    @FXML
    public void initialize() {
        loadProPlayers();
    }

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


    private void loadProPlayers() {
        ObservableList<String> players = FXCollections.observableArrayList();

        try (Connection conn = DBUtil.getConnection()) {
            String sql = "SELECT username, wins FROM players ORDER BY wins DESC LIMIT 5";
            PreparedStatement stmt = conn.prepareStatement(sql);
            ResultSet rs = stmt.executeQuery();

            int rank = 1;
            while (rs.next()) {
                String username = rs.getString("username");
                int wins = rs.getInt("wins");

                String prefix = switch (rank) {
                    case 1 -> "👑 ";
                    case 2 -> "🥈 ";
                    case 3 -> "🥉 ";
                    default -> "🎖 ";
                };

                players.add(prefix + username + " - " + wins + " Wins");
                rank++;
            }

            proPlayerListView.setItems(players);
        } catch (Exception e) {
            e.printStackTrace();
            proPlayerListView.getItems().add("Failed to load players.");
        }
    }

    //rony
}
