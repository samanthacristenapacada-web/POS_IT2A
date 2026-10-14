package com.scc;

import com.scc.dao.DatabaseConnection;
import com.scc.dao.UserDao;
import com.scc.model.User;
import com.scc.ui.AdminMenu;
import java.util.Scanner;

public class Main {
    private static Scanner sc = new Scanner(System.in);

    public static void main(String[] args) {
        DatabaseConnection.initialize();
        mainMenu();
    }

    public static void mainMenu() {
        while (true) {
            System.out.println("\n=== POS AUTH SYSTEM ===");
            System.out.println("1. Register");
            System.out.println("2. Login");
            System.out.println("3. Exit");

            int choice = -1;
            while (true) {
                System.out.print("Choose: ");
                String input = sc.nextLine();

                if (input.matches("\\d+")) {
                    choice = Integer.parseInt(input);
                    break;
                } else {
                    System.out.println("Invalid input. Please enter a number (1-3).");
                }
            }

            switch (choice) {
                case 1 -> registerScreen();
                case 2 -> loginScreen();
                case 3 -> { System.out.println("Exiting..."); return; }
                default -> System.out.println("Invalid choice. Please enter 1, 2, or 3.");
            }
        }
    }


    private static void registerScreen() {
        System.out.print("Full Name: ");
        String fullName = sc.nextLine();
        System.out.print("Username: ");
        String username = sc.nextLine();
        System.out.print("Password: ");
        String password = sc.nextLine();

        // Role selection with numeric choices and validation
        System.out.println("Select Role:");
        System.out.println("1. CASHIER");
        System.out.println("2. STORE_MANAGER");
        System.out.println("3. SYSTEM_ADMINISTRATOR");
        System.out.println("4. INVENTORY_STAFF");

        int roleChoice = -1;
        while (true) {
            System.out.print("Choose (1-4): ");
            String input = sc.nextLine();

            if (input.matches("\\d+")) {
                roleChoice = Integer.parseInt(input);
                if (roleChoice >= 1 && roleChoice <= 4) {
                    break;
                } else {
                    System.out.println("Invalid choice. Please enter a number between 1 and 4.");
                }
            } else {
                System.out.println("Invalid input. Please enter a number only.");
            }
        }

        String role = switch (roleChoice) {
            case 1 -> "CASHIER";
            case 2 -> "STORE_MANAGER";
            case 3 -> "SYSTEM_ADMINISTRATOR";
            case 4 -> "INVENTORY_STAFF";
            default -> null;
        };

        System.out.print("Contact Number: ");
        String contactNumber = sc.nextLine();

        if (UserDao.register(fullName, username, password, role, contactNumber)) {
            System.out.println("Account registered successfully (status: PENDING).");
        }
    }



    private static void loginScreen() {
        System.out.print("Username: ");
        String username = sc.nextLine();
        System.out.print("Password: ");
        String password = sc.nextLine();

        User user = UserDao.login(username, password);
        if (user != null) {
            if (!user.Status().equals("ACTIVE")) {
                System.out.println("Your account is not active. Please wait for admin approval.");
                return;
            }

            System.out.println("Welcome, " + user.FullName() + " (" + user.Role() + ")");
            System.out.println("Contact: " + user.ContactNumber());
            System.out.println("Status: " + user.Status());

            if (user.Role().equals("SYSTEM_ADMINISTRATOR")) {
                com.scc.ui.AdminMenu.showAdminMenu();
            }
        } else {
            System.out.println("Login failed.");
        }
    }
}
