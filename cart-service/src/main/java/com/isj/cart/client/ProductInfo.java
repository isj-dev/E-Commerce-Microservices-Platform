package com.isj.cart.client;

import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Getter
@NoArgsConstructor
public class ProductInfo {
    private Long id;
    private String name;
    private BigDecimal price;
}
