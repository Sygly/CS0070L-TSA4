package com.mycompany.bookbridge.dao;

import com.mycompany.bookbridge.db.DatabaseConnection;
import com.mycompany.bookbridge.model.LibraryBook;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class BookDAO {

    // Adds a new book to the catalog.
    public void addBook(LibraryBook book) throws SQLException {

        String sql = """
                INSERT INTO books
                (isbn, title, author, total_copies, available_copies)
                VALUES (?, ?, ?, ?, ?)
                """;

        try (Connection connection =
                     DatabaseConnection.getConnection();
             PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            statement.setString(1, book.getIsbn());
            statement.setString(2, book.getTitle());
            statement.setString(3, book.getAuthor());
            statement.setInt(4, book.getTotalCopies());
            statement.setInt(5, book.getAvailableCopies());

            int rows = statement.executeUpdate();

            if (rows != 1) {
                throw new SQLException(
                        "Book registration failed.");
            }
        }
    }

    // Finds a book using its ISBN.
    public Optional<LibraryBook> findByIsbn(String isbn)
            throws SQLException {

        String sql = """
                SELECT isbn, title, author,
                       total_copies, available_copies
                FROM books
                WHERE isbn = ?
                """;

        try (Connection connection =
                     DatabaseConnection.getConnection();
             PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            statement.setString(1, isbn);

            try (ResultSet result = statement.executeQuery()) {

                if (result.next()) {
                    return Optional.of(mapBook(result));
                }
            }
        }

        return Optional.empty();
    }

    // Searches books using a title keyword.
    public List<LibraryBook> searchByTitle(String keyword)
            throws SQLException {

        String sql = """
                SELECT isbn, title, author,
                       total_copies, available_copies
                FROM books
                WHERE LOWER(title) LIKE LOWER(?)
                ORDER BY title, isbn
                """;

        List<LibraryBook> books = new ArrayList<>();

        try (Connection connection =
                     DatabaseConnection.getConnection();
             PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            statement.setString(1, "%"
                    + escapeLike(keyword) + "%");

            try (ResultSet result = statement.executeQuery()) {

                while (result.next()) {
                    books.add(mapBook(result));
                }
            }
        }

        return books;
    }

    // Returns all books in the catalog.
    public List<LibraryBook> findAll() throws SQLException {

        String sql = """
                SELECT isbn, title, author,
                       total_copies, available_copies
                FROM books
                ORDER BY title, isbn
                """;

        List<LibraryBook> books = new ArrayList<>();

        try (Connection connection =
                     DatabaseConnection.getConnection();
             PreparedStatement statement =
                     connection.prepareStatement(sql);
             ResultSet result = statement.executeQuery()) {

            while (result.next()) {
                books.add(mapBook(result));
            }
        }

        return books;
    }

    // Maps a database row to a LibraryBook object.
    private LibraryBook mapBook(ResultSet result)
            throws SQLException {

        return new LibraryBook(
                result.getString("isbn"),
                result.getString("title"),
                result.getString("author"),
                result.getInt("total_copies"),
                result.getInt("available_copies")
        );
    }

    // Treats wildcard characters as literal search text.
    private String escapeLike(String value) {

        if (value == null) {
            throw new IllegalArgumentException(
                    "Search keyword cannot be null.");
        }

        return value.replace("\\", "\\\\")
                .replace("%", "\\%")
                .replace("_", "\\_");
    }
}