package com.scc.ui;

import com.scc.dao.ProductDao;
import com.scc.dao.SalesDao;
import com.scc.model.Product;
import com.scc.model.SaleReceipt;
import com.scc.model.User;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class CashierMenu {

    public static void showCashierMenu(User cashier) {

        while (true) {

            System.out.println("""

                    =================================
                              CASHIER MENU
                    =================================

                    1. Process Sale
                    2. View My Transactions
                    3. Search Products
                    4. Back to Main Menu

                    """);

            int choice = readInt(
                    "Choose an option (1-4): ",
                    1,
                    4,
                    false
            );

            switch (choice) {

                case 1 -> processSale(cashier);

                case 2 -> viewTransactions(cashier);

                case 3 -> searchProducts(cashier);

                case 4 -> {
                    System.out.println("Returning to main menu...");
                    return;
                }
            }
        }
    }

    // ==========================================
    // PROCESS SALE
    // ==========================================
    private static void processSale(User cashier) {

        Map<Integer, Integer> cart = new LinkedHashMap<>();

        while (true) {

            System.out.println("""

                    ========== SHOPPING CART ==========

                    1. Add Product
                    2. Change Product Quantity
                    3. Remove Product
                    4. Checkout
                    5. Cancel Sale

                    """);

            int choice = readInt(
                    "Choose an option (1-5): ",
                    1,
                    5,
                    false
            );

            switch (choice) {

                case 1 -> addProductToCart(cart);

                case 2 -> changeProductQuantity(cart);

                case 3 -> removeProductFromCart(cart);

                case 4 -> {

                    if (cart.isEmpty()) {
                        System.out.println("Your cart is empty.");
                        break;
                    }

                    BigDecimal total = displayCart(cart);

                    if (total == null) {
                        System.out.println(
                                "Unable to calculate cart total."
                        );
                        break;
                    }

                    if (!confirm("Proceed to checkout?")) {
                        System.out.println("Checkout cancelled.");
                        break;
                    }

                    BigDecimal amountPaid =
                            readPaymentAmount(total);

                    if (amountPaid == null) {
                        System.out.println("Payment cancelled.");
                        break;
                    }

                    SaleReceipt receipt =
                            SalesDao.completeSale(
                                    cashier.userId(),
                                    cart,
                                    amountPaid
                            );

                    if (receipt != null) {
                        printReceipt(cashier, receipt);
                        return;
                    }

                    System.out.println(
                            "Please review your cart and try again."
                    );
                }

                case 5 -> {
                    if (confirm("Cancel this sale?")) {
                        System.out.println("Sale cancelled.");
                        return;
                    }
                }
            }
        }
    }

    // ==========================================
    // ADD PRODUCT TO CART
    // ==========================================
    private static void addProductToCart(
            Map<Integer, Integer> cart) {

        displayAvailableProducts();

        int productId = readInt(
                "\nEnter Product ID (0 = back): ",
                0,
                Integer.MAX_VALUE,
                true
        );

        if (productId == 0) {
            return;
        }

        Product product = ProductDao.getProductById(productId);

        if (product == null) {
            System.out.println("Product not found.");
            return;
        }

        if (product.stockQuantity() <= 0) {
            System.out.println("This product is out of stock.");
            return;
        }

        System.out.println(
                "Selected: " + product.productName()
        );

        System.out.println(
                "Available Stock: " + product.stockQuantity()
        );

        int quantity = readInt(
                "Enter quantity: ",
                1,
                Integer.MAX_VALUE,
                false
        );

        int currentQuantity = cart.getOrDefault(productId, 0);
        int newQuantity = currentQuantity + quantity;

        if (newQuantity > product.stockQuantity()) {
            System.out.println(
                    "Not enough stock. Available quantity: "
                            + product.stockQuantity()
            );
            return;
        }

        cart.put(productId, newQuantity);

        System.out.println(
                "Product added to cart successfully."
        );
    }

    // ==========================================
    // CHANGE PRODUCT QUANTITY
    // ==========================================
    private static void changeProductQuantity(
            Map<Integer, Integer> cart) {

        if (cart.isEmpty()) {
            System.out.println("Your cart is empty.");
            return;
        }

        displayCart(cart);

        int productId = readInt(
                "Enter Product ID to change (0 = back): ",
                0,
                Integer.MAX_VALUE,
                true
        );

        if (productId == 0) {
            return;
        }

        if (!cart.containsKey(productId)) {
            System.out.println("Product is not in your cart.");
            return;
        }

        int quantity = readInt(
                "Enter new quantity (0 = remove): ",
                0,
                Integer.MAX_VALUE,
                true
        );

        if (quantity == 0) {
            cart.remove(productId);
            System.out.println("Product removed from cart.");
            return;
        }

        Product product = ProductDao.getProductById(productId);

        if (product == null
                || quantity > product.stockQuantity()) {

            System.out.println("Requested quantity is unavailable.");
            return;
        }

        cart.put(productId, quantity);

        System.out.println("Cart quantity updated.");
    }

    // ==========================================
    // REMOVE PRODUCT FROM CART
    // ==========================================
    private static void removeProductFromCart(
            Map<Integer, Integer> cart) {

        if (cart.isEmpty()) {
            System.out.println("Your cart is empty.");
            return;
        }

        displayCart(cart);

        int productId = readInt(
                "Enter Product ID to remove (0 = back): ",
                0,
                Integer.MAX_VALUE,
                true
        );

        if (productId == 0) {
            return;
        }

        if (cart.remove(productId) != null) {
            System.out.println("Product removed successfully.");
        } else {
            System.out.println("Product is not in your cart.");
        }
    }

    // ==========================================
    // DISPLAY AVAILABLE PRODUCTS
    // ==========================================
    private static void displayAvailableProducts() {

        List<Product> products =
                ProductDao.getAvailableProducts();

        System.out.println("\n========== AVAILABLE PRODUCTS ==========");

        System.out.printf(
                "%-8s %-25s %-16s %-15s %-10s%n",
                "ID",
                "PRODUCT",
                "CATEGORY",
                "PRICE",
                "STOCK"
        );

        System.out.println(
                "--------------------------------------------------------------------------"
        );

        if (products.isEmpty()) {
            System.out.println("No available products.");
            return;
        }

        for (Product product : products) {

            System.out.printf(
                    "%-8d %-25s %-16s PHP %-10.2f %-10d%n",
                    product.productId(),
                    shorten(product.productName(), 24),
                    shorten(product.category(), 15),
                    product.unitPrice(),
                    product.stockQuantity()
            );
        }

        System.out.println(
                "=========================================================================="
        );
    }

    // ==========================================
    // DISPLAY CART AND CALCULATE TOTAL
    // ==========================================
    private static BigDecimal displayCart(
            Map<Integer, Integer> cart) {

        BigDecimal total = BigDecimal.ZERO;

        System.out.println("\n========== CART SUMMARY ==========");

        System.out.printf(
                "%-8s %-23s %-8s %-12s %-12s%n",
                "ID",
                "PRODUCT",
                "QTY",
                "PRICE",
                "SUBTOTAL"
        );

        System.out.println(
                "----------------------------------------------------------------"
        );

        for (Map.Entry<Integer, Integer> entry
                : cart.entrySet()) {

            Product product =
                    ProductDao.getProductById(entry.getKey());

            if (product == null) {
                System.out.println(
                        "Product ID " + entry.getKey()
                                + " is no longer available."
                );
                return null;
            }

            int quantity = entry.getValue();

            BigDecimal price =
                    BigDecimal.valueOf(product.unitPrice())
                            .setScale(2, RoundingMode.HALF_UP);

            BigDecimal subtotal =
                    price.multiply(BigDecimal.valueOf(quantity))
                            .setScale(2, RoundingMode.HALF_UP);

            total = total.add(subtotal);

            System.out.printf(
                    "%-8d %-23s %-8d PHP %-8.2f PHP %-8.2f%n",
                    product.productId(),
                    shorten(product.productName(), 22),
                    quantity,
                    price,
                    subtotal
            );
        }

        total = total.setScale(2, RoundingMode.HALF_UP);

        System.out.println(
                "----------------------------------------------------------------"
        );

        System.out.printf(
                "TOTAL AMOUNT: PHP %,.2f%n",
                total
        );

        System.out.println(
                "=================================="
        );

        return total;
    }

    // ==========================================
    // PAYMENT INPUT
    // ==========================================
    private static BigDecimal readPaymentAmount(
            BigDecimal total) {

        while (true) {

            System.out.printf(
                    "Total Amount: PHP %,.2f%n",
                    total
            );

            System.out.print("Cash received: PHP ");

            String input = ConsoleInput.sc.nextLine().trim();

            try {

                BigDecimal amount = new BigDecimal(input);

                if (amount.signum() < 0) {
                    System.out.println(
                            "Payment cannot be negative."
                    );
                    continue;
                }

                amount = amount.setScale(
                        2,
                        RoundingMode.HALF_UP
                );

                if (amount.compareTo(total) < 0) {
                    System.out.println(
                            "Insufficient payment. Please enter "
                                    + "at least PHP " + total
                    );
                    continue;
                }

                return amount;

            } catch (NumberFormatException e) {

                System.out.println(
                        "Invalid amount. Please enter a valid number."
                );
            }
        }
    }

    // ==========================================
    // PRINT RECEIPT
    // ==========================================
    private static void printReceipt(
            User cashier,
            SaleReceipt receipt) {

        System.out.println("\n");
        System.out.println("==========================================");
        System.out.println("              POS SYSTEM");
        System.out.println("               RECEIPT");
        System.out.println("==========================================");

        System.out.println(
                "Transaction ID: " + receipt.transactionId()
        );

        System.out.println(
                "Date: " + receipt.transactionDate()
        );

        System.out.println(
                "Cashier: " + cashier.fullName()
        );

        System.out.println(
                "Cashier ID: " + cashier.userId()
        );

        System.out.println("------------------------------------------");

        System.out.printf(
                "%-20s %-5s %-12s%n",
                "PRODUCT",
                "QTY",
                "SUBTOTAL"
        );

        System.out.println("------------------------------------------");

        for (SaleReceipt.SaleLine item : receipt.items()) {

            System.out.printf(
                    "%-20s %-5d PHP %-8.2f%n",
                    shorten(item.productName(), 19),
                    item.quantity(),
                    item.subtotal()
            );
        }

        System.out.println("------------------------------------------");

        System.out.printf(
                "TOTAL:       PHP %,.2f%n",
                receipt.totalAmount()
        );

        System.out.printf(
                "CASH:        PHP %,.2f%n",
                receipt.amountPaid()
        );

        System.out.printf(
                "CHANGE:      PHP %,.2f%n",
                receipt.change()
        );

        System.out.println("------------------------------------------");
        System.out.println("Payment Method: CASH");
        System.out.println("Status: COMPLETED");
        System.out.println("       THANK YOU!");
        System.out.println("==========================================");
    }

    // ==========================================
    // SEARCH PRODUCTS
    // ==========================================
    private static void searchProducts(User cashier) {

        System.out.println("\n========== PRODUCT SEARCH ==========");

        System.out.print(
                "Enter product name or category: "
        );

        String keyword =
                ConsoleInput.sc.nextLine().trim();

        if (keyword.isEmpty()) {
            System.out.println("Search cannot be empty.");
            return;
        }

        List<Product> products =
                ProductDao.searchProducts(
                        keyword,
                        cashier.userId()
                );

        System.out.println("\n========== SEARCH RESULTS ==========");

        if (products.isEmpty()) {
            System.out.println("No matching products found.");
            return;
        }

        System.out.printf(
                "%-8s %-25s %-16s %-15s %-10s%n",
                "ID",
                "PRODUCT",
                "CATEGORY",
                "PRICE",
                "STOCK"
        );

        for (Product product : products) {

            System.out.printf(
                    "%-8d %-25s %-16s PHP %-10.2f %-10d%n",
                    product.productId(),
                    shorten(product.productName(), 24),
                    shorten(product.category(), 15),
                    product.unitPrice(),
                    product.stockQuantity()
            );
        }
    }

    // ==========================================
    // VIEW TRANSACTION HISTORY
    // ==========================================
    private static void viewTransactions(User cashier) {

        SalesDao.viewTransactions(cashier.userId());

        System.out.println(
                "\nEnter a Transaction ID to view its details."
        );

        System.out.println("Enter 0 to return.");

        int transactionId = readInt(
                "Transaction ID: ",
                0,
                Integer.MAX_VALUE,
                true
        );

        if (transactionId == 0) {
            return;
        }

        SalesDao.viewTransactionDetails(
                cashier.userId(),
                transactionId
        );
    }

    // ==========================================
    // INPUT VALIDATION
    // ==========================================
    private static int readInt(
            String prompt,
            int min,
            int max,
            boolean allowZero) {

        while (true) {

            System.out.print(prompt);

            String input =
                    ConsoleInput.sc.nextLine().trim();

            try {

                int value = Integer.parseInt(input);

                if (allowZero && value == 0) {
                    return 0;
                }

                if (value >= min && value <= max) {
                    return value;
                }

                System.out.println("Number is outside the allowed range.");

            } catch (NumberFormatException e) {

                System.out.println(
                        "Invalid input. Enter a whole number."
                );
            }
        }
    }

    // ==========================================
    // CONFIRMATION
    // ==========================================
    private static boolean confirm(String prompt) {

        while (true) {

            System.out.print(prompt + " (Y/N): ");

            String answer =
                    ConsoleInput.sc.nextLine().trim();

            if (answer.equalsIgnoreCase("Y")) {
                return true;
            }

            if (answer.equalsIgnoreCase("N")) {
                return false;
            }

            System.out.println("Please enter Y or N.");
        }
    }

    // ==========================================
    // TEXT HELPER
    // ==========================================
    private static String shorten(
            String value,
            int maxLength) {

        if (value == null) {
            return "N/A";
        }

        if (value.length() > maxLength) {
            return value.substring(0, maxLength - 1) + ".";
        }

        return value;
    }
}