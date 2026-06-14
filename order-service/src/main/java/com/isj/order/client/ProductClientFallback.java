package com.isj.order.client;

import com.isj.common.exception.BusinessException;
import com.isj.common.exception.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class ProductClientFallback implements ProductClient {

    @Override
    public void decreaseStock(Long productId, StockRequest request) {
        log.error("Circuit breaker open: stock decrease failed for productId={}", productId);
        throw new BusinessException(ErrorCode.PRODUCT_SERVICE_UNAVAILABLE);
    }

    @Override
    public void increaseStock(Long productId, StockRequest request) {
        log.warn("Circuit breaker open: stock restoration skipped for productId={}", productId);
    }
}
