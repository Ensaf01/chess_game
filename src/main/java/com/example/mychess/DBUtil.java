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
        try (Connection conn = DBUtil.getConnection()) {
            String sql = """
            UPDATE game_requests
            SET status = 'accepted'
            WHERE sender_id = (SELECT id FROM players WHERE username = ?)
              AND receiver_id = (SELECT id FROM players WHERE username = ?)
              AND status = 'pending'
        """;

            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, senderUsername);   // challenger = sender
                stmt.setString(2, receiverUsername); // opponent = receiver

                int rows = stmt.executeUpdate();
                System.out.println("✅ markChallengeAccepted: updated rows = " + rows);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }



}
