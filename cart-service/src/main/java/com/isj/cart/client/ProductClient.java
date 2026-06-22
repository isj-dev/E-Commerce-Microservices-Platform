package com.isj.cart.client;

import com.isj.common.dto.ApiResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "product-service", fallback = ProductClientFallback.class)
public interface ProductClient {

    @GetMapping("/products/{productId}")
    ApiResponse<ProductInfo> getProduct(@PathVariable("productId") Long productId);
}
