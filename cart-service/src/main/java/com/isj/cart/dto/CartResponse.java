package com.isj.cart.dto;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Builder
public class CartResponse {

    private final Long userId;
    private final List<CartItemResponse> items;
    private final BigDecimal totalAmount;
    private final int totalItems;

    @Getter
    @Builder
    public static class CartItemResponse {
        private final Long productId;
        private final String productName;
        private final Integer quantity;
        private final BigDecimal unitPrice;
        private final BigDecimal subtotal;
    }
}
