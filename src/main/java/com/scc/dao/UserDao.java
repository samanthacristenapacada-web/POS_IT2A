package com.scc.dao;

import com.scc.model.User;
import java.sql.*;

public class UserDao {

    public static boolean register(String fullName, String username, String password, String role, String contactNumber) {
        String sql = "INSERT INTO Users(FullName, Username, Password, Role, ContactNumber, Status) VALUES(?,?,?,?,?,?)";
        try (Connection conn = DatabaseConnection.connect();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, fullName);
            pstmt.setString(2, username);
            pstmt.setString(3, password);
            pstmt.setString(4, role.toUpperCase());
            pstmt.setString(5, contactNumber);
            pstmt.setString(6, "PENDING");
            pstmt.executeUpdate();
            return true;
        } catch (SQLException e) {
            System.out.println("Register Error: " + e.getMessage());
            return false;
        }
    }

    public static User login(String username, String password) {
        String sql = "SELECT * FROM Users WHERE Username=? AND Password=? AND Status='ACTIVE'";
        try (Connection conn = DatabaseConnection.connect();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, username);
            pstmt.setString(2, password);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return new User(
                        rs.getInt("UserID"),
                        rs.getString("FullName"),
                        rs.getString("Username"),
                        rs.getString("Password"),
                        rs.getString("Role"),
                        rs.getString("ContactNumber"),
                        rs.getString("Status")
                );
            }
        } catch (SQLException e) {
            System.out.println("Login Error: " + e.getMessage());
        }
        return null;
    }
}