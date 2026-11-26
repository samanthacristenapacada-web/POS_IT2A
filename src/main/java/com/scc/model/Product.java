package com.scc.model;

public record Product(
        int productId,
        String productName,
        String category,
        double unitPrice,
        int stockQuantity,
        String description
) {
}