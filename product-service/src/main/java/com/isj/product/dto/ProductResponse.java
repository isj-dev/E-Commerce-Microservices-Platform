package com.isj.product.dto;

import com.isj.product.domain.Product;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
public class ProductResponse {

    private final Long id;
    private final String name;
    private final String description;
    private final BigDecimal price;
    private final Integer stock;
    private final String category;
    private final String imageUrl;
    private final LocalDateTime createdAt;

    public ProductResponse(Product product) {
        this.id = product.getId();
        this.name = product.getName();
        this.description = product.getDescription();
        this.price = product.getPrice();
        this.stock = product.getStock();
        this.category = product.getCategory();
        this.imageUrl = product.getImageUrl();
        this.createdAt = product.getCreatedAt();
    }
}
