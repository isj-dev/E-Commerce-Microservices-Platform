package com.isj.order.client;

import com.isj.common.exception.BusinessException;
import com.isj.common.exception.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class ProductClientFallback implements ProductClient {

    @Override // 주문 생성 시 재고를 못 줄이면 주문 자체가 실패해야 하므로 에러 리턴
    public void decreaseStock(Long productId, StockRequest request) {
        log.error("Circuit breaker open: stock decrease failed for productId={}", productId);
        throw new BusinessException(ErrorCode.PRODUCT_SERVICE_UNAVAILABLE);
    }

    @Override // 결제 실패 후 재고를 복구하는 보상 트랜잭션인데, product-service가 죽어있으면 복구를 못 하더라도 주문은 이미 실패 처리된 상태이므로 로그만 남기고 진행
    public void increaseStock(Long productId, StockRequest request) {
        log.warn("Circuit breaker open: stock restoration skipped for productId={}", productId);
    }
}
