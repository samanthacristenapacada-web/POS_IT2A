
package com.scc.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class InventoryLogDao {

    // Save an inventory movement using an existing connection.
    // This allows the log to be committed or rolled back together
    // with the stock update.
    public static void logMovement(
            Connection conn,
            Integer productId,
            String productName,
            Integer userId,
            String movementType,
            int quantityChanged,
            int previousStock,
            int newStock,
            String reason) throws SQLException {

        String sql = """
            INSERT INTO Inventory_Logs (
                ProductID,
                ProductName,
                UserID,
                MovementType,
                QuantityChanged,
                PreviousStock,
                NewStock,
                Reason
            )
            VALUES (?, ?, ?, ?, ?, ?, ?, ?)
        """;

        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {

            if (productId == null) {
                pstmt.setNull(1, java.sql.Types.INTEGER);
            } else {
                pstmt.setInt(1, productId);
            }

            pstmt.setString(2, productName);

            if (userId == null) {
                pstmt.setNull(3, java.sql.Types.INTEGER);
            } else {
                pstmt.setInt(3, userId);
            }

            pstmt.setString(4, movementType);
            pstmt.setInt(5, quantityChanged);
            pstmt.setInt(6, previousStock);
            pstmt.setInt(7, newStock);
            pstmt.setString(8, reason);

            pstmt.executeUpdate();
        }
    }

    // Display the latest inventory movements.
    public static void viewInventoryHistory() {

        String sql = """
            SELECT
                InventoryLogID,
                ProductID,
                ProductName,
                UserID,
                MovementType,
                QuantityChanged,
                PreviousStock,
                NewStock,
                Reason,
                Timestamp
            FROM Inventory_Logs
            ORDER BY InventoryLogID DESC
            LIMIT 100
        """;

        try (Connection conn = DatabaseConnection.connect();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {

            System.out.println("\n========== INVENTORY MOVEMENT HISTORY ==========");

            System.out.printf(
                    "%-6s %-8s %-20s %-18s %-8s %-8s %-20s%n",
                    "LOG ID",
                    "PROD ID",
                    "PRODUCT",
                    "MOVEMENT",
                    "CHANGE",
                    "STOCK",
                    "DATE"
            );

            System.out.println(
                    "--------------------------------------------------------------------------"
            );

            boolean found = false;

            while (rs.next()) {

                found = true;

                String productId = rs.getString("ProductID");
                String productName = shorten(
                        rs.getString("ProductName"), 19
                );
                String movement = shorten(
                        rs.getString("MovementType"), 17
                );
                String date = rs.getString("Timestamp");

                System.out.printf(
                        "%-6d %-8s %-20s %-18s %-8d %d -> %-4d %-20s%n",
                        rs.getInt("InventoryLogID"),
                        productId == null ? "Deleted" : productId,
                        productName,
                        movement,
                        rs.getInt("QuantityChanged"),
                        rs.getInt("PreviousStock"),
                        rs.getInt("NewStock"),
                        date
                );

                System.out.println(
                        "       Reason: " + rs.getString("Reason")
                );
            }

            if (!found) {
                System.out.println("No inventory history found.");
            }

            System.out.println(
                    "================================================"
            );

        } catch (SQLException e) {
            System.out.println(
                    "Error retrieving inventory history: " + e.getMessage()
            );
        }
    }

    private static String shorten(String value, int maxLength) {

        if (value == null) {
            return "N/A";
        }

        if (value.length() > maxLength) {
            return value.substring(0, maxLength - 1) + ".";
        }

        return value;
    }
}
