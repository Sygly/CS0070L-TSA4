package com.mycompany.bookbridge.dao;

import com.mycompany.bookbridge.db.DatabaseConnection;
import com.mycompany.bookbridge.model.Member;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ReservationDAO {

    private final MemberDAO memberDAO = new MemberDAO();

    // Creates a reservation for an unavailable book.
    public int reserveBook(String memberId, String isbn)
            throws SQLException {

        if (memberId == null || memberId.isBlank()
                || isbn == null || isbn.isBlank()) {
            throw new IllegalArgumentException(
                    "Member ID and ISBN are required.");
        }

        Member member = memberDAO.findById(memberId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Member not found: " + memberId));

        try (Connection connection =
                     DatabaseConnection.getConnection()) {

            connection.setAutoCommit(false);

            try {
                // Lock the book row during reservation validation.
                String bookSql = """
                        SELECT available_copies
                        FROM books
                        WHERE isbn = ?
                        FOR UPDATE
                        """;

                try (PreparedStatement statement =
                             connection.prepareStatement(bookSql)) {

                    statement.setString(1, isbn);

                    try (ResultSet result = statement.executeQuery()) {

                        if (!result.next()) {
                            throw new IllegalArgumentException(
                                    "Book not found: " + isbn);
                        }

                        if (result.getInt("available_copies") > 0) {
                            throw new IllegalStateException(
                                    "Book is available. Issue a loan instead.");
                        }
                    }
                }
                
                // Prevent members from reserving books
                // they currently have on active loan.
                String activeLoanSql = """
                        SELECT loan_id
                        FROM loans
                        WHERE member_id = ?
                          AND isbn = ?
                          AND status = 'ACTIVE'
                        LIMIT 1
                        """;

                try (PreparedStatement statement =
                             connection.prepareStatement(activeLoanSql)) {

                    statement.setString(1, memberId);
                    statement.setString(2, isbn);

                    try (ResultSet result = statement.executeQuery()) {

                        if (result.next()) {
                            throw new IllegalStateException(
                                    "You cannot reserve a book "
                                    + "you currently have on loan.");
                        }
                    }
                }

                // Prevent duplicate pending reservations.
                String duplicateSql = """
                        SELECT reservation_id
                        FROM reservations
                        WHERE member_id = ?
                          AND isbn = ?
                          AND status IN ('WAITING', 'READY')
                        FOR UPDATE
                        """;

                try (PreparedStatement statement =
                             connection.prepareStatement(duplicateSql)) {

                    statement.setString(1, memberId);
                    statement.setString(2, isbn);

                    try (ResultSet result = statement.executeQuery()) {

                        if (result.next()) {
                            throw new IllegalStateException(
                                    "Member already has a pending reservation.");
                        }
                    }
                }

                String insertSql = """
                        INSERT INTO reservations
                        (member_id, isbn, status)
                        VALUES (?, ?, 'WAITING')
                        """;

                int reservationId;

                try (PreparedStatement statement =
                             connection.prepareStatement(
                                     insertSql,
                                     Statement.RETURN_GENERATED_KEYS)) {

                    statement.setString(1, memberId);
                    statement.setString(2, isbn);

                    if (statement.executeUpdate() != 1) {
                        throw new SQLException(
                                "Reservation creation failed.");
                    }

                    try (ResultSet keys =
                                 statement.getGeneratedKeys()) {

                        if (!keys.next()) {
                            throw new SQLException(
                                    "Reservation ID was not generated.");
                        }

                        reservationId = keys.getInt(1);
                    }
                }

                connection.commit();
                return reservationId;

            } catch (SQLException | RuntimeException ex) {
                rollback(connection, ex);
                throw ex;
            }
        }
    }

    // Cancels a pending reservation.
    public void cancelReservation(int reservationId)
            throws SQLException {

        if (reservationId <= 0) {
            throw new IllegalArgumentException(
                    "Reservation ID must be positive.");
        }

        String sql = """
                UPDATE reservations
                SET status = 'CANCELLED'
                WHERE reservation_id = ?
                  AND status IN ('WAITING', 'READY')
                """;

        try (Connection connection =
                     DatabaseConnection.getConnection();
             PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            statement.setInt(1, reservationId);

            if (statement.executeUpdate() != 1) {
                throw new IllegalStateException(
                        "Reservation not found or cannot be cancelled.");
            }
        }
    }

    // Retrieves reservations for JTable display.
    public List<Object[]> findAll() throws SQLException {

        String sql = """
                SELECT r.reservation_id, r.member_id,
                       m.full_name, r.isbn, b.title,
                       r.reservation_date, r.status
                FROM reservations r
                JOIN members m ON r.member_id = m.member_id
                JOIN books b ON r.isbn = b.isbn
                ORDER BY r.reservation_date DESC,
                         r.reservation_id DESC
                """;

        List<Object[]> reservations = new ArrayList<>();

        try (Connection connection =
                     DatabaseConnection.getConnection();
             PreparedStatement statement =
                     connection.prepareStatement(sql);
             ResultSet result = statement.executeQuery()) {

            while (result.next()) {
                reservations.add(new Object[]{
                    result.getInt("reservation_id"),
                    result.getString("member_id"),
                    result.getString("full_name"),
                    result.getString("isbn"),
                    result.getString("title"),
                    result.getTimestamp("reservation_date"),
                    result.getString("status")
                });
            }
        }

        return reservations;
    }

    // Preserves rollback failures as suppressed exceptions.
    private void rollback(Connection connection, Exception original) {
        try {
            connection.rollback();
        } catch (SQLException rollbackError) {
            original.addSuppressed(rollbackError);
        }
    }
}