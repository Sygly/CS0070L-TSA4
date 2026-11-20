package com.mycompany.bookbridge.dao;

import com.mycompany.bookbridge.db.DatabaseConnection;
import com.mycompany.bookbridge.model.*;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class LoanDAO {

    private final MemberDAO memberDAO = new MemberDAO();
    private final BookDAO bookDAO = new BookDAO();

    // Issues a loan using one database transaction.
    public int issueLoan(String memberId, String isbn,
                         int requestedDays) throws SQLException {

        if (memberId == null || memberId.isBlank()
                || isbn == null || isbn.isBlank()) {
            throw new IllegalArgumentException(
                    "Member ID and ISBN are required.");
        }

        if (requestedDays < 1 || requestedDays > 30) {
            throw new IllegalArgumentException(
                    "Requested days must be between 1 and 30.");
        }

        Member member = memberDAO.findById(memberId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Member not found: " + memberId));

        if (requestedDays > member.getMaximumLoanDays()) {
            throw new IllegalArgumentException(
                    "Loan limit exceeded for "
                    + member.getMembershipType());
        }

        LocalDate issueDate = LocalDate.now();
        LocalDate dueDate = issueDate.plusDays(requestedDays);

        try (Connection connection =
                     DatabaseConnection.getConnection()) {

            connection.setAutoCommit(false);

            try {
                
                // Check reservation priority before issuing a loan.
                String reservationSql = """
                        SELECT member_id
                        FROM reservations
                        WHERE isbn = ?
                          AND status IN ('WAITING', 'READY')
                        ORDER BY reservation_date ASC,
                                 reservation_id ASC
                        LIMIT 1
                        """;

                try (PreparedStatement statement =
                             connection.prepareStatement(reservationSql)) {

                    statement.setString(1, isbn);

                    try (ResultSet result = statement.executeQuery()) {

                        if (result.next()) {
                            String firstMember =
                                    result.getString("member_id");

                            if (!firstMember.equals(memberId)) {
                                throw new IllegalStateException(
                                        "Book is reserved for another member.");
                            }
                        }
                    }
                }
                // Atomically claim one available copy.
                String updateBook = """
                        UPDATE books
                        SET available_copies = available_copies - 1
                        WHERE isbn = ?
                          AND available_copies > 0
                        """;

                try (PreparedStatement statement =
                             connection.prepareStatement(updateBook)) {

                    statement.setString(1, isbn);

                    if (statement.executeUpdate() != 1) {
                        throw new IllegalStateException(
                                "Book unavailable or not found: " + isbn);
                    }
                }

                // Prevent duplicate active loans for this member/book.
                String duplicateSql = """
                        SELECT loan_id FROM loans
                        WHERE member_id = ?
                          AND isbn = ?
                          AND status = 'ACTIVE'
                        FOR UPDATE
                        """;

                try (PreparedStatement statement =
                             connection.prepareStatement(duplicateSql)) {

                    statement.setString(1, memberId);
                    statement.setString(2, isbn);

                    try (ResultSet result = statement.executeQuery()) {
                        if (result.next()) {
                            throw new IllegalStateException(
                                    "Member already has an active loan.");
                        }
                    }
                }

                String insertLoan = """
                        INSERT INTO loans
                        (member_id, isbn, issue_date, due_date, status)
                        VALUES (?, ?, ?, ?, 'ACTIVE')
                        """;

                int loanId;

                try (PreparedStatement statement =
                             connection.prepareStatement(
                                     insertLoan,
                                     Statement.RETURN_GENERATED_KEYS)) {

                    statement.setString(1, memberId);
                    statement.setString(2, isbn);
                    statement.setDate(3, Date.valueOf(issueDate));
                    statement.setDate(4, Date.valueOf(dueDate));

                    if (statement.executeUpdate() != 1) {
                        throw new SQLException("Loan creation failed.");
                    }

                    try (ResultSet keys =
                                 statement.getGeneratedKeys()) {

                        if (!keys.next()) {
                            throw new SQLException(
                                    "Loan ID was not generated.");
                        }

                        loanId = keys.getInt(1);
                    }
                }

                connection.commit();
                return loanId;

            } catch (SQLException | RuntimeException ex) {
                rollback(connection, ex);
                throw ex;
            }
        }
    }

    // Returns a borrowed book and restores one available copy.
    public void returnBook(int loanId) throws SQLException {

        if (loanId <= 0) {
            throw new IllegalArgumentException(
                    "Loan ID must be positive.");
        }

        try (Connection connection =
                     DatabaseConnection.getConnection()) {

            connection.setAutoCommit(false);

            try {
                String findLoan = """
                        SELECT isbn, status
                        FROM loans
                        WHERE loan_id = ?
                        FOR UPDATE
                        """;

                String isbn;

                try (PreparedStatement statement =
                             connection.prepareStatement(findLoan)) {

                    statement.setInt(1, loanId);

                    try (ResultSet result = statement.executeQuery()) {
                        if (!result.next()) {
                            throw new IllegalArgumentException(
                                    "Loan not found: " + loanId);
                        }

                        if (!"ACTIVE".equals(result.getString("status"))) {
                            throw new IllegalStateException(
                                    "Loan has already been returned.");
                        }

                        isbn = result.getString("isbn");
                    }
                }

                String updateLoan = """
                        UPDATE loans
                        SET status = 'RETURNED',
                            return_date = CURRENT_DATE()
                        WHERE loan_id = ?
                          AND status = 'ACTIVE'
                        """;

                try (PreparedStatement statement =
                             connection.prepareStatement(updateLoan)) {

                    statement.setInt(1, loanId);

                    if (statement.executeUpdate() != 1) {
                        throw new SQLException(
                                "Loan return update failed.");
                    }
                }

                String updateBook = """
                        UPDATE books
                        SET available_copies = available_copies + 1
                        WHERE isbn = ?
                          AND available_copies < total_copies
                        """;

                try (PreparedStatement statement =
                             connection.prepareStatement(updateBook)) {

                    statement.setString(1, isbn);

                    if (statement.executeUpdate() != 1) {
                        throw new SQLException(
                                "Book inventory update failed.");
                    }
                }

                connection.commit();

            } catch (SQLException | RuntimeException ex) {
                rollback(connection, ex);
                throw ex;
            }
        }
    }

    // Retrieves the current loan records for JTable display.
    public List<Object[]> findAll() throws SQLException {

        String sql = """
                SELECT l.loan_id, l.member_id, m.full_name,
                       l.isbn, b.title, l.issue_date,
                       l.due_date, l.return_date, l.status
                FROM loans l
                JOIN members m ON l.member_id = m.member_id
                JOIN books b ON l.isbn = b.isbn
                ORDER BY l.loan_id DESC
                """;

        List<Object[]> records = new ArrayList<>();

        try (Connection connection =
                     DatabaseConnection.getConnection();
             PreparedStatement statement =
                     connection.prepareStatement(sql);
             ResultSet result = statement.executeQuery()) {

            while (result.next()) {
                records.add(new Object[]{
                    result.getInt("loan_id"),
                    result.getString("member_id"),
                    result.getString("full_name"),
                    result.getString("isbn"),
                    result.getString("title"),
                    result.getDate("issue_date"),
                    result.getDate("due_date"),
                    result.getDate("return_date"),
                    result.getString("status")
                });
            }
        }

        return records;
    }

    // Preserves the original exception if rollback also fails.
    private void rollback(Connection connection, Exception original) {
        try {
            connection.rollback();
        } catch (SQLException rollbackError) {
            original.addSuppressed(rollbackError);
        }
    }
}