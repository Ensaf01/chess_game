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

import javafx.application.Platform;



public class DashboardController {
    private SocketClient socketClient;
    private int whitePlayerId;
    private int blackPlayerId;
    //public boolean gameControllerRef;
    private GameController gameControllerRef;

    @FXML
    Label welcomeLabel;
    @FXML private Label statsLabel;
    @FXML private ListView<String> playersListView;

    public int loggedInUserId;
    public String loggedInUsername;

    public DashboardController() throws IOException {
    }
    public void initializeUser(int userId, String username) {
        this.loggedInUserId = userId;
        this.loggedInUsername = username;
        welcomeLabel.setText("Welcome, " + username + "!");

        loadStats();
        loadOtherPlayers();

        new Thread(() -> {
            try {
                socketClient = new SocketClient("localhost", 5555, username, new SocketClient.MessageListener() {
                    @Override
                    public void onChallengeReceived(String fromUser) {
                        Platform.runLater(() -> showChallengeDialog(fromUser));
                    }

                    @Override
                    public void onChallengeResponse(String fromUser, String response) {
                        Platform.runLater(() -> {
                            if ("ACCEPT".equalsIgnoreCase(response)) {
                                openGameWindow(loggedInUsername, fromUser); // Challenger side
                            } else {
                                new Alert(Alert.AlertType.INFORMATION, fromUser + " declined your challenge.").show();
                            }
                        });
                    }

                    @Override
                    public void onMoveReceived(String fromUser, String moveData) {
                        if (gameControllerRef != null) {
                            System.out.println("[GameController] onMoveReceived from " + fromUser + ": " + moveData);

                            Platform.runLater(() -> gameControllerRef.processOpponentMove(moveData));
                        } else {
                            System.err.println("No active gameControllerRef to deliver move");
                        }

                    }

                    @Override
                    public void onStartGame(String opponentUsername) {

                    }
                });
                System.out.println("[Dashboard] Connected to socket server");
            } catch (IOException e) {
                e.printStackTrace();
                Platform.runLater(() -> new Alert(Alert.AlertType.ERROR, "Failed to connect to server").show());
            }
        }).start();
    }

    @FXML
    private void onRefreshPlayersClick() {
        loadOtherPlayers();
    }



//new


    private void handleIncomingMessage(String message) {
        // Runs in background thread, update UI on FX thread
        Platform.runLater(() -> {
            System.out.println("[Dashboard] Message received: " + message);
            // Example message formats:
            // CHALLENGE_REQUEST:<challengerUsername>
            // CHALLENGE_ACCEPTED:<accepterUsername>
            // CHALLENGE_DECLINED:<declinerUsername>

            if (message.startsWith("CHALLENGE_REQUEST:")) {
                String challenger = message.substring("CHALLENGE_REQUEST:".length());
                showChallengeDialog(challenger);

            } else if (message.startsWith("CHALLENGE_ACCEPTED:")) {
                String accepter = message.substring("CHALLENGE_ACCEPTED:".length());
                if (accepter.equals(playersListView.getSelectionModel().getSelectedItem())) {
                    openGameWindow(loggedInUsername, accepter);
                }

            } else if (message.startsWith("CHALLENGE_DECLINED:")) {
                String decliner = message.substring("CHALLENGE_DECLINED:".length());
                new Alert(Alert.AlertType.INFORMATION, decliner + " declined your challenge.").show();
            }
        });
    }

    private void showChallengeDialog(String challenger) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Game Challenge");
        alert.setHeaderText(null);
        alert.setContentText(challenger + " has challenged you to a game. Accept?");

        ButtonType acceptButton = new ButtonType("Accept");
        ButtonType declineButton = new ButtonType("Decline");
        alert.getButtonTypes().setAll(acceptButton, declineButton);

        alert.showAndWait().ifPresent(response -> {
            if (response == acceptButton) {
                socketClient.sendMessage("CHALLENGE_RESPONSE:" + loggedInUsername + ":" + challenger + ":ACCEPT");

                // ✅ THIS IS THE MISSING PIECE
                openGameWindow(challenger, loggedInUsername); // White = challenger, You = black

            } else {
                socketClient.sendMessage("CHALLENGE_RESPONSE:" + loggedInUsername + ":" + challenger + ":DECLINE");
            }
        });
    }


    private void openGameWindow(String whitePlayer, String blackPlayer) {
        try {
            int whiteId = getPlayerIdByUsername(whitePlayer);
            int blackId = getPlayerIdByUsername(blackPlayer);
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/mychess/game_view.fxml"));
            Parent root = loader.load();
            GameController controller = loader.getController();
            gameControllerRef = controller;
            controller.setSocketClient(socketClient);
            controller.setLoggedInUsername(loggedInUsername);
            controller.setPlayers(whitePlayer, blackPlayer, whiteId, blackId);
            controller.setDashboardController(this);
            if (loggedInUsername.equals(whitePlayer)) {
                controller.setOpponentUsername(blackPlayer);
            } else {
                controller.setOpponentUsername(whitePlayer);
            }
            System.out.println("[Dashboard] gameControllerRef set for " + whitePlayer + " vs " + blackPlayer);

            Stage stage = new Stage();
            stage.setScene(new Scene(root, 900, 800));
            stage.setTitle("Chess Game: " + whitePlayer + " vs " + blackPlayer);
            stage.show();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private int getPlayerIdByUsername(String username) {
        try (Connection conn = DBUtil.getConnection()) {
            String sql = "SELECT id FROM players WHERE username = ?";
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setString(1, username);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getInt("id");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return -1; // fail-safe
    }


    //new
    private void loadStats() {
        try (Connection conn = DBUtil.getConnection()) {
            String sql = "SELECT wins, losses, category FROM players WHERE id = ?";
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setInt(1, loggedInUserId);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                int wins = rs.getInt("wins");
                int losses = rs.getInt("losses");
                String category = rs.getString("category");

                statsLabel.setText("Wins: " + wins + " | Losses: " + losses + " | Category: " + category);
            }
        } catch (Exception e) {
            e.printStackTrace();
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
    private void  onSendRequestClick() {

        String selectedPlayer = playersListView.getSelectionModel().getSelectedItem();
        if (selectedPlayer == null) {
            new Alert(Alert.AlertType.WARNING, "Please select a player to challenge.").show();
            return;
        }
        if (socketClient != null) {
            socketClient.sendMessage("CHALLENGE:" + loggedInUsername + ":" + selectedPlayer);
            new Alert(Alert.AlertType.INFORMATION, "Challenge sent to " + selectedPlayer).show();
        }

        try (Connection conn = DBUtil.getConnection()) {
            String query = "SELECT id FROM players WHERE username = ?";
            PreparedStatement stmt = conn.prepareStatement(query);
            stmt.setString(1, selectedPlayer);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                int receiverId = rs.getInt("id");
                String receiverUsername=rs.getString("name");

                // Insert game request
                String insert = "INSERT INTO game_requests (sender_id, receiver_id,receiverUsername) VALUES (?, ?)";
                PreparedStatement ins = conn.prepareStatement(insert);
                ins.setInt(1, loggedInUserId);
                ins.setInt(2, receiverId);
                ins.executeUpdate();



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

            // Set white as user, black as AI
            controller.setPlayers(loggedInUsername, "AI", loggedInUserId, -1);
            controller.setPlayerIds(loggedInUserId, -1);
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("Play With AI");
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    public void refreshDashboard() {
        loadStats();
        loadOtherPlayers();
    }

}