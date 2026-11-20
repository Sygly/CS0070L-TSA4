package com.mycompany.bookbridge.db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DatabaseConnection {

    private static final String URL =
            "jdbc:mariadb://localhost:3306/bookbridge_tsa4_db";

    private static final String USER =
            System.getenv().getOrDefault("BOOKBRIDGE_DB_USER", "root");

    private static final String PASSWORD =
            System.getenv().getOrDefault("BOOKBRIDGE_DB_PASSWORD", "");

    public static Connection getConnection()
            throws SQLException {

        return DriverManager.getConnection(
                URL, USER, PASSWORD);
    }
}