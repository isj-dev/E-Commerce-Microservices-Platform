package com.isj.order.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "product-service", fallback = ProductClientFallback.class)
public interface ProductClient {

    @PutMapping("/products/{productId}/stock/decrease")
    void decreaseStock(@PathVariable("productId") Long productId, @RequestBody StockRequest request);

    @PutMapping("/products/{productId}/stock/increase")
    void increaseStock(@PathVariable("productId") Long productId, @RequestBody StockRequest request);
}
