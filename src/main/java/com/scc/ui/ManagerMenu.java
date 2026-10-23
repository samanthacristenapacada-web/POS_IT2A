package com.scc.ui;

import java.util.Scanner;

public class ManagerMenu {
    private static Scanner sc = new Scanner(System.in);

    public static void showManagerMenu() {
        while (true) {
            System.out.println("\n=== MANAGER MENU ===");
            System.out.println("1. View Sales Reports");
            System.out.println("2. Manage Inventory");
            System.out.println("3. Monitor Cashier Activities");
            System.out.println("4. Back to Main Menu");

            String choice = sc.nextLine();
            switch (choice) {
                case "1" -> System.out.println("Viewing sales reports... (to be implemented)");
                case "2" -> System.out.println("Managing inventory... (to be implemented)");
                case "3" -> System.out.println("Monitoring cashier activities... (to be implemented)");
                case "4" -> { return; }
                default -> System.out.println("Invalid choice.");
            }
        }
    }
}
