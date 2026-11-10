
package com.scc.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class ManagerDao {

    // ==========================================
    // OPTION 1: VIEW SALES REPORT
    // ==========================================
    public static void viewSalesReport(int managerId) {

        String sql = """
            SELECT
                COUNT(*) AS TotalTransactions,
                SUM(CASE
                    WHEN UPPER(TransactionStatus) = 'COMPLETED'
                    THEN 1 ELSE 0
                END) AS CompletedTransactions,
                SUM(CASE
                    WHEN UPPER(TransactionStatus) = 'VOIDED'
                    THEN 1 ELSE 0
                END) AS VoidedTransactions,
                COALESCE(SUM(
                    CASE
                        WHEN UPPER(TransactionStatus) = 'COMPLETED'
                        THEN TotalAmount ELSE 0
                    END
                ), 0) AS TotalSales
            FROM SalesTransactions
            """;

        try (Connection conn = DatabaseConnection.connect();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {

            if (rs.next()) {

                System.out.println("\n========== SALES REPORT ==========");

                System.out.printf("%-28s : %d%n",
                        "Total Transactions",
                        rs.getInt("TotalTransactions"));

                System.out.printf("%-28s : %d%n",
                        "Completed Transactions",
                        rs.getInt("CompletedTransactions"));

                System.out.printf("%-28s : %d%n",
                        "Voided Transactions",
                        rs.getInt("VoidedTransactions"));

                System.out.printf("%-28s : PHP %,.2f%n",
                        "Total Completed Sales",
                        rs.getDouble("TotalSales"));

                System.out.println("==================================");
            }

            ActivityLogDao.logAction(
                    managerId,
                    "VIEW_SALES_REPORT",
                    "SalesTransactions",
                    "Manager viewed the sales report."
            );

        } catch (SQLException e) {
            System.out.println("Error generating sales report: "
                    + e.getMessage());
        }
    }

    // ==========================================
    // OPTION 2: VIEW INVENTORY
    // ==========================================
    public static void viewInventory(int managerId) {

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

            System.out.println("--------------------------------------------"
                    + "----------------");

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
                    managerId,
                    "VIEW_INVENTORY",
                    "Products",
                    "Manager viewed product inventory."
            );

        } catch (SQLException e) {
            System.out.println("Error viewing inventory: "
                    + e.getMessage());
        }
    }

    // ==========================================
    // OPTION 3: ADD NEW PRODUCT
    // ==========================================
    public static void addProduct(
            int managerId,
            String productName,
            String category,
            double unitPrice,
            int stock,
            String description) {

        String sql = """
            INSERT INTO Products
            (ProductName, Category, UnitPrice,
             StockQuantity, Description)
            VALUES (?, ?, ?, ?, ?)
            """;

        try (Connection conn = DatabaseConnection.connect();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, productName);
            pstmt.setString(2, category);
            pstmt.setDouble(3, unitPrice);
            pstmt.setInt(4, stock);
            pstmt.setString(5, description);

            int rows = pstmt.executeUpdate();

            if (rows > 0) {

                System.out.println("Product added successfully!");

                ActivityLogDao.logAction(
                        managerId,
                        "PRODUCT_ADDED",
                        "Products",
                        "Manager added product: " + productName
                                + ", Price: PHP " + unitPrice
                                + ", Initial stock: " + stock
                );
            }

        } catch (SQLException e) {
            System.out.println("Error adding product: "
                    + e.getMessage());
        }
    }

    // ==========================================
    // OPTION 4: RESTOCK PRODUCT
    // ==========================================
    public static void updateStock(
            int managerId,
            int productId,
            int quantityToAdd) {

        String sql = """
            UPDATE Products
            SET StockQuantity = StockQuantity + ?
            WHERE ProductID = ?
            """;

        try (Connection conn = DatabaseConnection.connect();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, quantityToAdd);
            pstmt.setInt(2, productId);

            int rows = pstmt.executeUpdate();

            if (rows > 0) {

                System.out.println("Stock updated successfully!");

                ActivityLogDao.logAction(
                        managerId,
                        "STOCK_REFILLED",
                        "Products",
                        "Manager added " + quantityToAdd
                                + " units to Product ID " + productId
                );

            } else {
                System.out.println("Product ID not found.");
            }

        } catch (SQLException e) {
            System.out.println("Error updating stock: "
                    + e.getMessage());
        }
    }

    // ==========================================
    // OPTION 5: MONITOR CASHIER ACTIVITIES
    // ==========================================
    public static void monitorCashierActivities(int managerId) {

        String sql = """
            SELECT
                S.TransactionID,
                S.TransactionDate,
                U.UserID,
                U.Username,
                S.TotalAmount,
                S.PaymentMethod,
                S.TransactionStatus
            FROM SalesTransactions S
            INNER JOIN Users U ON S.UserID = U.UserID
            WHERE UPPER(U.Role) = 'CASHIER'
            ORDER BY S.TransactionID DESC
            """;

        try (Connection conn = DatabaseConnection.connect();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {

            System.out.println("\n========== CASHIER ACTIVITIES ==========");

            System.out.printf("%-8s %-9s %-15s %-20s %-12s %-12s%n",
                    "TRANS ID", "USER ID", "CASHIER",
                    "DATE", "AMOUNT", "STATUS");

            System.out.println("------------------------------------------------"
                    + "----------------");

            boolean found = false;

            while (rs.next()) {

                found = true;

                System.out.printf(
                        "%-8d %-9d %-15s %-20s PHP %-7.2f %-12s%n",
                        rs.getInt("TransactionID"),
                        rs.getInt("UserID"),
                        shorten(rs.getString("Username"), 14),
                        rs.getString("TransactionDate"),
                        rs.getDouble("TotalAmount"),
                        rs.getString("TransactionStatus")
                );

                System.out.println("Payment Method: "
                        + rs.getString("PaymentMethod"));
            }

            if (!found) {
                System.out.println("No cashier transactions found.");
            }

            System.out.println("========================================");

            ActivityLogDao.logAction(
                    managerId,
                    "MONITOR_CASHIER_ACTIVITIES",
                    "SalesTransactions",
                    "Manager viewed cashier transaction activities."
            );

        } catch (SQLException e) {
            System.out.println("Error monitoring cashier activities: "
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
