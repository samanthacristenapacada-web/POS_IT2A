
package com.scc.ui;

import com.scc.dao.UserDao;
import com.scc.dao.ActivityLogDao;
import com.scc.model.User;

public class Authentication {

    public static void start() {

        while (true) {
            System.out.println("\n========== POS SYSTEM ==========");
            System.out.println("1. Register");
            System.out.println("2. Login");
            System.out.println("3. Exit");

            int choice = getValidatedChoice(1, 3);

            switch (choice) {
                case 1 -> registerFlow();
                case 2 -> loginFlow();
                case 3 -> {
                    System.out.println("Goodbye!");
                    return;
                }
            }
        }
    }

    private static int getValidatedChoice(int min, int max) {

        while (true) {
            System.out.print("Choose option (" + min + "-" + max + "): ");
            String input = ConsoleInput.sc.nextLine().trim();

            try {
                int choice = Integer.parseInt(input);

                if (choice >= min && choice <= max) {
                    return choice;
                }
            } catch (NumberFormatException ignored) {
                // Ask again for invalid input.
            }

            System.out.println("Invalid input. Please try again.");
        }
    }

    private static void registerFlow() {

        System.out.print("Full Name: ");
        String fullName = ConsoleInput.sc.nextLine().trim();

        System.out.print("Username: ");
        String username = ConsoleInput.sc.nextLine().trim();

        System.out.print("Password: ");
        String password = ConsoleInput.sc.nextLine().trim();

        System.out.println("\nSelect Role:");
        System.out.println("1. SYSTEM_ADMINISTRATOR");
        System.out.println("2. CASHIER");
        System.out.println("3. STORE_MANAGER");
        System.out.println("4. INVENTORY_STAFF");

        int roleChoice = getValidatedChoice(1, 4);

        String role;
        if (roleChoice == 1) {
            role = "SYSTEM_ADMINISTRATOR";
        } else if (roleChoice == 2) {
            role = "CASHIER";
        } else if (roleChoice == 3) {
            role = "STORE_MANAGER";
        } else {
            role = "INVENTORY_STAFF";
        }

        System.out.print("Contact Number: ");
        String contact = ConsoleInput.sc.nextLine().trim();

        if (fullName.isEmpty() || username.isEmpty()
                || password.isEmpty() || contact.isEmpty()) {
            System.out.println("All fields are required.");
            return;
        }

        boolean success = UserDao.register(
                fullName, username, password, role, contact
        );

        System.out.println(
                success
                        ? "Registration successful (status: PENDING)."
                        : "Registration failed."
        );
    }

    private static void loginFlow() {

        System.out.print("Username: ");
        String username = ConsoleInput.sc.nextLine().trim();

        System.out.print("Password: ");
        String password = ConsoleInput.sc.nextLine().trim();

        User user = UserDao.login(username, password);

        if (user == null) {
            System.out.println("Login failed or account is not ACTIVE.");
            return;
        }

        System.out.println("\nWelcome, " + user.fullName()
                + " (" + user.role() + ")");

        System.out.println("Contact: " + user.contactNumber());
        System.out.println("Status: " + user.status());

        ActivityLogDao.logAction(
                user.userId(),
                "LOGIN",
                "Users",
                "User successfully logged in."
        );

        routeToMenu(user);

        ActivityLogDao.logAction(
                user.userId(),
                "LOGOUT",
                "Users",
                "User logged out of the POS system."
        );

        System.out.println("Session ended.");
    }

    private static void routeToMenu(User user) {

        switch (user.role().toUpperCase()) {

            case "SYSTEM_ADMINISTRATOR" ->
                    AdminMenu.showAdminMenu(user);

            case "CASHIER" ->
                    CashierMenu.showCashierMenu();

            case "STORE_MANAGER" ->
                    ManagerMenu.showManagerMenu();

            case "INVENTORY_STAFF" ->
                    InventoryMenu.showInventoryMenu();

            default ->
                    System.out.println("Unknown user role.");
        }
    }
}
