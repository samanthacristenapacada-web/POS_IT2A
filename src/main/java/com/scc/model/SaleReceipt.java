package com.scc.model;

import java.math.BigDecimal;
import java.util.List;

public record SaleReceipt(
        int transactionId,
        String transactionDate,
        BigDecimal totalAmount,
        BigDecimal amountPaid,
        BigDecimal change,
        List<SaleLine> items
) {

    public record SaleLine(
            int productId,
            String productName,
            int quantity,
            BigDecimal unitPrice,
            BigDecimal subtotal
    ) {
    }
}