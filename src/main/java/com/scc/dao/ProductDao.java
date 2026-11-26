package com.scc.dao;

import com.scc.model.Product;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class ProductDao {

    // Get products that have available stock.
    public static List<Product> getAvailableProducts() {

        List<Product> products = new ArrayList<>();

        String sql = """
                SELECT ProductID, ProductName, Category,
                       UnitPrice, StockQuantity, Description
                FROM Products
                WHERE StockQuantity > 0
                ORDER BY ProductName
                """;

        try (Connection conn = DatabaseConnection.connect();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {

            while (rs.next()) {
                products.add(mapProduct(rs));
            }

        } catch (SQLException e) {
            System.out.println(
                    "Error retrieving products: " + e.getMessage()
            );
        }

        return products;
    }

    // Find one product using its ID.
    public static Product getProductById(int productId) {

        String sql = """
                SELECT ProductID, ProductName, Category,
                       UnitPrice, StockQuantity, Description
                FROM Products
                WHERE ProductID = ?
                """;

        try (Connection conn = DatabaseConnection.connect();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, productId);

            try (ResultSet rs = pstmt.executeQuery()) {

                if (rs.next()) {
                    return mapProduct(rs);
                }
            }

        } catch (SQLException e) {
            System.out.println(
                    "Product lookup error: " + e.getMessage()
            );
        }

        return null;
    }

    // Search products by product name or category.
    // Every completed search is recorded in Activity_Logs.
    public static List<Product> searchProducts(
            String keyword,
            int userId) {

        List<Product> products = new ArrayList<>();

        String sql = """
                SELECT ProductID, ProductName, Category,
                       UnitPrice, StockQuantity, Description
                FROM Products
                WHERE ProductName LIKE ?
                   OR Category LIKE ?
                ORDER BY ProductName
                """;

        try (Connection conn = DatabaseConnection.connect();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            String searchValue = "%" + keyword.trim() + "%";

            pstmt.setString(1, searchValue);
            pstmt.setString(2, searchValue);

            try (ResultSet rs = pstmt.executeQuery()) {

                while (rs.next()) {
                    products.add(mapProduct(rs));
                }
            }

            ActivityLogDao.logAction(
                    conn,
                    userId,
                    "PRODUCT_SEARCHED",
                    "Products",
                    "Cashier searched for: " + keyword
                            + ". Results found: " + products.size()
            );

        } catch (SQLException e) {
            System.out.println(
                    "Product search error: " + e.getMessage()
            );
        }

        return products;
    }

    private static Product mapProduct(ResultSet rs)
            throws SQLException {

        return new Product(
                rs.getInt("ProductID"),
                rs.getString("ProductName"),
                rs.getString("Category"),
                rs.getDouble("UnitPrice"),
                rs.getInt("StockQuantity"),
                rs.getString("Description")
        );
    }
}