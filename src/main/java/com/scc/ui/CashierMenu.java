package com.scc.ui;

import java.util.Scanner;

public class CashierMenu {
    private static Scanner sc = new Scanner(System.in);

    public static void showCashierMenu() {
        while (true) {
            System.out.println("\n=== CASHIER MENU ===");
            System.out.println("1. Process Sale");
            System.out.println("2. View Transactions");
            System.out.println("3. Back to Main Menu");

            String choice = sc.nextLine();
            switch (choice) {
                case "1" -> System.out.println("Processing sale... (to be implemented)");
                case "2" -> System.out.println("Viewing transactions... (to be implemented)");
                case "3" -> { return; }
                default -> System.out.println("Invalid choice.");
            }
        }
    }
}
