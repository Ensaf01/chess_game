package com.example.mychess;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;

public class DBUtil {
    public static Connection getConnection() throws Exception {
        // Load MySQL JDBC Driver
        Class.forName("com.mysql.cj.jdbc.Driver");

        // Connection details
        String url = "jdbc:mysql://localhost:3306/my_chess?useSSL=false&serverTimezone=UTC";
        String user = "root";
        String password = "";

        return DriverManager.getConnection(url, user, password);
    }

    public static void markChallengeAccepted(String senderUsername, String receiverUsername) {
        String sql = """
        UPDATE game_requests
        SET status = 'accept'
        WHERE sender_id = (SELECT id FROM players WHERE username = ?)
          AND receiver_id = (SELECT id FROM players WHERE username = ?)
          AND status = 'pending'
        """;

        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, senderUsername);   // challenger
            stmt.setString(2, receiverUsername); // accepting player

            int rows = stmt.executeUpdate();
            System.out.println("Challenge status updated: " + rows + " row(s).");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }




}
