package com.scc.dao;

import com.scc.model.User;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class UserDao {

    // ===================================
    // REGISTER USER
    // ===================================
    public static boolean register(
            String fullName,
            String username,
            String password,
            String role,
            String contactNumber) {

        String sql = """
                INSERT INTO Users
                (FullName, Username, Password, Role,
                 ContactNumber, Status)
                VALUES (?, ?, ?, ?, ?, ?)
                """;

        try (Connection conn = DatabaseConnection.connect();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, fullName);
            pstmt.setString(2, username);
            pstmt.setString(3, password);
            pstmt.setString(4, role.toUpperCase());
            pstmt.setString(5, contactNumber);
            pstmt.setString(6, "PENDING");

            pstmt.executeUpdate();

            return true;

        } catch (SQLException e) {

            System.out.println(
                    "Register Error: "
                            + e.getMessage());

            return false;
        }
    }

    // ===================================
    // LOGIN
    // ===================================
    public static User login(
            String username,
            String password) {

        String sql = """
                SELECT *
                FROM Users
                WHERE Username = ?
                AND Password = ?
                AND Status = 'ACTIVE'
                """;

        try (Connection conn = DatabaseConnection.connect();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, username);
            pstmt.setString(2, password);

            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                return mapUser(rs);
            }

        } catch (SQLException e) {

            System.out.println(
                    "Login Error: "
                            + e.getMessage());
        }

        return null;
    }

    // ===================================
    // VIEW ALL USERS
    // ===================================
    public static List<User> getAllUsers() {

        List<User> users =
                new ArrayList<>();

        String sql =
                "SELECT * FROM Users";

        try (Connection conn = DatabaseConnection.connect();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {

            while (rs.next()) {
                users.add(mapUser(rs));
            }

        } catch (SQLException e) {

            System.out.println(
                    "Fetch Users Error: "
                            + e.getMessage());
        }

        return users;
    }

    // ===================================
    // VIEW PENDING USERS
    // ===================================
    public static List<User> getPendingUsers() {

        List<User> users =
                new ArrayList<>();

        String sql = """
                SELECT *
                FROM Users
                WHERE Status = 'PENDING'
                """;

        try (Connection conn = DatabaseConnection.connect();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {

            while (rs.next()) {
                users.add(mapUser(rs));
            }

        } catch (SQLException e) {

            System.out.println(
                    "Pending Users Error: "
                            + e.getMessage());
        }

        return users;
    }

    // ===================================
    // UPDATE STATUS ONLY
    // ===================================
    public static boolean updateUserStatus(
            int userId,
            String status) {

        String sql = """
                UPDATE Users
                SET Status = ?
                WHERE UserID = ?
                """;

        try (Connection conn = DatabaseConnection.connect();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(
                    1,
                    status.toUpperCase());

            pstmt.setInt(
                    2,
                    userId);

            return pstmt.executeUpdate() > 0;

        } catch (SQLException e) {

            System.out.println(
                    "Status Update Error: "
                            + e.getMessage());
        }

        return false;
    }

    // ===================================
    // UPDATE ROLE + STATUS
    // ===================================
    public static boolean updateUserRoleAndStatus(
            int userId,
            String newRole,
            String newStatus) {

        String sql = """
                UPDATE Users
                SET Role = ?,
                    Status = ?
                WHERE UserID = ?
                """;

        try (Connection conn = DatabaseConnection.connect();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(
                    1,
                    newRole.toUpperCase());

            pstmt.setString(
                    2,
                    newStatus.toUpperCase());

            pstmt.setInt(
                    3,
                    userId);

            return pstmt.executeUpdate() > 0;

        } catch (SQLException e) {

            System.out.println(
                    "Role/Status Update Error: "
                            + e.getMessage());
        }

        return false;
    }

    // ===================================
    // DELETE USER
    // ===================================
    public static boolean deleteUser(
            int userId) {

        String sql =
                "DELETE FROM Users WHERE UserID = ?";

        try (Connection conn = DatabaseConnection.connect();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, userId);

            return pstmt.executeUpdate() > 0;

        } catch (SQLException e) {

            System.out.println(
                    "Delete User Error: "
                            + e.getMessage());
        }

        return false;
    }

    // ===================================
    // MAP RESULTSET TO USER OBJECT
    // ===================================
    private static User mapUser(
            ResultSet rs) throws SQLException {

        return new User(
                rs.getInt("UserID"),
                rs.getString("FullName"),
                rs.getString("Username"),
                rs.getString("Password"),
                rs.getString("Role"),
                rs.getString("ContactNumber"),
                rs.getString("Status")
        );
    }
}