package com.scc.ui;

import com.scc.dao.UserDao;
import com.scc.model.User;
import java.util.Scanner;

public class Authentication {
    private static final Scanner sc = new Scanner(System.in);

    public static void start() {
        while (true) {
            System.out.println("\n=== POS SYSTEM ===");
            System.out.println("1. Register");
            System.out.println("2. Login");
            System.out.println("3. Exit");

            int choice = getValidatedChoice(1, 3);
            switch (choice) {
                case 1 -> registerFlow();
                case 2 -> loginFlow();
                case 3 -> { System.out.println("Goodbye!"); return; }
                default -> System.out.println("Invalid choice.");
            }
        }
    }

    private static int getValidatedChoice(int min, int max) {
        while (true) {
            System.out.print("Choose option (" + min + "-" + max + "): ");
            String input = sc.nextLine();
            if (input.matches("\\d+")) {
                int choice = Integer.parseInt(input);
                if (choice >= min && choice <= max) return choice;
            }
            System.out.println("Invalid input. Please enter a number between " + min + " and " + max + ".");
        }
    }

    private static void registerFlow() {
        System.out.print("Full Name: ");
        String fullName = sc.nextLine();
        System.out.print("Username: ");
        String username = sc.nextLine();
        System.out.print("Password: ");
        String password = sc.nextLine();

        // Role selection with numeric choices
        System.out.println("Select Role:");
        System.out.println("1. SYSTEM_ADMINISTRATOR");
        System.out.println("2. CASHIER");
        System.out.println("3. STORE_MANAGER");
        System.out.println("4. INVENTORY_STAFF");

        int roleChoice = getValidatedChoice(1, 4);
        String role = switch (roleChoice) {
            case 1 -> "SYSTEM_ADMINISTRATOR";
            case 2 -> "CASHIER";
            case 3 -> "STORE_MANAGER";
            case 4 -> "INVENTORY_STAFF";
            default -> null;
        };

        System.out.print("Contact Number: ");
        String contact = sc.nextLine();

        boolean success = UserDao.register(fullName, username, password, role, contact);
        System.out.println(success ? "Registration successful (status: PENDING)." : "Registration failed.");
    }

    private static void loginFlow() {
        System.out.print("Username: ");
        String username = sc.nextLine();
        System.out.print("Password: ");
        String password = sc.nextLine();

        User user = UserDao.login(username, password);
        if (user == null) {
            System.out.println("Login failed or account not ACTIVE.");
            return;
        }

        // Correct camelCase accessors from User.java
        System.out.println("Welcome, " + user.FullName() + " (" + user.Role() + ")");
        System.out.println("Contact: " + user.ContactNumber());
        System.out.println("Status: " + user.Status());

        routeToMenu(user);
    }

    private static void routeToMenu(User user) {
        switch (user.Role().toUpperCase()) {
            case "SYSTEM_ADMINISTRATOR" -> AdminMenu.showAdminMenu();
            case "CASHIER" -> CashierMenu.showCashierMenu();
            case "STORE_MANAGER" -> ManagerMenu.showManagerMenu();
            case "INVENTORY_STAFF" -> InventoryMenu.showInventoryMenu();
            default -> System.out.println("Unknown role.");
        }
    }
}
