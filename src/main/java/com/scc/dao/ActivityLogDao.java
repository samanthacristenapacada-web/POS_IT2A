package com.scc.dao;

import com.scc.model.ActivityLog;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ActivityLogDao {

    // ===================================
    // INSERT NEW LOG
    // ===================================
    public static void logAction(
            int userId,
            String actionType,
            String tableAffected,
            String description) {

        String sql = """
                INSERT INTO Activity_Logs
                (UserID, Action, TableAffected, Description)
                VALUES (?, ?, ?, ?)
                """;

        try (Connection conn = DatabaseConnection.connect();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, userId);
            pstmt.setString(2, actionType.toUpperCase());
            pstmt.setString(3, tableAffected);
            pstmt.setString(4, description);

            pstmt.executeUpdate();

        } catch (SQLException e) {

            System.out.println(
                    "Log Error: "
                            + e.getMessage());
        }
    }

    // ===================================
    // VIEW ALL LOGS
    // ===================================
    public static List<ActivityLog> getAllLogs() {

        List<ActivityLog> logs =
                new ArrayList<>();

        String sql = """
                SELECT *
                FROM Activity_Logs
                ORDER BY Timestamp DESC
                """;

        try (Connection conn = DatabaseConnection.connect();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {

            while (rs.next()) {

                logs.add(
                        new ActivityLog(
                                rs.getInt("LogID"),
                                rs.getInt("UserID"),
                                rs.getString("Action"),
                                rs.getString("TableAffected"),
                                rs.getString("Description"),
                                rs.getString("Timestamp")
                        )
                );
            }

        } catch (SQLException e) {

            System.out.println(
                    "Fetch Logs Error: "
                            + e.getMessage());
        }

        return logs;
    }

    // ===================================
    // FILTER LOGS BY USER ID
    // ===================================
    public static List<ActivityLog> getLogsByUserId(
            int userId) {

        List<ActivityLog> logs =
                new ArrayList<>();

        String sql = """
                SELECT *
                FROM Activity_Logs
                WHERE UserID = ?
                ORDER BY Timestamp DESC
                """;

        try (Connection conn = DatabaseConnection.connect();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, userId);

            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {

                logs.add(
                        new ActivityLog(
                                rs.getInt("LogID"),
                                rs.getInt("UserID"),
                                rs.getString("Action"),
                                rs.getString("TableAffected"),
                                rs.getString("Description"),
                                rs.getString("Timestamp")
                        )
                );
            }

        } catch (SQLException e) {

            System.out.println(
                    "Filter Logs Error: "
                            + e.getMessage());
        }

        return logs;
    }

    // ===================================
    // CLEAR ALL LOGS
    // ===================================
    public static boolean clearLogs() {

        String sql =
                "DELETE FROM Activity_Logs";

        try (Connection conn = DatabaseConnection.connect();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            return pstmt.executeUpdate() >= 0;

        } catch (SQLException e) {

            System.out.println(
                    "Clear Logs Error: "
                            + e.getMessage());
        }

        return false;
    }
}