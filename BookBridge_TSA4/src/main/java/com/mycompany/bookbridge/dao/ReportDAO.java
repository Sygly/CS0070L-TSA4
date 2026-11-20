package com.mycompany.bookbridge.dao;

import com.mycompany.bookbridge.db.DatabaseConnection;
import java.sql.*;

public class ReportDAO {

    public String generateReport() throws SQLException {

        StringBuilder report = new StringBuilder();

        report.append("========== BOOKBRIDGE TSA4 REPORT ==========\n\n");

        try (Connection connection =
                DatabaseConnection.getConnection()) {

            report.append("Total Registered Members: ")
                    .append(count(connection,
                            "SELECT COUNT(*) FROM members"))
                    .append("\n");

            report.append("Total Book Titles: ")
                    .append(count(connection,
                            "SELECT COUNT(*) FROM books"))
                    .append("\n");

            report.append("Total Book Copies: ")
                    .append(count(connection,
                            "SELECT COALESCE(SUM(total_copies), 0) FROM books"))
                    .append("\n");

            report.append("Available Copies: ")
                    .append(count(connection,
                            "SELECT COALESCE(SUM(available_copies), 0) FROM books"))
                    .append("\n");

            report.append("Checked-Out Copies: ")
                    .append(count(connection,
                            "SELECT COALESCE(SUM(total_copies - available_copies), 0) FROM books"))
                    .append("\n\n");

            report.append("Active Loans: ")
                    .append(count(connection,
                            "SELECT COUNT(*) FROM loans WHERE status = 'ACTIVE'"))
                    .append("\n");

            report.append("Returned Loans: ")
                    .append(count(connection,
                            "SELECT COUNT(*) FROM loans WHERE status = 'RETURNED'"))
                    .append("\n");

            report.append("Pending Reservations: ")
                    .append(count(connection,
                            "SELECT COUNT(*) FROM reservations WHERE status IN ('WAITING', 'READY')"))
                    .append("\n");

            report.append("Cancelled Reservations: ")
                    .append(count(connection,
                            "SELECT COUNT(*) FROM reservations WHERE status = 'CANCELLED'"))
                    .append("\n\n");

            report.append("Active Loans by Membership Type:\n");

            String memberTypeSql = """
                    SELECT m.membership_type, COUNT(*) AS total
                    FROM loans l
                    JOIN members m ON l.member_id = m.member_id
                    WHERE l.status = 'ACTIVE'
                    GROUP BY m.membership_type
                    """;

            try (PreparedStatement statement =
                    connection.prepareStatement(memberTypeSql);
                 ResultSet result = statement.executeQuery()) {

                java.util.Map<String, Integer> totals =
                        new java.util.LinkedHashMap<>();

                totals.put("STUDENT", 0);
                totals.put("FACULTY", 0);
                totals.put("GUEST", 0);

                while (result.next()) {
                    totals.put(
                            result.getString("membership_type"),
                            result.getInt("total"));
                }

                for (var entry : totals.entrySet()) {
                    report.append("  ")
                            .append(entry.getKey())
                            .append(": ")
                            .append(entry.getValue())
                            .append("\n");
                }
            }

            report.append("\nTotal Estimated Late Fines: PHP ");

            String finesSql = """
                    SELECT COALESCE(SUM(
                        GREATEST(
                            DATEDIFF(
                                COALESCE(return_date, CURRENT_DATE()),
                                due_date
                            ), 0
                        ) *
                        CASE m.membership_type
                            WHEN 'STUDENT' THEN 5
                            WHEN 'FACULTY' THEN 2
                            WHEN 'GUEST' THEN 10
                            ELSE 0
                        END
                    ), 0) AS total_fines
                    FROM loans l
                    JOIN members m ON l.member_id = m.member_id
                    """;

            try (PreparedStatement statement =
                    connection.prepareStatement(finesSql);
                 ResultSet result = statement.executeQuery()) {

                if (result.next()) {
                    report.append(result.getLong("total_fines"));
                }
            }
        }

        report.append("\n\n==========================================");

        return report.toString();
    }

    private long count(Connection connection, String sql)
            throws SQLException {

        try (PreparedStatement statement =
                connection.prepareStatement(sql);
             ResultSet result = statement.executeQuery()) {

            if (!result.next()) {
                throw new SQLException("Report query returned no result.");
            }

            return result.getLong(1);
        }
    }
}