
package com.scc.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class InventoryDao {

    // ==========================================
    // OPTION 1: VIEW INVENTORY
    // ==========================================
    public static void viewInventory(int userId) {

        String sql = """
            SELECT ProductID, ProductName, Category,
                   UnitPrice, StockQuantity, Description
            FROM Products
            ORDER BY ProductID
            """;

        try (Connection conn = DatabaseConnection.connect();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {

            System.out.println("\n================ INVENTORY ================");

            System.out.printf("%-8s %-22s %-15s %-14s %-10s%n",
                    "ID", "PRODUCT", "CATEGORY", "PRICE", "STOCK");

            System.out.println("------------------------------------------------"
                    + "----------");

            boolean found = false;

            while (rs.next()) {

                found = true;

                System.out.printf("%-8d %-22s %-15s PHP %-9.2f %-10d%n",
                        rs.getInt("ProductID"),
                        shorten(rs.getString("ProductName"), 21),
                        shorten(rs.getString("Category"), 14),
                        rs.getDouble("UnitPrice"),
                        rs.getInt("StockQuantity"));
            }

            if (!found) {
                System.out.println("No products found.");
            }

            System.out.println("============================================");

            ActivityLogDao.logAction(
                    userId,
                    "VIEW_INVENTORY",
                    "Products",
                    "Inventory staff viewed product inventory."
            );

        } catch (SQLException e) {
            System.out.println("Error viewing inventory: "
                    + e.getMessage());
        }
    }

    // ==========================================
    // OPTION 2: UPDATE PRODUCT DETAILS
    // ==========================================
    public static void updateProduct(
            int userId,
            int productId,
            String productName,
            String category,
            double unitPrice,
            String description) {

        String sql = """
            UPDATE Products
            SET ProductName = ?,
                Category = ?,
                UnitPrice = ?,
                Description = ?
            WHERE ProductID = ?
            """;

        try (Connection conn = DatabaseConnection.connect();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, productName);
            pstmt.setString(2, category);
            pstmt.setDouble(3, unitPrice);
            pstmt.setString(4, description);
            pstmt.setInt(5, productId);

            int rows = pstmt.executeUpdate();

            if (rows > 0) {

                System.out.println("Product updated successfully!");

                // Record the successful update.
                ActivityLogDao.logAction(
                        userId,
                        "PRODUCT_UPDATED",
                        "Products",
                        "Inventory staff updated Product ID " + productId
                                + ". New name: " + productName
                                + ", Category: " + category
                                + ", Price: PHP " + unitPrice
                );

            } else {
                System.out.println("Product ID not found.");
            }

        } catch (SQLException e) {
            System.out.println("Error updating product: "
                    + e.getMessage());
        }
    }

    // ==========================================
    // OPTION 3: DELETE PRODUCT
    // ==========================================
    public static void deleteProduct(int userId, int productId) {

        String checkSql = """
            SELECT COUNT(*)
            FROM TransactionDetails
            WHERE ProductID = ?
            """;

        String deleteSql = """
            DELETE FROM Products
            WHERE ProductID = ?
            """;

        try (Connection conn = DatabaseConnection.connect()) {

            conn.setAutoCommit(false);

            try {

                // Check whether the product has transaction history.
                try (PreparedStatement check =
                             conn.prepareStatement(checkSql)) {

                    check.setInt(1, productId);

                    try (ResultSet rs = check.executeQuery()) {

                        if (rs.next() && rs.getInt(1) > 0) {

                            System.out.println(
                                    "Deletion denied! This product has "
                                            + "existing transaction records."
                            );

                            conn.rollback();
                            return;
                        }
                    }
                }

                // Delete the product if it has no transaction history.
                try (PreparedStatement delete =
                             conn.prepareStatement(deleteSql)) {

                    delete.setInt(1, productId);

                    int rows = delete.executeUpdate();

                    if (rows > 0) {

                        conn.commit();

                        System.out.println(
                                "Product deleted successfully!"
                        );

                        // Record the successful deletion.
                        ActivityLogDao.logAction(
                                userId,
                                "PRODUCT_DELETED",
                                "Products",
                                "Inventory staff deleted Product ID "
                                        + productId
                        );

                    } else {

                        conn.rollback();

                        System.out.println("Product ID not found.");
                    }
                }

            } catch (SQLException e) {

                conn.rollback();
                throw e;

            } finally {
                conn.setAutoCommit(true);
            }

        } catch (SQLException e) {
            System.out.println("Error deleting product: "
                    + e.getMessage());
        }
    }

    // ==========================================
    // HELPER METHOD
    // ==========================================
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
