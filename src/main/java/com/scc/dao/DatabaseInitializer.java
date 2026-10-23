package com.scc.dao;

import java.sql.Connection;
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
                    Role TEXT NOT NULL CHECK(Role IN ('CASHIER','STORE_MANAGER','SYSTEM_ADMINISTRATOR','INVENTORY_STAFF')),
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
                    DateAdded TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                )
            """);

            stmt.execute("""
                CREATE TABLE IF NOT EXISTS SalesTransactions (
                    TransactionID INTEGER PRIMARY KEY AUTOINCREMENT,
                    TransactionDate TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    UserID INTEGER NOT NULL,
                    TotalAmount REAL NOT NULL,
                    PaymentMethod TEXT,
                    TransactionStatus TEXT,
                    FOREIGN KEY(UserID) REFERENCES Users(UserID) ON DELETE CASCADE
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
                    FOREIGN KEY(TransactionID) REFERENCES SalesTransactions(TransactionID) ON DELETE CASCADE,
                    FOREIGN KEY(ProductID) REFERENCES Products(ProductID) ON DELETE CASCADE
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
                    FOREIGN KEY(UserID) REFERENCES Users(UserID) ON DELETE CASCADE
                )
            """);

        } catch (Exception e) {
            System.out.println("Database initialization error: " + e.getMessage());
        }
    }
}
