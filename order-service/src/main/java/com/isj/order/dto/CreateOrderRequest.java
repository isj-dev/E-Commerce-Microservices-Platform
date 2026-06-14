package com.isj.order.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
public class CreateOrderRequest {

    @NotEmpty
    @Valid
    private List<OrderItemRequest> items;

    @NotBlank
    private String deliveryAddress;

    @Getter
    @NoArgsConstructor
    public static class OrderItemRequest {

        @NotNull
        private Long productId;

        @NotBlank
        private String productName;

        @Min(1)
        private Integer quantity;

        @NotNull
        private java.math.BigDecimal unitPrice;
    }
}
