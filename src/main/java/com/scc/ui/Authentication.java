package com.scc.ui;

import com.scc.dao.UserDao;
import com.scc.model.User;

import java.util.Scanner;

public class Authentication {

    private static final Scanner sc =
            new Scanner(System.in);

    public static void start() {

        while (true) {

            System.out.println("""
                    
                    ========================================
                              POS SYSTEM
                    ========================================
                    
                    1. Register
                    2. Login
                    3. Exit
                    """);

            int choice =
                    getValidatedChoice(1, 3);

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

    private static int getValidatedChoice(
            int min,
            int max) {

        while (true) {

            System.out.print(
                    "Choose option (" +
                            min +
                            "-" +
                            max +
                            "): ");

            String input =
                    sc.nextLine();

            if (input.matches("\\d+")) {

                int choice =
                        Integer.parseInt(input);

                if (choice >= min &&
                        choice <= max) {

                    return choice;
                }
            }

            System.out.println(
                    "Invalid input. Please enter a number between "
                            + min + " and " + max + ".");
        }
    }

    private static void registerFlow() {

        System.out.print(
                "Full Name: ");

        String fullName =
                sc.nextLine();

        System.out.print(
                "Username: ");

        String username =
                sc.nextLine();

        System.out.print(
                "Password: ");

        String password =
                sc.nextLine();

        System.out.println("""
                
                Select Role:
                1. CASHIER
                2. STORE_MANAGER
                3. SYSTEM_ADMINISTRATOR
                4. INVENTORY_STAFF
                """);

        int roleChoice =
                getValidatedChoice(1, 4);

        String role =
                switch (roleChoice) {

                    case 1 -> "CASHIER";
                    case 2 -> "STORE_MANAGER";
                    case 3 -> "SYSTEM_ADMINISTRATOR";
                    case 4 -> "INVENTORY_STAFF";
                    default -> "";
                };

        System.out.print(
                "Contact Number: ");

        String contact =
                sc.nextLine();

        boolean success =
                UserDao.register(
                        fullName,
                        username,
                        password,
                        role,
                        contact);

        System.out.println(
                success
                        ? "Registration successful (Status: PENDING)."
                        : "Registration failed.");
    }

    private static void loginFlow() {

        System.out.print(
                "Username: ");

        String username =
                sc.nextLine();

        System.out.print(
                "Password: ");

        String password =
                sc.nextLine();

        User user =
                UserDao.login(
                        username,
                        password);

        if (user == null) {

            System.out.println(
                    "Login failed or account not ACTIVE.");

            return;
        }

        System.out.println(
                "\nWelcome, "
                        + user.fullName()
                        + " ("
                        + user.role()
                        + ")");

        System.out.println(
                "Contact: "
                        + user.contactNumber());

        System.out.println(
                "Status: "
                        + user.status());

        routeToMenu(user);
    }

    private static void routeToMenu(
            User user) {

        switch (
                user.role()
                        .toUpperCase()) {

            case "SYSTEM_ADMINISTRATOR" ->
                    AdminMenu.showAdminMenu(user);

            case "CASHIER" ->
                    CashierMenu.showCashierMenu();

            case "STORE_MANAGER" ->
                    ManagerMenu.showManagerMenu();

            case "INVENTORY_STAFF" ->
                    InventoryMenu.showInventoryMenu();

            default ->
                    System.out.println(
                            "Unknown role.");
        }
    }
}