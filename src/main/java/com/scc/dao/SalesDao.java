package com.scc.dao;

import com.scc.model.Product;
import com.scc.model.SaleReceipt;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class SalesDao {

    // ==========================================
    // COMPLETE SALE
    // ==========================================
    public static SaleReceipt completeSale(
            int cashierId,
            Map<Integer, Integer> cart,
            BigDecimal amountPaid) {

        if (cart == null || cart.isEmpty()) {
            System.out.println("Your cart is empty.");
            return null;
        }

        if (amountPaid == null || amountPaid.signum() < 0) {
            System.out.println("Invalid payment amount.");
            return null;
        }

        String productSql = """
                SELECT ProductID, ProductName, Category,
                       UnitPrice, StockQuantity, Description
                FROM Products
                WHERE ProductID = ?
                """;

        String transactionSql = """
                INSERT INTO SalesTransactions
                (UserID, TotalAmount, PaymentMethod, TransactionStatus)
                VALUES (?, ?, ?, ?)
                """;

        String detailSql = """
                INSERT INTO TransactionDetails
                (TransactionID, ProductID, Quantity, UnitPrice, Subtotal)
                VALUES (?, ?, ?, ?, ?)
                """;

        String stockSql = """
                UPDATE Products
                SET StockQuantity = StockQuantity - ?
                WHERE ProductID = ?
                  AND StockQuantity >= ?
                """;

        try (Connection conn = DatabaseConnection.connect()) {

            conn.setAutoCommit(false);

            try {

                List<SaleReceipt.SaleLine> saleLines =
                        new ArrayList<>();

                BigDecimal totalAmount = BigDecimal.ZERO;

                // ----------------------------------
                // STEP 1: VALIDATE PRODUCTS AND STOCK
                // ----------------------------------

                for (Map.Entry<Integer, Integer> entry
                        : cart.entrySet()) {

                    int productId = entry.getKey();
                    int quantity = entry.getValue();

                    if (quantity <= 0) {
                        throw new SQLException(
                                "Invalid quantity for Product ID "
                                        + productId
                        );
                    }

                    Product product;

                    try (PreparedStatement pstmt =
                                 conn.prepareStatement(productSql)) {

                        pstmt.setInt(1, productId);

                        try (ResultSet rs = pstmt.executeQuery()) {

                            if (!rs.next()) {
                                throw new SQLException(
                                        "Product ID " + productId
                                                + " does not exist."
                                );
                            }

                            product = new Product(
                                    rs.getInt("ProductID"),
                                    rs.getString("ProductName"),
                                    rs.getString("Category"),
                                    rs.getDouble("UnitPrice"),
                                    rs.getInt("StockQuantity"),
                                    rs.getString("Description")
                            );
                        }
                    }


                    if (product.stockQuantity() < quantity) {
                        throw new SQLException(
                                "Not enough stock for "
                                        + product.productName()
                                        + ". Available: "
                                        + product.stockQuantity()
                        );
                    }

                    BigDecimal unitPrice =
                            BigDecimal.valueOf(product.unitPrice())
                                    .setScale(2, RoundingMode.HALF_UP);

                    BigDecimal subtotal =
                            unitPrice.multiply(
                                    BigDecimal.valueOf(quantity)
                            ).setScale(2, RoundingMode.HALF_UP);

                    totalAmount = totalAmount.add(subtotal);

                    saleLines.add(
                            new SaleReceipt.SaleLine(
                                    product.productId(),
                                    product.productName(),
                                    quantity,
                                    unitPrice,
                                    subtotal
                            )
                    );
                }

                totalAmount = totalAmount.setScale(
                        2,
                        RoundingMode.HALF_UP
                );

                if (amountPaid.compareTo(totalAmount) < 0) {
                    throw new SQLException(
                            "Payment is less than the total amount."
                    );
                }

                BigDecimal change =
                        amountPaid.subtract(totalAmount)
                                .setScale(2, RoundingMode.HALF_UP);

                // ----------------------------------
                // STEP 2: INSERT SALES TRANSACTION
                // ----------------------------------

                int transactionId;

                try (PreparedStatement pstmt =
                             conn.prepareStatement(
                                     transactionSql,
                                     Statement.RETURN_GENERATED_KEYS
                             )) {

                    pstmt.setInt(1, cashierId);
                    pstmt.setDouble(2, totalAmount.doubleValue());
                    pstmt.setString(3, "CASH");
                    pstmt.setString(4, "COMPLETED");

                    pstmt.executeUpdate();

                    try (ResultSet keys = pstmt.getGeneratedKeys()) {

                        if (keys.next()) {
                            transactionId = keys.getInt(1);

                        } else {

                            try (Statement stmt =
                                         conn.createStatement();
                                 ResultSet rs = stmt.executeQuery(
                                         "SELECT last_insert_rowid()"
                                 )) {

                                if (!rs.next()) {
                                    throw new SQLException(
                                            "Unable to retrieve transaction ID."
                                    );
                                }

                                transactionId = rs.getInt(1);
                            }
                        }
                    }
                }

                // ----------------------------------
                // STEP 3: INSERT TRANSACTION DETAILS
                // ----------------------------------

                try (PreparedStatement pstmt =
                             conn.prepareStatement(detailSql)) {

                    for (SaleReceipt.SaleLine item : saleLines) {

                        pstmt.setInt(1, transactionId);
                        pstmt.setInt(2, item.productId());
                        pstmt.setInt(3, item.quantity());
                        pstmt.setDouble(
                                4,
                                item.unitPrice().doubleValue()
                        );
                        pstmt.setDouble(
                                5,
                                item.subtotal().doubleValue()
                        );

                        pstmt.addBatch();
                    }

                    pstmt.executeBatch();
                }


                // ----------------------------------
                // STEP 4: DEDUCT PRODUCT STOCK
                // ----------------------------------

                String currentStockSql = """
                    SELECT ProductName, StockQuantity
                    FROM Products
                    WHERE ProductID = ?
                """;

                try (PreparedStatement stockCheck =
                             conn.prepareStatement(currentStockSql);
                     PreparedStatement pstmt =
                             conn.prepareStatement(stockSql)) {

                    for (SaleReceipt.SaleLine item : saleLines) {

                        int previousStock;
                        String productName;

                        stockCheck.setInt(1, item.productId());

                        try (ResultSet rs = stockCheck.executeQuery()) {

                            if (!rs.next()) {
                                throw new SQLException(
                                        "Product not found: "
                                                + item.productName()
                                );
                            }

                            previousStock =
                                    rs.getInt("StockQuantity");

                            productName =
                                    rs.getString("ProductName");
                        }

                        pstmt.setInt(1, item.quantity());
                        pstmt.setInt(2, item.productId());
                        pstmt.setInt(3, item.quantity());

                        int updated = pstmt.executeUpdate();

                        if (updated != 1) {
                            throw new SQLException(
                                    "Stock update failed for Product ID "
                                            + item.productId()
                            );
                        }

                        int newStock = previousStock - item.quantity();

                        InventoryLogDao.logMovement(
                                conn,
                                item.productId(),
                                productName,
                                cashierId,
                                "SALE_DEDUCTED",
                                item.quantity(),
                                previousStock,
                                newStock,
                                "Stock deducted from Sale #"
                                        + transactionId
                        );

                        ActivityLogDao.logAction(
                                conn,
                                cashierId,
                                "STOCK_DEDUCTED",
                                "Products",
                                "Sale #" + transactionId
                                        + ": deducted "
                                        + item.quantity()
                                        + " unit(s) of "
                                        + productName
                        );
                    }
                }


                // ----------------------------------
                // STEP 5: RECORD SALE AUDIT LOG
                // ----------------------------------

                ActivityLogDao.logAction(
                        conn,
                        cashierId,
                        "SALE_COMPLETED",
                        "SalesTransactions",
                        "Completed Sale #" + transactionId
                                + ". Total: PHP "
                                + totalAmount
                );

                // ----------------------------------
                // STEP 6: GET TRANSACTION DATE
                // ----------------------------------

                String transactionDate = "";

                String dateSql = """
                        SELECT TransactionDate
                        FROM SalesTransactions
                        WHERE TransactionID = ?
                        """;

                try (PreparedStatement pstmt =
                             conn.prepareStatement(dateSql)) {

                    pstmt.setInt(1, transactionId);

                    try (ResultSet rs = pstmt.executeQuery()) {

                        if (rs.next()) {
                            transactionDate =
                                    rs.getString("TransactionDate");
                        }
                    }
                }

                // ----------------------------------
                // STEP 7: COMMIT EVERYTHING
                // ----------------------------------

                conn.commit();

                System.out.println(
                        "\nSale completed successfully!"
                );

                return new SaleReceipt(
                        transactionId,
                        transactionDate,
                        totalAmount,
                        amountPaid,
                        change,
                        saleLines
                );

            } catch (Exception e) {

                conn.rollback();

                System.out.println(
                        "\nSale was not completed. "
                                + "All database changes were rolled back."
                );

                System.out.println("Reason: " + e.getMessage());

                return null;

            } finally {

                conn.setAutoCommit(true);
            }

        } catch (SQLException e) {

            System.out.println(
                    "Sales database error: " + e.getMessage()
            );

            return null;
        }
    }

    // ==========================================
    // VIEW RECENT TRANSACTIONS
    // ==========================================
    public static void viewTransactions(int cashierId) {

        String sql = """
                SELECT TransactionID, TransactionDate,
                       TotalAmount, PaymentMethod, TransactionStatus
                FROM SalesTransactions
                WHERE UserID = ?
                ORDER BY TransactionID DESC
                LIMIT 50
                """;

        try (Connection conn = DatabaseConnection.connect();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, cashierId);

            try (ResultSet rs = pstmt.executeQuery()) {

                System.out.println("\n========== MY TRANSACTIONS ==========");

                System.out.printf(
                        "%-10s %-22s %-15s %-12s %-12s%n",
                        "TRANS ID",
                        "DATE",
                        "TOTAL",
                        "PAYMENT",
                        "STATUS"
                );

                System.out.println(
                        "----------------------------------------------------------------"
                );

                boolean found = false;

                while (rs.next()) {

                    found = true;

                    System.out.printf(
                            "%-10d %-22s PHP %-10.2f %-12s %-12s%n",
                            rs.getInt("TransactionID"),
                            rs.getString("TransactionDate"),
                            rs.getDouble("TotalAmount"),
                            rs.getString("PaymentMethod"),
                            rs.getString("TransactionStatus")
                    );
                }

                if (!found) {
                    System.out.println("No transactions found.");
                }

                ActivityLogDao.logAction(
                        conn,
                        cashierId,
                        "VIEW_TRANSACTIONS",
                        "SalesTransactions",
                        "Cashier viewed their transaction history."
                );
            }

        } catch (SQLException e) {

            System.out.println(
                    "Error viewing transactions: " + e.getMessage()
            );
        }
    }

    // ==========================================
    // VIEW ONE TRANSACTION
    // ==========================================
    public static void viewTransactionDetails(
            int cashierId,
            int transactionId) {

        String transactionSql = """
                SELECT TransactionID, TransactionDate,
                       TotalAmount, PaymentMethod, TransactionStatus
                FROM SalesTransactions
                WHERE TransactionID = ?
                  AND UserID = ?
                """;

        String detailsSql = """
                SELECT D.ProductID, P.ProductName,
                       D.Quantity, D.UnitPrice, D.Subtotal
                FROM TransactionDetails D
                INNER JOIN Products P
                    ON D.ProductID = P.ProductID
                WHERE D.TransactionID = ?
                """;

        try (Connection conn = DatabaseConnection.connect();
             PreparedStatement transactionStmt =
                     conn.prepareStatement(transactionSql)) {

            transactionStmt.setInt(1, transactionId);
            transactionStmt.setInt(2, cashierId);

            try (ResultSet transaction =
                         transactionStmt.executeQuery()) {

                if (!transaction.next()) {

                    System.out.println(
                            "Transaction not found or does not belong to you."
                    );

                    return;
                }

                System.out.println("\n========== TRANSACTION DETAILS ==========");
                System.out.println(
                        "Transaction ID: "
                                + transaction.getInt("TransactionID")
                );
                System.out.println(
                        "Date: "
                                + transaction.getString("TransactionDate")
                );
                System.out.println(
                        "Payment: "
                                + transaction.getString("PaymentMethod")
                );
                System.out.println(
                        "Status: "
                                + transaction.getString("TransactionStatus")
                );

                try (PreparedStatement detailsStmt =
                             conn.prepareStatement(detailsSql)) {

                    detailsStmt.setInt(1, transactionId);

                    try (ResultSet details =
                                 detailsStmt.executeQuery()) {

                        System.out.printf(
                                "%-8s %-22s %-8s %-12s %-12s%n",
                                "ID",
                                "PRODUCT",
                                "QTY",
                                "PRICE",
                                "SUBTOTAL"
                        );

                        while (details.next()) {

                            System.out.printf(
                                    "%-8d %-22s %-8d PHP %-8.2f PHP %-8.2f%n",
                                    details.getInt("ProductID"),
                                    details.getString("ProductName"),
                                    details.getInt("Quantity"),
                                    details.getDouble("UnitPrice"),
                                    details.getDouble("Subtotal")
                            );
                        }
                    }
                }

                System.out.printf(
                        "TOTAL AMOUNT: PHP %,.2f%n",
                        transaction.getDouble("TotalAmount")
                );

                System.out.println("=========================================");

                ActivityLogDao.logAction(
                        conn,
                        cashierId,
                        "VIEW_TRANSACTION_DETAILS",
                        "SalesTransactions",
                        "Cashier viewed Transaction ID "
                                + transactionId
                );
            }

        } catch (SQLException e) {

            System.out.println(
                    "Error retrieving transaction details: "
                            + e.getMessage()
            );
        }
    }

    // ==========================================
    // VOID A COMPLETED SALE
    // ==========================================
    public static void voidSale(
            int actorId,
            int transactionId,
            String reason) {

        if (reason == null || reason.trim().isEmpty()) {
            System.out.println("A reason is required to void a sale.");
            return;
        }

        String roleSql = """
            SELECT Role, Status
            FROM Users
            WHERE UserID = ?
        """;

        String transactionSql = """
            SELECT TransactionStatus
            FROM SalesTransactions
            WHERE TransactionID = ?
        """;

        String detailsSql = """
            SELECT
                D.ProductID,
                D.Quantity,
                P.ProductName
            FROM TransactionDetails D
            INNER JOIN Products P
                ON D.ProductID = P.ProductID
            WHERE D.TransactionID = ?
        """;

        String stockSql = """
            SELECT StockQuantity
            FROM Products
            WHERE ProductID = ?
        """;

        String restoreSql = """
            UPDATE Products
            SET StockQuantity = StockQuantity + ?
            WHERE ProductID = ?
        """;

        String updateTransactionSql = """
            UPDATE SalesTransactions
            SET TransactionStatus = 'VOIDED'
            WHERE TransactionID = ?
              AND UPPER(TransactionStatus) = 'COMPLETED'
        """;

        try (Connection conn = DatabaseConnection.connect()) {

            conn.setAutoCommit(false);

            try {

                // STEP 1: CHECK THE USER'S ROLE AND STATUS.
                boolean authorized = false;

                try (PreparedStatement pstmt =
                             conn.prepareStatement(roleSql)) {

                    pstmt.setInt(1, actorId);

                    try (ResultSet rs = pstmt.executeQuery()) {

                        if (rs.next()) {

                            String role = rs.getString("Role");
                            String status = rs.getString("Status");

                            authorized =
                                    "ACTIVE".equalsIgnoreCase(status)
                                            && (
                                            "SYSTEM_ADMINISTRATOR".equalsIgnoreCase(role)
                                                    || "STORE_MANAGER".equalsIgnoreCase(role)
                                    );
                        }
                    }
                }

                if (!authorized) {
                    throw new SQLException(
                            "Only active System Administrators and Store Managers "
                                    + "can void transactions."
                    );
                }

                // STEP 2: CHECK IF THE TRANSACTION EXISTS
                // AND IS STILL COMPLETED.
                try (PreparedStatement pstmt =
                             conn.prepareStatement(transactionSql)) {

                    pstmt.setInt(1, transactionId);

                    try (ResultSet rs = pstmt.executeQuery()) {

                        if (!rs.next()) {
                            throw new SQLException(
                                    "Transaction ID not found."
                            );
                        }

                        String status =
                                rs.getString("TransactionStatus");

                        if (!"COMPLETED".equalsIgnoreCase(status)) {
                            throw new SQLException(
                                    "Only completed transactions can be voided."
                            );
                        }
                    }
                }

                // STEP 3: RESTORE STOCK FOR EACH SOLD PRODUCT.
                try (PreparedStatement detailsStmt =
                             conn.prepareStatement(detailsSql);
                     PreparedStatement stockStmt =
                             conn.prepareStatement(stockSql);
                     PreparedStatement restoreStmt =
                             conn.prepareStatement(restoreSql)) {

                    detailsStmt.setInt(1, transactionId);

                    try (ResultSet rs = detailsStmt.executeQuery()) {

                        boolean hasItems = false;

                        while (rs.next()) {

                            hasItems = true;

                            int productId = rs.getInt("ProductID");
                            int quantity = rs.getInt("Quantity");
                            String productName =
                                    rs.getString("ProductName");

                            stockStmt.setInt(1, productId);

                            int previousStock;

                            try (ResultSet stockRs =
                                         stockStmt.executeQuery()) {

                                if (!stockRs.next()) {
                                    throw new SQLException(
                                            "Product no longer exists: "
                                                    + productName
                                    );
                                }

                                previousStock =
                                        stockRs.getInt("StockQuantity");
                            }

                            restoreStmt.setInt(1, quantity);
                            restoreStmt.setInt(2, productId);

                            if (restoreStmt.executeUpdate() != 1) {
                                throw new SQLException(
                                        "Unable to restore stock for "
                                                + productName
                                );
                            }

                            int newStock = previousStock + quantity;

                            InventoryLogDao.logMovement(
                                    conn,
                                    productId,
                                    productName,
                                    actorId,
                                    "SALE_VOID_RESTOCK",
                                    quantity,
                                    previousStock,
                                    newStock,
                                    "Stock restored from voided Sale #"
                                            + transactionId
                            );
                        }

                        if (!hasItems) {
                            throw new SQLException(
                                    "No transaction details were found."
                            );
                        }
                    }
                }

                // STEP 4: CHANGE TRANSACTION STATUS.
                try (PreparedStatement pstmt =
                             conn.prepareStatement(updateTransactionSql)) {

                    pstmt.setInt(1, transactionId);

                    if (pstmt.executeUpdate() != 1) {
                        throw new SQLException(
                                "Transaction status could not be updated."
                        );
                    }
                }

                // STEP 5: RECORD AUDIT EVENTS.
                ActivityLogDao.logAction(
                        conn,
                        actorId,
                        "SALE_VOIDED",
                        "SalesTransactions",
                        "Transaction #" + transactionId
                                + " was voided. Reason: " + reason.trim()
                );

                ActivityLogDao.logAction(
                        conn,
                        actorId,
                        "STOCK_RESTORED",
                        "Products",
                        "Stock restored for all items in Transaction #"
                                + transactionId
                );

                // STEP 6: COMMIT ALL CHANGES.
                conn.commit();

                System.out.println(
                        "Transaction #" + transactionId
                                + " was successfully voided."
                );

                System.out.println(
                        "Product stock has been restored."
                );

            } catch (Exception e) {

                conn.rollback();

                System.out.println("Unable to void transaction.");
                System.out.println("Reason: " + e.getMessage());
                System.out.println("All changes were rolled back.");

            } finally {
                conn.setAutoCommit(true);
            }

        } catch (SQLException e) {
            System.out.println(
                    "Transaction voiding error: " + e.getMessage()
            );
        }
    }
}