package com.mycompany.bookbridge.db;

import java.sql.Connection;
import java.sql.SQLException;

public class DatabaseConnectionTest {

    public static void main(String[] args) {

        try (Connection connection =
                DatabaseConnection.getConnection()) {

            System.out.println("BOOKBRIDGE TSA4");
            System.out.println("Database Connection: SUCCESS");
            System.out.println("Connected Database: "
                    + connection.getCatalog());

        } catch (SQLException e) {

            System.out.println("Database Connection: FAILED");
            System.out.println("Error: " + e.getMessage());
        }
    }
}