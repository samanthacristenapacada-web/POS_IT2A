
package com.scc.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;

public class DatabaseInitializer {

    public static void initialize() {

        try (Connection conn = DatabaseConnection.connect();
             Statement stmt = conn.createStatement()) {

            stmt.execute("""
                CREATE TABLE IF NOT EXISTS Users (
                    UserID INTEGER PRIMARY KEY AUTOINCREMENT,
                    FullName TEXT NOT NULL,
                    Username TEXT UNIQUE NOT NULL,
                    Password TEXT NOT NULL,
                    Role TEXT NOT NULL CHECK(
                        Role IN (
                            'CASHIER',
                            'STORE_MANAGER',
                            'SYSTEM_ADMINISTRATOR',
                            'INVENTORY_STAFF'
                        )
                    ),
                    ContactNumber TEXT NOT NULL,
                    Status TEXT DEFAULT 'PENDING'
                )
            """);

            stmt.execute("""
                CREATE TABLE IF NOT EXISTS Products (
                    ProductID INTEGER PRIMARY KEY AUTOINCREMENT,
                    ProductName TEXT NOT NULL,
                    Category TEXT,
                    UnitPrice REAL NOT NULL,
                    StockQuantity INTEGER NOT NULL,
                    Description TEXT,
                    DateAdded TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    ReorderLevel INTEGER NOT NULL DEFAULT 5
                )
            """);

            // Add ReorderLevel to existing databases if missing.
            boolean hasReorderLevel = false;

            try (ResultSet rs =
                         stmt.executeQuery("PRAGMA table_info(Products)")) {

                while (rs.next()) {
                    if ("ReorderLevel".equalsIgnoreCase(
                            rs.getString("name"))) {
                        hasReorderLevel = true;
                        break;
                    }
                }
            }

            if (!hasReorderLevel) {
                stmt.execute("""
                    ALTER TABLE Products
                    ADD COLUMN ReorderLevel INTEGER NOT NULL DEFAULT 5
                """);
            }

            stmt.execute("""
                CREATE TABLE IF NOT EXISTS SalesTransactions (
                    TransactionID INTEGER PRIMARY KEY AUTOINCREMENT,
                    TransactionDate TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    UserID INTEGER NOT NULL,
                    TotalAmount REAL NOT NULL,
                    PaymentMethod TEXT,
                    TransactionStatus TEXT,
                    FOREIGN KEY(UserID)
                        REFERENCES Users(UserID) ON DELETE CASCADE
                )
            """);

            stmt.execute("""
                CREATE TABLE IF NOT EXISTS TransactionDetails (
                    DetailID INTEGER PRIMARY KEY AUTOINCREMENT,
                    TransactionID INTEGER NOT NULL,
                    ProductID INTEGER NOT NULL,
                    Quantity INTEGER NOT NULL,
                    UnitPrice REAL NOT NULL,
                    Subtotal REAL NOT NULL,
                    FOREIGN KEY(TransactionID)
                        REFERENCES SalesTransactions(TransactionID)
                        ON DELETE CASCADE,
                    FOREIGN KEY(ProductID)
                        REFERENCES Products(ProductID)
                        ON DELETE CASCADE
                )
            """);

            stmt.execute("""
                CREATE TABLE IF NOT EXISTS Activity_Logs (
                    LogID INTEGER PRIMARY KEY AUTOINCREMENT,
                    UserID INTEGER NOT NULL,
                    Action TEXT NOT NULL,
                    TableAffected TEXT,
                    Description TEXT,
                    Timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    FOREIGN KEY(UserID)
                        REFERENCES Users(UserID) ON DELETE CASCADE
                )
            """);

            // Inventory movement history.
            // SET NULL preserves the history if a product or user is deleted.
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS Inventory_Logs (
                    InventoryLogID INTEGER PRIMARY KEY AUTOINCREMENT,
                    ProductID INTEGER,
                    ProductName TEXT NOT NULL,
                    UserID INTEGER,
                    MovementType TEXT NOT NULL,
                    QuantityChanged INTEGER NOT NULL,
                    PreviousStock INTEGER NOT NULL,
                    NewStock INTEGER NOT NULL,
                    Reason TEXT,
                    Timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    FOREIGN KEY(ProductID)
                        REFERENCES Products(ProductID) ON DELETE SET NULL,
                    FOREIGN KEY(UserID)
                        REFERENCES Users(UserID) ON DELETE SET NULL
                )
            """);

            System.out.println("Database initialized successfully.");

        } catch (Exception e) {
            System.out.println(
                    "Database initialization error: " + e.getMessage()
            );
        }
    }
}
