package com.mycompany.bookbridge.dao;

import com.mycompany.bookbridge.db.DatabaseConnection;
import com.mycompany.bookbridge.model.*;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class MemberDAO {

    public void registerMember(Member member) throws SQLException {
        String sql = """
                INSERT INTO members
                (member_id, full_name, membership_type)
                VALUES (?, ?, ?)
                """;

        try (Connection connection =
                     DatabaseConnection.getConnection();
             PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            statement.setString(1, member.getMemberId());
            statement.setString(2, member.getMemberName());
            statement.setString(3,
                    member.getMembershipType().name());

            int rows = statement.executeUpdate();

            if (rows != 1) {
                throw new SQLException(
                        "Member registration failed.");
            }
        }
    }

    public Optional<Member> findById(String memberId)
            throws SQLException {

        String sql = """
                SELECT member_id, full_name, membership_type
                FROM members WHERE member_id = ?
                """;

        try (Connection connection =
                     DatabaseConnection.getConnection();
             PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            statement.setString(1, memberId);

            try (ResultSet result = statement.executeQuery()) {
                if (result.next()) {
                    return Optional.of(mapMember(result));
                }
            }
        }

        return Optional.empty();
    }

    public List<Member> findAll() throws SQLException {
        String sql = """
                SELECT member_id, full_name, membership_type
                FROM members ORDER BY member_id
                """;

        List<Member> members = new ArrayList<>();

        try (Connection connection =
                     DatabaseConnection.getConnection();
             PreparedStatement statement =
                     connection.prepareStatement(sql);
             ResultSet result = statement.executeQuery()) {

            while (result.next()) {
                members.add(mapMember(result));
            }
        }

        return members;
    }

    private Member mapMember(ResultSet result)
            throws SQLException {

        String id = result.getString("member_id");
        String name = result.getString("full_name");
        MembershipType type = MembershipType.valueOf(
                result.getString("membership_type"));

        return switch (type) {
            case STUDENT -> new StudentMember(id, name);
            case FACULTY -> new FacultyMember(id, name);
            case GUEST -> new GuestMember(id, name);
        };
    }
}