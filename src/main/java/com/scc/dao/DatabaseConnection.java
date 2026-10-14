package com.scc.dao;

import java.sql.*;

public class DatabaseConnection {
    private static final String URL = "jdbc:sqlite:pos_system.db";

    public static Connection connect() throws SQLException {
        return DriverManager.getConnection(URL);
    }

    public static void initialize() {
        try (Connection conn = connect(); Statement stmt = conn.createStatement()) {
            String sql = """
                CREATE TABLE IF NOT EXISTS Users (
                    UserID INTEGER PRIMARY KEY AUTOINCREMENT,
                    FullName TEXT NOT NULL,
                    Username TEXT UNIQUE NOT NULL,
                    Password TEXT NOT NULL,
                    Role TEXT NOT NULL CHECK(Role IN ('CASHIER','STORE_MANAGER','SYSTEM_ADMINISTRATOR','INVENTORY_STAFF')),
                    ContactNumber TEXT NOT NULL,
                    Status TEXT DEFAULT 'PENDING'
                );
            """;
            stmt.execute(sql);
        } catch (SQLException e) {
            System.out.println("DB Init Error: " + e.getMessage());
        }
    }
}
