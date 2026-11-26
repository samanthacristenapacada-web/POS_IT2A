
package com.scc.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class DashboardDao {

    public static void displayAdminOverview() {

        String sql = """
            SELECT
                (SELECT COUNT(*)
                 FROM Users) AS TotalUsers,

                (SELECT COUNT(*)
                 FROM Users
                 WHERE UPPER(Status) = 'ACTIVE') AS ActiveUsers,

                (SELECT COUNT(*)
                 FROM Users
                 WHERE UPPER(Status) = 'PENDING') AS PendingUsers,

                (SELECT COUNT(*)
                 FROM Products) AS TotalProducts,

                (SELECT COUNT(*)
                 FROM Products
                 WHERE StockQuantity <= ReorderLevel)
                    AS LowStockProducts,

                (SELECT COUNT(*)
                 FROM SalesTransactions
                 WHERE UPPER(TransactionStatus) = 'COMPLETED')
                    AS CompletedTransactions,

                (SELECT COUNT(*)
                 FROM SalesTransactions
                 WHERE UPPER(TransactionStatus) = 'COMPLETED'
                   AND DATE(TransactionDate, 'localtime')
                       = DATE('now', 'localtime'))
                    AS TodayTransactions,

                (SELECT COALESCE(SUM(TotalAmount), 0)
                 FROM SalesTransactions
                 WHERE UPPER(TransactionStatus) = 'COMPLETED'
                   AND DATE(TransactionDate, 'localtime')
                       = DATE('now', 'localtime'))
                    AS TodaySales,

                (SELECT COALESCE(SUM(TotalAmount), 0)
                 FROM SalesTransactions
                 WHERE UPPER(TransactionStatus) = 'COMPLETED')
                    AS TotalRevenue,

                (SELECT COUNT(*)
                 FROM SalesTransactions
                 WHERE UPPER(TransactionStatus) = 'VOIDED')
                    AS VoidedTransactions
            """;

        try (Connection conn = DatabaseConnection.connect();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {

            if (rs.next()) {

                System.out.println("""
                    
                    ==================================================
                         SYSTEM ADMINISTRATOR DASHBOARD OVERVIEW
                    ==================================================
                    """);

                System.out.println("USER STATISTICS");
                System.out.println("------------------------------------------");
                System.out.println(
                        "Total Users: "
                                + rs.getInt("TotalUsers"));
                System.out.println(
                        "Active Users: "
                                + rs.getInt("ActiveUsers"));
                System.out.println(
                        "Pending Users: "
                                + rs.getInt("PendingUsers"));

                System.out.println("\nINVENTORY STATISTICS");
                System.out.println("------------------------------------------");
                System.out.println(
                        "Total Products: "
                                + rs.getInt("TotalProducts"));
                System.out.println(
                        "Low-Stock Products: "
                                + rs.getInt("LowStockProducts"));

                System.out.println("\nSALES STATISTICS");
                System.out.println("------------------------------------------");
                System.out.println(
                        "Completed Transactions: "
                                + rs.getInt("CompletedTransactions"));
                System.out.println(
                        "Today's Transactions: "
                                + rs.getInt("TodayTransactions"));
                System.out.printf(
                        "Today's Sales: PHP %,.2f%n",
                        rs.getDouble("TodaySales"));
                System.out.printf(
                        "Overall Sales Revenue: PHP %,.2f%n",
                        rs.getDouble("TotalRevenue"));
                System.out.println(
                        "Voided Transactions: "
                                + rs.getInt("VoidedTransactions"));

                System.out.println(
                        "==================================================");
            }

        } catch (SQLException e) {

            System.out.println(
                    "Unable to load dashboard statistics: "
                            + e.getMessage());
        }
    }

    // ==========================================
// INVENTORY STAFF DASHBOARD OVERVIEW
// ==========================================
    public static void displayInventoryOverview() {

        String sql = """
        SELECT
            COUNT(*) AS TotalProducts,

            COALESCE(SUM(StockQuantity), 0)
                AS TotalUnits,

            SUM(CASE
                WHEN StockQuantity > 0
                 AND StockQuantity <= ReorderLevel
                THEN 1 ELSE 0
            END) AS LowStockProducts,

            SUM(CASE
                WHEN StockQuantity = 0
                THEN 1 ELSE 0
            END) AS OutOfStockProducts

        FROM Products
        """;

        try (Connection conn = DatabaseConnection.connect();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {

            if (rs.next()) {

                System.out.println("""

                ==================================================
                       INVENTORY STAFF DASHBOARD
                ==================================================
                """);

                System.out.println("INVENTORY OVERVIEW");
                System.out.println("------------------------------------------");

                System.out.println(
                        "Total Products: "
                                + rs.getInt("TotalProducts"));

                System.out.println(
                        "Total Units in Stock: "
                                + rs.getInt("TotalUnits"));

                System.out.println(
                        "Low-Stock Products: "
                                + rs.getInt("LowStockProducts"));

                System.out.println(
                        "Out-of-Stock Products: "
                                + rs.getInt("OutOfStockProducts"));

                System.out.println(
                        "==================================================");
            }

        } catch (SQLException e) {

            System.out.println(
                    "Unable to load inventory dashboard: "
                            + e.getMessage());
        }
    }
}
