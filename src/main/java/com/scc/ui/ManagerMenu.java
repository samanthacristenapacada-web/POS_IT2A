
package com.scc.ui;

import com.scc.dao.ManagerDao;
import com.scc.model.User;

public class ManagerMenu {

    public static void showManagerMenu(User manager) {

        while (true) {

            System.out.println("\n======================================");
            System.out.println("             MANAGER DASHBOARD");
            System.out.println("======================================");
            System.out.println("Welcome, " + manager.fullName());
            System.out.println("--------------------------------------");
            System.out.println("1. View Sales Reports");
            System.out.println("2. View Inventory");
            System.out.println("3. Add New Product");
            System.out.println("4. Restock Product");
            System.out.println("5. Monitor Cashier Activities");
            System.out.println("6. Back to Main Menu");
            System.out.println("======================================");

            int choice = getNumber("Enter choice: ", 1, 6);

            switch (choice) {

                case 1 ->
                        ManagerDao.viewSalesReport(manager.userId());

                case 2 ->
                        ManagerDao.viewInventory(manager.userId());

                case 3 ->
                        addProduct(manager);

                case 4 ->
                        restockProduct(manager);

                case 5 ->
                        ManagerDao.monitorCashierActivities(
                                manager.userId()
                        );

                case 6 -> {
                    System.out.println("Returning to main menu...");
                    return;
                }
            }

            pause();
        }
    }

    // ADD NEW PRODUCT
    private static void addProduct(User manager) {

        System.out.println("\n========== ADD NEW PRODUCT ==========");
        System.out.println("Enter 0 at any prompt to cancel.");

        String productName = getRequiredText("Enter product name: ");
        if (productName == null) return;

        String category = getRequiredText("Enter category: ");
        if (category == null) return;

        Double unitPrice = getPositiveDouble("Enter unit price: ");
        if (unitPrice == null) return;

        Integer stock = getPositiveInt("Enter initial stock: ");
        if (stock == null) return;

        String description = getRequiredText("Enter description: ");
        if (description == null) return;

        System.out.println("\n========== PRODUCT SUMMARY ==========");
        System.out.println("Product Name: " + productName);
        System.out.println("Category: " + category);
        System.out.println("Unit Price: PHP " + unitPrice);
        System.out.println("Initial Stock: " + stock);
        System.out.println("Description: " + description);

        if (!confirmAction("Confirm adding this product?")) {
            System.out.println("Product addition cancelled.");
            return;
        }

        ManagerDao.addProduct(
                manager.userId(),
                productName,
                category,
                unitPrice,
                stock,
                description
        );
    }

    // RESTOCK PRODUCT
    private static void restockProduct(User manager) {

        System.out.println("\n========== RESTOCK PRODUCT ==========");
        System.out.println("Enter 0 at any prompt to cancel.");

        Integer productId = getPositiveInt("Enter Product ID: ");
        if (productId == null) return;

        Integer quantity = getPositiveInt("Quantity to Add: ");
        if (quantity == null) return;

        System.out.println("\n========== RESTOCK SUMMARY ==========");
        System.out.println("Product ID: " + productId);
        System.out.println("Quantity to Add: " + quantity);

        if (!confirmAction("Confirm restocking this product?")) {
            System.out.println("Restocking cancelled.");
            return;
        }

        ManagerDao.updateStock(
                manager.userId(),
                productId,
                quantity
        );
    }

    // REQUIRED TEXT VALIDATION
    private static String getRequiredText(String prompt) {

        while (true) {

            System.out.print(prompt);
            String input = ConsoleInput.sc.nextLine().trim();

            if (input.equals("0")) {
                System.out.println("Operation cancelled.");
                return null;
            }

            if (!input.isEmpty()) {
                return input;
            }

            System.out.println("This field cannot be empty.");
        }
    }

    // POSITIVE INTEGER VALIDATION
    private static Integer getPositiveInt(String prompt) {

        while (true) {

            System.out.print(prompt);
            String input = ConsoleInput.sc.nextLine().trim();

            if (input.equals("0")) {
                System.out.println("Operation cancelled.");
                return null;
            }

            try {

                int value = Integer.parseInt(input);

                if (value > 0) {
                    return value;
                }

                System.out.println("Enter a number greater than zero.");

            } catch (NumberFormatException e) {
                System.out.println("Invalid input. Enter a whole number.");
            }
        }
    }

    // POSITIVE DOUBLE VALIDATION
    private static Double getPositiveDouble(String prompt) {

        while (true) {

            System.out.print(prompt);
            String input = ConsoleInput.sc.nextLine().trim();

            if (input.equals("0")) {
                System.out.println("Operation cancelled.");
                return null;
            }

            try {

                double value = Double.parseDouble(input);

                if (Double.isFinite(value) && value > 0) {
                    return value;
                }

                System.out.println("Enter a valid amount greater than zero.");

            } catch (NumberFormatException e) {
                System.out.println("Invalid amount.");
            }
        }
    }

    // CONFIRMATION
    private static boolean confirmAction(String prompt) {

        while (true) {

            System.out.print(prompt + " (Y/N): ");
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

    // MENU INTEGER VALIDATION
    private static int getNumber(String prompt, int min, int max) {

        while (true) {

            System.out.print(prompt);
            String input = ConsoleInput.sc.nextLine().trim();

            try {

                int number = Integer.parseInt(input);

                if (number >= min && number <= max) {
                    return number;
                }

            } catch (NumberFormatException ignored) {
                // Ask again if input is invalid.
            }

            System.out.println(
                    "Invalid input. Enter a number from "
                            + min + " to " + max + "."
            );
        }
    }

    // PAUSE
    private static void pause() {

        System.out.println("\nPress ENTER to continue...");
        ConsoleInput.sc.nextLine();
    }
}
