package com.example.mychess;

import java.sql.Connection;
import java.sql.DriverManager;

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
}
