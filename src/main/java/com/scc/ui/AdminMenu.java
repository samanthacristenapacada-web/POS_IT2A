package com.scc.ui;

import com.scc.dao.DatabaseConnection;
import java.sql.*;
import java.util.Scanner;

public class AdminMenu {
    private static Scanner sc = new Scanner(System.in);

    public static void showAdminMenu() {
        while (true) {
            System.out.println("\n=== ADMIN MENU ===");
            System.out.println("1. View All Users");
            System.out.println("2. Approve User (set ACTIVE)");
            System.out.println("3. Deactivate User (set INACTIVE)");
            System.out.println("4. Back to Main Menu");

            int choice = -1;
            while (true) {
                System.out.print("Choose: ");
                String input = sc.nextLine();

                if (input.matches("\\d+")) { // validate numeric input
                    choice = Integer.parseInt(input);
                    break;
                } else {
                    System.out.println("Invalid input. Please enter a number (1-4).");
                }
            }

            switch (choice) {
                case 1 -> viewAllUsers();
                case 2 -> updateUserStatus("ACTIVE");
                case 3 -> updateUserStatus("INACTIVE");
                case 4 -> {
                    return;
                }
                default -> System.out.println("Invalid choice. Please enter 1–4.");
            }
        }
    }


    private static void viewAllUsers() {
        String sql = "SELECT * FROM Users";
        try (Connection conn = DatabaseConnection.connect();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            System.out.println("\n--- User Accounts ---");
            while (rs.next()) {
                System.out.printf("ID: %d | Name: %s | Role: %s | Status: %s%n",
                        rs.getInt("UserID"),
                        rs.getString("FullName"),
                        rs.getString("Role"),
                        rs.getString("Status"));
            }
        } catch (SQLException e) {
            System.out.println("Error viewing users: " + e.getMessage());
        }
    }

    private static void updateUserStatus(String newStatus) {
        int userId = -1;
        while (true) {
            System.out.print("Enter UserID to update: ");
            String input = sc.nextLine();

            if (input.matches("\\d+")) { // validate numeric input
                userId = Integer.parseInt(input);
                break;
            } else {
                System.out.println("Invalid input. Please enter a numeric UserID.");
            }
        }

        String sql = "UPDATE Users SET Status=? WHERE UserID=?";
        try (Connection conn = DatabaseConnection.connect();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, newStatus);
            pstmt.setInt(2, userId);
            int rows = pstmt.executeUpdate();
            if (rows > 0) {
                System.out.println("User status updated to " + newStatus + ".");
            } else {
                System.out.println("No user found with that ID.");
            }
        } catch (SQLException e) {
            System.out.println("Error updating status: " + e.getMessage());
        }
    }
}
