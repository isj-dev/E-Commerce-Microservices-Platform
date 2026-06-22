package com.isj.cart.client;

import com.isj.common.dto.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class ProductClientFallback implements ProductClient {

    @Override // product-service 장애 시에도 장바구니 조회 자체는 깨지지 않도록 빈 데이터로 응답
    public ApiResponse<ProductInfo> getProduct(Long productId) {
        log.warn("Circuit breaker open: failed to fetch product info for productId={}", productId);
        return ApiResponse.ok(null);
    }
}
