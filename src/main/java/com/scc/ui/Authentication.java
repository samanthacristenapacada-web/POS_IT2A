
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

        System.out.println("\n========== USER REGISTRATION ==========");
        System.out.println("Enter 0 at any prompt to cancel.");

        String fullName = getRegistrationInput("Enter full name: ");
        if (fullName == null) return;

        String username = getRegistrationInput("Enter username: ");
        if (username == null) return;

        String password = getRegistrationInput("Enter password: ");
        if (password == null) return;

        System.out.println("\nSelect Role:");
        System.out.println("1. SYSTEM_ADMINISTRATOR");
        System.out.println("2. CASHIER");
        System.out.println("3. STORE_MANAGER");
        System.out.println("4. INVENTORY_STAFF");
        System.out.println("0. Cancel Registration");

        String role = "";

        while (true) {

            System.out.print("Enter role choice: ");
            String choice = ConsoleInput.sc.nextLine().trim();

            if (choice.equals("0")) {
                System.out.println("Registration cancelled.");
                return;
            }

            switch (choice) {

                case "1" -> role = "SYSTEM_ADMINISTRATOR";
                case "2" -> role = "CASHIER";
                case "3" -> role = "STORE_MANAGER";
                case "4" -> role = "INVENTORY_STAFF";

                default -> {
                    System.out.println("Invalid choice. Select 1-4 or 0 to cancel.");
                    continue;
                }
            }

            break;
        }

        String contactNumber = getRegistrationInput("Enter contact number: ");
        if (contactNumber == null) return;

        System.out.println("\n========== REGISTRATION SUMMARY ==========");
        System.out.println("Full Name: " + fullName);
        System.out.println("Username: " + username);
        System.out.println("Role: " + role);
        System.out.println("Contact Number: " + contactNumber);
        System.out.println("Password: [Hidden]");

        if (!confirmRegistration()) {
            System.out.println("Registration cancelled.");
            return;
        }

        UserDao.register(
                fullName,
                username,
                password,
                role,
                contactNumber
        );
    }

    private static String getRegistrationInput(String prompt) {

        while (true) {

            System.out.print(prompt);
            String input = ConsoleInput.sc.nextLine().trim();

            if (input.equals("0")) {
                System.out.println("Registration cancelled.");
                return null;
            }

            if (!input.isEmpty()) {
                return input;
            }

            System.out.println("This field cannot be empty.");
        }
    }

    private static boolean confirmRegistration() {

        while (true) {

            System.out.print("Confirm registration? (Y/N): ");
            String answer = ConsoleInput.sc.nextLine().trim();

            if (answer.equalsIgnoreCase("Y")) {
                return true;
            }

            if (answer.equalsIgnoreCase("N") || answer.equals("0")) {
                return false;
            }

            System.out.println("Enter Y to confirm or N/0 to cancel.");
        }
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
                    ManagerMenu.showManagerMenu(user);

            case "INVENTORY_STAFF" ->
                    InventoryMenu.showInventoryMenu(user);

            default ->
                    System.out.println("Unknown user role.");
        }
    }
}
