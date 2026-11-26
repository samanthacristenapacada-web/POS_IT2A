package com.scc.dao;

import com.scc.model.ActivityLog;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class ActivityLogDao {

    // Record an action using a separate database connection.
    public static void logAction(
            int userId,
            String action,
            String tableAffected,
            String description) {

        String sql = """
                INSERT INTO Activity_Logs
                (UserID, Action, TableAffected, Description)
                VALUES (?, ?, ?, ?)
                """;

        try (Connection conn = DatabaseConnection.connect()) {
            logAction(conn, userId, action, tableAffected, description);

        } catch (SQLException e) {
            System.out.println("Audit log error: " + e.getMessage());
        }
    }

    // Record an action using an existing database connection.
    // Useful when the log must be part of another transaction.
    public static void logAction(
            Connection conn,
            int userId,
            String action,
            String tableAffected,
            String description) throws SQLException {

        String sql = """
                INSERT INTO Activity_Logs
                (UserID, Action, TableAffected, Description)
                VALUES (?, ?, ?, ?)
                """;

        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, userId);
            pstmt.setString(2, action);
            pstmt.setString(3, tableAffected);
            pstmt.setString(4, description);

            pstmt.executeUpdate();
        }
    }

    // Retrieve all audit logs.
    public static List<ActivityLog> getAllLogs() {

        List<ActivityLog> logs = new ArrayList<>();

        String sql = """
                SELECT LogID, UserID, Action, Description, Timestamp
                FROM Activity_Logs
                ORDER BY LogID DESC
                """;

        try (Connection conn = DatabaseConnection.connect();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {

            while (rs.next()) {
                logs.add(mapActivityLog(rs));
            }

        } catch (SQLException e) {
            System.out.println(
                    "Error retrieving audit logs: " + e.getMessage()
            );
        }

        return logs;
    }

    // Retrieve logs for a specific user.
    public static List<ActivityLog> getLogsByUserId(int userId) {

        List<ActivityLog> logs = new ArrayList<>();

        String sql = """
                SELECT LogID, UserID, Action, Description, Timestamp
                FROM Activity_Logs
                WHERE UserID = ?
                ORDER BY LogID DESC
                """;

        try (Connection conn = DatabaseConnection.connect();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, userId);

            try (ResultSet rs = pstmt.executeQuery()) {

                while (rs.next()) {
                    logs.add(mapActivityLog(rs));
                }
            }

        } catch (SQLException e) {
            System.out.println(
                    "Error filtering audit logs: " + e.getMessage()
            );
        }

        return logs;
    }

    private static ActivityLog mapActivityLog(ResultSet rs)
            throws SQLException {

        return new ActivityLog(
                rs.getInt("LogID"),
                rs.getInt("UserID"),
                rs.getString("Action"),
                rs.getString("Description"),
                rs.getString("Timestamp")
        );
    }
}