package com.scc.ui;

import java.util.Scanner;

public class InventoryMenu {
    private static Scanner sc = new Scanner(System.in);

    public static void showInventoryMenu() {
        while (true) {
            System.out.println("\n=== INVENTORY MENU ===");
            System.out.println("1. Add Product");
            System.out.println("2. Update Stock");
            System.out.println("3. View Inventory");
            System.out.println("4. Back to Main Menu");

            String choice = sc.nextLine();
            switch (choice) {
                case "1" -> System.out.println("Adding product... (to be implemented)");
                case "2" -> System.out.println("Updating stock... (to be implemented)");
                case "3" -> System.out.println("Viewing inventory... (to be implemented)");
                case "4" -> { return; }
                default -> System.out.println("Invalid choice.");
            }
        }
    }
}
