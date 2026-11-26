
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
// ADD NEW PRODUCT WITH INVENTORY HISTORY
// ==========================================
    public static void addProduct(
            int managerId,
            String productName,
            String category,
            double unitPrice,
            int stock,
            String description) {

        if (productName == null || productName.isBlank()
                || category == null || category.isBlank()
                || description == null || description.isBlank()
                || !Double.isFinite(unitPrice) || unitPrice <= 0
                || stock < 0) {
            System.out.println("Invalid product information.");
            return;
        }

        String sql = """
        INSERT INTO Products
        (ProductName, Category, UnitPrice,
         StockQuantity, Description)
        VALUES (?, ?, ?, ?, ?)
        """;

        try (Connection conn = DatabaseConnection.connect()) {
            conn.setAutoCommit(false);

            try {
                int productId;

                try (PreparedStatement pstmt = conn.prepareStatement(
                        sql, java.sql.Statement.RETURN_GENERATED_KEYS)) {

                    pstmt.setString(1, productName.trim());
                    pstmt.setString(2, category.trim());
                    pstmt.setDouble(3, unitPrice);
                    pstmt.setInt(4, stock);
                    pstmt.setString(5, description.trim());
                    pstmt.executeUpdate();

                    try (ResultSet keys = pstmt.getGeneratedKeys()) {
                        if (keys.next()) {
                            productId = keys.getInt(1);
                        } else {
                            try (PreparedStatement idStmt =
                                         conn.prepareStatement(
                                                 "SELECT last_insert_rowid()");
                                 ResultSet rs = idStmt.executeQuery()) {
                                if (!rs.next()) {
                                    throw new SQLException(
                                            "Could not retrieve Product ID.");
                                }
                                productId = rs.getInt(1);
                            }
                        }
                    }
                }

                InventoryLogDao.logMovement(
                        conn,
                        productId,
                        productName.trim(),
                        managerId,
                        "INITIAL_STOCK",
                        stock,
                        0,
                        stock,
                        "Initial stock recorded when product was added."
                );

                ActivityLogDao.logAction(
                        conn,
                        managerId,
                        "PRODUCT_ADDED",
                        "Products",
                        "Added " + productName.trim()
                                + " with price PHP " + unitPrice
                                + " and initial stock " + stock
                );

                conn.commit();

                System.out.println("Product added successfully!");
                System.out.println("Product ID: " + productId);

            } catch (Exception e) {
                conn.rollback();
                System.out.println("Product addition failed: "
                        + e.getMessage());
                System.out.println("Changes were rolled back.");
            } finally {
                conn.setAutoCommit(true);
            }

        } catch (SQLException e) {
            System.out.println("Error adding product: " + e.getMessage());
        }
    }


    // ==========================================
    // RESTOCK PRODUCT WITH INVENTORY HISTORY
    // ==========================================
    public static void updateStock(
            int managerId,
            int productId,
            int quantityToAdd) {

        if (quantityToAdd <= 0) {
            System.out.println("Quantity must be greater than zero.");
            return;
        }

        String selectSql = """
            SELECT ProductName, StockQuantity
            FROM Products
            WHERE ProductID = ?
        """;

        String updateSql = """
            UPDATE Products
            SET StockQuantity = StockQuantity + ?
            WHERE ProductID = ?
        """;

        try (Connection conn = DatabaseConnection.connect()) {

            conn.setAutoCommit(false);

            try {

                String productName;
                int previousStock;

                try (PreparedStatement pstmt =
                             conn.prepareStatement(selectSql)) {

                    pstmt.setInt(1, productId);

                    try (ResultSet rs = pstmt.executeQuery()) {

                        if (!rs.next()) {
                            throw new SQLException("Product ID not found.");
                        }

                        productName = rs.getString("ProductName");
                        previousStock = rs.getInt("StockQuantity");
                    }
                }

                try (PreparedStatement pstmt =
                             conn.prepareStatement(updateSql)) {

                    pstmt.setInt(1, quantityToAdd);
                    pstmt.setInt(2, productId);

                    if (pstmt.executeUpdate() != 1) {
                        throw new SQLException("Stock update failed.");
                    }
                }

                int newStock = previousStock + quantityToAdd;

                InventoryLogDao.logMovement(
                        conn,
                        productId,
                        productName,
                        managerId,
                        "STOCK_RESTOCKED",
                        quantityToAdd,
                        previousStock,
                        newStock,
                        "Product restocked by Store Manager."
                );

                ActivityLogDao.logAction(
                        conn,
                        managerId,
                        "STOCK_REFILLED",
                        "Products",
                        "Added " + quantityToAdd
                                + " units to " + productName
                                + ". Previous stock: " + previousStock
                                + ". New stock: " + newStock
                );

                conn.commit();

                System.out.println("Stock updated successfully!");
                System.out.println("Previous stock: " + previousStock);
                System.out.println("New stock: " + newStock);

            } catch (Exception e) {

                conn.rollback();

                System.out.println("Restocking failed: " + e.getMessage());
                System.out.println("Changes were rolled back.");

            } finally {
                conn.setAutoCommit(true);
            }

        } catch (SQLException e) {
            System.out.println("Error updating stock: " + e.getMessage());
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


    // ==========================================
    // VIEW LOW-STOCK PRODUCTS
    // ==========================================
    public static void viewLowStock(int managerId) {

        String sql = """
            SELECT ProductID, ProductName, Category,
                   StockQuantity, ReorderLevel
            FROM Products
            WHERE StockQuantity <= ReorderLevel
            ORDER BY StockQuantity ASC
        """;

        try (Connection conn = DatabaseConnection.connect();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {

            System.out.println("\n========== LOW-STOCK PRODUCTS ==========");

            System.out.printf(
                    "%-10s %-25s %-15s %-12s %-12s%n",
                    "PRODUCT ID",
                    "PRODUCT NAME",
                    "CATEGORY",
                    "CURRENT",
                    "REORDER"
            );

            System.out.println(
                    "----------------------------------------------------------------"
            );

            boolean found = false;

            while (rs.next()) {

                found = true;

                System.out.printf(
                        "%-10d %-25s %-15s %-12d %-12d%n",
                        rs.getInt("ProductID"),
                        rs.getString("ProductName"),
                        rs.getString("Category"),
                        rs.getInt("StockQuantity"),
                        rs.getInt("ReorderLevel")
                );
            }

            if (!found) {
                System.out.println("No low-stock products found.");
            }

            System.out.println("========================================");

            ActivityLogDao.logAction(
                    conn,
                    managerId,
                    "VIEW_LOW_STOCK",
                    "Products",
                    "Manager viewed low-stock products."
            );

        } catch (SQLException e) {
            System.out.println(
                    "Error viewing low-stock products: " + e.getMessage()
            );
        }
    }


    // ==========================================
    // VIEW INVENTORY MOVEMENT HISTORY
    // ==========================================
    public static void viewInventoryHistory(int managerId) {

        InventoryLogDao.viewInventoryHistory();

        ActivityLogDao.logAction(
                managerId,
                "VIEW_INVENTORY_HISTORY",
                "Inventory_Logs",
                "Manager viewed inventory movement history."
        );
    }


    // ==========================================
    // DAILY, WEEKLY, AND MONTHLY SALES REPORTS
    // ==========================================
    public static void viewPeriodSalesReports(int managerId) {

        String sql = """
            WITH periods(label, start_date, end_date, sort_order) AS (

                SELECT
                    'DAILY',
                    date('now', 'localtime'),
                    date('now', 'localtime', '+1 day'),
                    1

                UNION ALL

                SELECT
                    'WEEKLY',
                    date(
                        'now',
                        'localtime',
                        '-' || (
                            (CAST(strftime('%w', 'now', 'localtime')
                            AS INTEGER) + 6) % 7
                        ) || ' days'
                    ),
                    date('now', 'localtime', '+1 day'),
                    2

                UNION ALL

                SELECT
                    'MONTHLY',
                    date('now', 'localtime', 'start of month'),
                    date('now', 'localtime', '+1 day'),
                    3
            )

            SELECT
                P.label AS ReportPeriod,

                COUNT(
                    CASE
                        WHEN UPPER(S.TransactionStatus) = 'COMPLETED'
                        THEN 1
                    END
                ) AS CompletedTransactions,

                COUNT(
                    CASE
                        WHEN UPPER(S.TransactionStatus) = 'VOIDED'
                        THEN 1
                    END
                ) AS VoidedTransactions,

                COALESCE(
                    SUM(
                        CASE
                            WHEN UPPER(S.TransactionStatus) = 'COMPLETED'
                            THEN S.TotalAmount
                            ELSE 0
                        END
                    ), 0
                ) AS TotalRevenue,

                COALESCE(
                    AVG(
                        CASE
                            WHEN UPPER(S.TransactionStatus) = 'COMPLETED'
                            THEN S.TotalAmount
                        END
                    ), 0
                ) AS AverageTransaction

            FROM periods P

            LEFT JOIN SalesTransactions S
                ON date(S.TransactionDate, 'localtime')
                    >= P.start_date
                AND date(S.TransactionDate, 'localtime')
                    < P.end_date

            GROUP BY P.label, P.sort_order
            ORDER BY P.sort_order
            """;

        try (Connection conn = DatabaseConnection.connect();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {

            System.out.println(
                    "\n========== PERIOD SALES REPORTS =========="
            );

            while (rs.next()) {

                System.out.println(
                        "\n---------- "
                                + rs.getString("ReportPeriod")
                                + " SALES ----------"
                );

                System.out.println(
                        "Completed Transactions: "
                                + rs.getInt("CompletedTransactions")
                );

                System.out.println(
                        "Voided Transactions: "
                                + rs.getInt("VoidedTransactions")
                );

                System.out.printf(
                        "Total Revenue: PHP %,.2f%n",
                        rs.getDouble("TotalRevenue")
                );

                System.out.printf(
                        "Average Transaction: PHP %,.2f%n",
                        rs.getDouble("AverageTransaction")
                );
            }

            System.out.println(
                    "\n==========================================="
            );

            ActivityLogDao.logAction(
                    conn,
                    managerId,
                    "VIEW_PERIOD_SALES_REPORTS",
                    "SalesTransactions",
                    "Manager viewed daily, weekly, and monthly sales reports."
            );

        } catch (SQLException e) {

            System.out.println(
                    "Error generating period sales reports: "
                            + e.getMessage()
            );
        }
    }


    // ==========================================
    // INVENTORY REPORT
    // ==========================================
    public static void viewInventoryReport(int managerId) {

        String overviewSql = """
            SELECT
                COUNT(*) AS TotalProducts,

                COALESCE(SUM(StockQuantity), 0)
                    AS TotalUnits,

                COALESCE(SUM(
                    StockQuantity * UnitPrice
                ), 0) AS TotalStockValue,

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

        String categorySql = """
            SELECT
                COALESCE(Category, 'Uncategorized')
                    AS ProductCategory,

                COUNT(*) AS ProductCount,

                COALESCE(SUM(StockQuantity), 0)
                    AS TotalUnits,

                COALESCE(SUM(
                    StockQuantity * UnitPrice
                ), 0) AS CategoryValue

            FROM Products

            GROUP BY Category
            ORDER BY Category
            """;

        String movementSql = """
            SELECT
                MovementType,
                COUNT(*) AS MovementCount,
                COALESCE(SUM(QuantityChanged), 0)
                    AS TotalQuantity

            FROM Inventory_Logs

            WHERE Timestamp >= datetime('now', '-30 days')

            GROUP BY MovementType
            ORDER BY MovementType
            """;

        try (Connection conn = DatabaseConnection.connect()) {

            // ==================================
            // PART 1: INVENTORY OVERVIEW
            // ==================================

            try (PreparedStatement pstmt =
                         conn.prepareStatement(overviewSql);
                 ResultSet rs = pstmt.executeQuery()) {

                if (rs.next()) {

                    System.out.println(
                            "\n========== INVENTORY REPORT =========="
                    );

                    System.out.println("\nINVENTORY OVERVIEW");
                    System.out.println("--------------------------------------");

                    System.out.println(
                            "Total Products: "
                                    + rs.getInt("TotalProducts")
                    );

                    System.out.println(
                            "Total Units in Stock: "
                                    + rs.getInt("TotalUnits")
                    );

                    System.out.printf(
                            "Total Stock Value: PHP %,.2f%n",
                            rs.getDouble("TotalStockValue")
                    );

                    System.out.println(
                            "Low-Stock Products: "
                                    + rs.getInt("LowStockProducts")
                    );

                    System.out.println(
                            "Out-of-Stock Products: "
                                    + rs.getInt("OutOfStockProducts")
                    );
                }
            }

            // ==================================
            // PART 2: CATEGORY SUMMARY
            // ==================================

            System.out.println("\nCATEGORY SUMMARY");
            System.out.println(
                    "------------------------------------------------------------"
            );

            System.out.printf(
                    "%-20s %-12s %-12s %-15s%n",
                    "CATEGORY",
                    "PRODUCTS",
                    "UNITS",
                    "VALUE"
            );

            System.out.println(
                    "------------------------------------------------------------"
            );

            boolean hasCategories = false;

            try (PreparedStatement pstmt =
                         conn.prepareStatement(categorySql);
                 ResultSet rs = pstmt.executeQuery()) {

                while (rs.next()) {

                    hasCategories = true;

                    System.out.printf(
                            "%-20s %-12d %-12d PHP %,.2f%n",
                            rs.getString("ProductCategory"),
                            rs.getInt("ProductCount"),
                            rs.getInt("TotalUnits"),
                            rs.getDouble("CategoryValue")
                    );
                }
            }

            if (!hasCategories) {
                System.out.println("No product categories found.");
            }

            // ==================================
            // PART 3: INVENTORY MOVEMENTS
            // LAST 30 DAYS
            // ==================================

            System.out.println("\nINVENTORY MOVEMENT SUMMARY");
            System.out.println("(Last 30 Days)");
            System.out.println(
                    "------------------------------------------------------------"
            );

            System.out.printf(
                    "%-25s %-18s %-18s%n",
                    "MOVEMENT TYPE",
                    "RECORDS",
                    "TOTAL QUANTITY"
            );

            System.out.println(
                    "------------------------------------------------------------"
            );

            boolean hasMovements = false;

            try (PreparedStatement pstmt =
                         conn.prepareStatement(movementSql);
                 ResultSet rs = pstmt.executeQuery()) {

                while (rs.next()) {

                    hasMovements = true;

                    System.out.printf(
                            "%-25s %-18d %-18d%n",
                            rs.getString("MovementType"),
                            rs.getInt("MovementCount"),
                            rs.getInt("TotalQuantity")
                    );
                }
            }

            if (!hasMovements) {
                System.out.println(
                        "No inventory movements recorded in the last 30 days."
                );
            }

            System.out.println(
                    "\n============================================================"
            );

            // ==================================
            // PART 4: AUDIT LOG
            // ==================================

            ActivityLogDao.logAction(
                    conn,
                    managerId,
                    "VIEW_INVENTORY_REPORT",
                    "Products",
                    "Manager viewed inventory valuation, category, "
                            + "stock status, and movement reports."
            );

        } catch (SQLException e) {

            System.out.println(
                    "Error generating inventory report: "
                            + e.getMessage()
            );
        }
    }
}
