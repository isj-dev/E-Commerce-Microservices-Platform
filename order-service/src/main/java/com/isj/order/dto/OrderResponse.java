package com.isj.order.dto;

import com.isj.order.domain.Order;
import com.isj.order.domain.OrderItem;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Getter
public class OrderResponse {

    private final Long id;
    private final Long userId;
    private final List<ItemResponse> items;
    private final BigDecimal totalAmount;
    private final String status;
    private final String deliveryAddress;
    private final LocalDateTime createdAt;

    public OrderResponse(Order order) {
        this.id = order.getId();
        this.userId = order.getUserId();
        this.items = order.getItems().stream().map(ItemResponse::new).toList();
        this.totalAmount = order.getTotalAmount();
        this.status = order.getStatus().name();
        this.deliveryAddress = order.getDeliveryAddress();
        this.createdAt = order.getCreatedAt();
    }

    @Getter
    public static class ItemResponse {
        private final Long productId;
        private final String productName;
        private final Integer quantity;
        private final BigDecimal unitPrice;
        private final BigDecimal subtotal;

        public ItemResponse(OrderItem item) {
            this.productId = item.getProductId();
            this.productName = item.getProductName();
            this.quantity = item.getQuantity();
            this.unitPrice = item.getUnitPrice();
            this.subtotal = item.getSubtotal();
        }
    }
}
