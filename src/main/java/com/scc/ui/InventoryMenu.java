
package com.scc.ui;

import com.scc.dao.InventoryDao;
import com.scc.model.User;

public class InventoryMenu {

    public static void showInventoryMenu(User inventoryStaff) {

        while (true) {

            System.out.println("\n========== INVENTORY MENU ==========");
            System.out.println("1. View Inventory");
            System.out.println("2. Update Product Details");
            System.out.println("3. Delete Product");
            System.out.println("4. Back to Main Menu");
            System.out.println("====================================");

            System.out.print("Enter choice: ");
            String choice = ConsoleInput.sc.nextLine().trim();

            switch (choice) {

                case "1" -> {
                    InventoryDao.viewInventory(inventoryStaff.userId());
                    pause();
                }

                case "2" -> {
                    updateProduct(inventoryStaff);
                    pause();
                }

                case "3" -> {
                    deleteProduct(inventoryStaff);
                    pause();
                }

                case "4" -> {
                    return;
                }

                default -> System.out.println("Invalid choice.");
            }
        }
    }

    // ==========================================
    // UPDATE PRODUCT DETAILS
    // ==========================================
    private static void updateProduct(User inventoryStaff) {

        System.out.println("\n========== UPDATE PRODUCT ==========");
        System.out.println("Enter 0 at any prompt to cancel.");

        Integer productId = getPositiveInt("Enter Product ID: ");
        if (productId == null) return;

        String productName = getRequiredText("Enter new product name: ");
        if (productName == null) return;

        String category = getRequiredText("Enter new category: ");
        if (category == null) return;

        Double unitPrice = getPositiveDouble("Enter new unit price: ");
        if (unitPrice == null) return;

        String description = getRequiredText("Enter new description: ");
        if (description == null) return;

        System.out.println("\n========== UPDATE SUMMARY ==========");
        System.out.println("Product ID: " + productId);
        System.out.println("New Name: " + productName);
        System.out.println("New Category: " + category);
        System.out.println("New Price: PHP " + unitPrice);
        System.out.println("New Description: " + description);

        if (!confirmAction("Confirm product update?")) {
            System.out.println("Product update cancelled.");
            return;
        }

        InventoryDao.updateProduct(
                inventoryStaff.userId(),
                productId,
                productName,
                category,
                unitPrice,
                description
        );
    }

    // ==========================================
    // DELETE PRODUCT
    // ==========================================
    private static void deleteProduct(User inventoryStaff) {

        System.out.println("\n========== DELETE PRODUCT ==========");
        System.out.println("Enter 0 at any prompt to cancel.");

        Integer productId = getPositiveInt("Enter Product ID: ");
        if (productId == null) return;

        System.out.println("\nYou are about to delete Product ID: " + productId);

        if (!confirmAction("Are you sure you want to delete this product?")) {
            System.out.println("Product deletion cancelled.");
            return;
        }

        InventoryDao.deleteProduct(
                inventoryStaff.userId(),
                productId
        );
    }

    // ==========================================
    // INPUT METHODS
    // ==========================================
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

    // ==========================================
    // CONFIRMATION
    // ==========================================
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

    private static void pause() {
        System.out.println("\nPress ENTER to continue...");
        ConsoleInput.sc.nextLine();
    }
}
