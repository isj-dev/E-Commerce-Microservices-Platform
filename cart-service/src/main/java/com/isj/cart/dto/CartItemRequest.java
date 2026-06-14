package com.isj.cart.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class CartItemRequest {

    @NotNull
    private Long productId;

    @Min(1)
    private Integer quantity;
}
