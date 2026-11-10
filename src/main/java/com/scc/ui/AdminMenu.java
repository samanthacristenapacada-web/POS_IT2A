package com.scc.ui;

import com.scc.dao.ActivityLogDao;
import com.scc.dao.UserDao;
import com.scc.model.ActivityLog;
import com.scc.model.User;

import java.util.List;
import java.util.Scanner;

public class AdminMenu {

    private static final Scanner sc = ConsoleInput.sc;

    public static void showAdminMenu(User currentAdmin) {

        while (true) {

            System.out.println("""

                    ========================================
                              ADMIN DASHBOARD
                    ========================================

                    1. View All Users
                    2. View Pending User Approvals
                    3. Approve User Registration
                    4. Add New User
                    5. Update User Role / Status
                    6. Delete User Account
                    7. View System Audit Logs
                    8. Logout
                    """);

            int choice =
                    readIntInput(
                            "Choose an option (1-8): ");

            switch (choice) {

                case 1 -> viewAllUsers();

                case 2 -> viewPendingUsers();

                case 3 -> approveUserFlow(currentAdmin);

                case 4 -> addUserFlow();

                case 5 -> updateUserRoleAndStatusFlow(currentAdmin);

                case 6 -> deleteUserFlow(currentAdmin);

                case 7 -> auditLogsMenu();

                case 8 -> {
                    System.out.println("Logging out...");
                    return;
                }

                default ->
                        System.out.println("Invalid option.");
            }
        }
    }

    // ==================================================
    // USER MANAGEMENT
    // ==================================================

    private static void viewAllUsers() {

        displayUsers(
                UserDao.getAllUsers());
    }

    private static void viewPendingUsers() {

        displayUsers(
                UserDao.getPendingUsers());
    }

    private static void approveUserFlow(
            User currentAdmin) {

        List<User> users =
                UserDao.getPendingUsers();

        if (users.isEmpty()) {

            System.out.println(
                    "No pending users found.");

            return;
        }

        displayUsers(users);

        int userId =
                readIntInput(
                        "Enter User ID to approve (0 = cancel): ");

        if (userId == 0) {
            System.out.println("Approval cancelled.");
            return;
        }

        boolean updated =
                UserDao.updateUserStatus(
                        userId,
                        "ACTIVE");

        if (updated) {

            ActivityLogDao.logAction(
                    currentAdmin.userId(),
                    "USER_APPROVED",
                    "Users",
                    "Approved account for User ID "
                            + userId
            );

            System.out.println(
                    "User approved successfully.");

        } else {

            System.out.println(
                    "User not found.");
        }
    }

    private static void addUserFlow() {

        System.out.print("Full Name: ");
        String fullName =
                sc.nextLine();

        System.out.print("Username: ");
        String username =
                sc.nextLine();

        System.out.print("Password: ");
        String password =
                sc.nextLine();

        String role =
                readValidRole();

        if (role == null) {

            System.out.println(
                    "User creation cancelled.");

            return;
        }

        System.out.print("Contact Number: ");
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
                        ? "User created successfully."
                        : "Failed to create user.");
    }

    private static void updateUserRoleAndStatusFlow(
            User currentAdmin) {

        displayUsers(
                UserDao.getAllUsers());

        int userId =
                readIntInput(
                        "Enter User ID (0 = cancel): ");

        if (userId == 0) {
            System.out.println("Update cancelled.");
            return;
        }

        if (userId ==
                currentAdmin.userId()) {

            System.out.println(
                    "WARNING: Modifying the currently active admin account is restricted.");

            return;
        }

        String role =
                readValidRole();

        if (role == null) {

            System.out.println(
                    "Update cancelled.");

            return;
        }

        String status =
                readValidStatus();

        if (status == null) {

            System.out.println(
                    "Update cancelled.");

            return;
        }

        boolean updated =
                UserDao.updateUserRoleAndStatus(
                        userId,
                        role,
                        status);

        if (updated) {

            ActivityLogDao.logAction(
                    currentAdmin.userId(),
                    "USER_UPDATED",
                    "Users",
                    "Updated User ID "
                            + userId
                            + " Role="
                            + role
                            + " Status="
                            + status
            );

            System.out.println(
                    "User updated successfully.");

        } else {

            System.out.println(
                    "User not found.");
        }
    }

    private static void deleteUserFlow(
            User currentAdmin) {

        displayUsers(
                UserDao.getAllUsers());

        int userId =
                readIntInput(
                        "Enter User ID to delete (0 = cancel): ");

        if (userId == 0) {
            System.out.println("Delete cancelled.");
            return;
        }

        if (userId ==
                currentAdmin.userId()) {

            System.out.println(
                    "WARNING: Modifying the currently active admin account is restricted.");

            return;
        }

        boolean deleted =
                UserDao.deleteUser(userId);

        if (deleted) {

            ActivityLogDao.logAction(
                    currentAdmin.userId(),
                    "USER_DELETED",
                    "Users",
                    "Deleted User ID "
                            + userId
            );

            System.out.println(
                    "User deleted successfully.");

        } else {

            System.out.println(
                    "User not found.");
        }
    }

    private static void displayUsers(
            List<User> users) {

        System.out.println();

        System.out.printf(
                "%-5s %-20s %-20s %-22s %-15s%n",
                "ID",
                "FULL NAME",
                "USERNAME",
                "ROLE",
                "STATUS");

        System.out.println(
                "--------------------------------------------------------------------------------");

        for (User user : users) {

            System.out.printf(
                    "%-5d %-20s %-20s %-22s %-15s%n",
                    user.userId(),
                    user.fullName(),
                    user.username(),
                    user.role(),
                    user.status());
        }
    }

    // ==================================================
    // AUDIT LOGS
    // ==================================================

    private static void auditLogsMenu() {

        while (true) {

            System.out.println("""

                    ==============================
                         SYSTEM AUDIT LOGS
                    ==============================

                    1. View All System Logs
                    2. Filter Logs By User ID
                    3. Back
                    """);

            int choice =
                    readIntInput(
                            "Choose option (1-3): ");

            switch (choice) {

                case 1 -> viewAllLogs();

                case 2 -> filterLogsByUserId();

                case 3 -> {
                    return;
                }

                default ->
                        System.out.println(
                                "Invalid option.");
            }
        }
    }

    private static void viewAllLogs() {

        List<ActivityLog> logs =
                ActivityLogDao.getAllLogs();

        if (logs.isEmpty()) {

            System.out.println(
                    "No logs found.");

            return;
        }

        System.out.printf(
                "%-5s | %-8s | %-18s | %-35s | %-20s%n",
                "ID",
                "USER",
                "ACTION",
                "DESCRIPTION",
                "TIMESTAMP");

        System.out.println(
                "-----------------------------------------------------------------------------------------------------");

        for (ActivityLog log : logs) {

            System.out.printf(
                    "%-5d | %-8d | %-18s | %-35s | %-20s%n",
                    log.logId(),
                    log.userId(),
                    log.action(),
                    log.description(),
                    log.timestamp());
        }
    }

    private static void filterLogsByUserId() {

        int userId =
                readIntInput(
                        "Enter User ID: ");

        List<ActivityLog> logs =
                ActivityLogDao.getLogsByUserId(
                        userId);

        if (logs.isEmpty()) {

            System.out.println(
                    "No logs found for User ID "
                            + userId);

            return;
        }

        System.out.printf(
                "%-5s | %-8s | %-18s | %-35s | %-20s%n",
                "ID",
                "USER",
                "ACTION",
                "DESCRIPTION",
                "TIMESTAMP");

        System.out.println(
                "-----------------------------------------------------------------------------------------------------");

        for (ActivityLog log : logs) {

            System.out.printf(
                    "%-5d | %-8d | %-18s | %-35s | %-20s%n",
                    log.logId(),
                    log.userId(),
                    log.action(),
                    log.description(),
                    log.timestamp());
        }
    }

    // ==================================================
    // VALIDATION HELPERS
    // ==================================================

    private static int readIntInput(
            String prompt) {

        while (true) {

            System.out.print(prompt);

            String input =
                    sc.nextLine();

            if (input.matches("\\d+")) {

                return Integer.parseInt(
                        input);
            }

            System.out.println(
                    "Invalid number.");
        }
    }

    private static String readValidRole() {

        while (true) {

            System.out.println("""

                    Select Role
                    1. CASHIER
                    2. STORE_MANAGER
                    3. SYSTEM_ADMINISTRATOR
                    4. INVENTORY_STAFF
                    0. Cancel
                    """);

            System.out.print(
                    "Enter role option: ");

            String input =
                    sc.nextLine();

            switch (input) {

                case "1":
                    return "CASHIER";

                case "2":
                    return "STORE_MANAGER";

                case "3":
                    return "SYSTEM_ADMINISTRATOR";

                case "4":
                    return "INVENTORY_STAFF";

                case "0":
                    return null;

                default:
                    System.out.println(
                            "Invalid role selection.");
            }
        }
    }

    private static String readValidStatus() {

        while (true) {

            System.out.println("""

                    Select Status
                    1. ACTIVE
                    2. PENDING
                    3. INACTIVE
                    0. Cancel
                    """);

            System.out.print(
                    "Enter status option: ");

            String input =
                    sc.nextLine();

            switch (input) {

                case "1":
                    return "ACTIVE";

                case "2":
                    return "PENDING";

                case "3":
                    return "INACTIVE";

                case "0":
                    return null;

                default:
                    System.out.println(
                            "Invalid status selection.");
            }
        }
    }
}