package com.isj.order.service;

import com.isj.common.exception.BusinessException;
import com.isj.common.exception.ErrorCode;
import com.isj.order.client.ProductClient;
import com.isj.order.client.StockRequest;
import com.isj.order.domain.Order;
import com.isj.order.domain.OrderItem;
import com.isj.order.dto.CreateOrderRequest;
import com.isj.order.dto.OrderResponse;
import com.isj.common.event.OrderEvent;
import com.isj.order.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OrderService {

    private static final String ORDER_TOPIC = "order-events";

    private final OrderRepository orderRepository;
    private final KafkaTemplate<String, OrderEvent> kafkaTemplate;
    private final ProductClient productClient;

    @Transactional
    public OrderResponse createOrder(Long userId, CreateOrderRequest request) {
        decreaseStockWithCompensation(request.getItems()); // 재고 감소

        BigDecimal totalAmount = request.getItems().stream() // 총액 계산
                .map(i -> i.getUnitPrice().multiply(BigDecimal.valueOf(i.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Order order = Order.builder() // 주문 엔티티 생성
                .userId(userId)
                .totalAmount(totalAmount)
                .deliveryAddress(request.getDeliveryAddress())
                .build();

        for (CreateOrderRequest.OrderItemRequest itemReq : request.getItems()) { // 주문 아이템 추가
            order.addItem(OrderItem.builder()
                    .productId(itemReq.getProductId())
                    .productName(itemReq.getProductName())
                    .quantity(itemReq.getQuantity())
                    .unitPrice(itemReq.getUnitPrice())
                    .build());
        }

        Order saved = orderRepository.save(order); // DB 저장

        kafkaTemplate.send(ORDER_TOPIC, String.valueOf(saved.getId()), buildOrderEvent(saved)); // Kafka 발행 (payment-service 결제 요청)
        log.info("Order created and event published: orderId={}", saved.getId());

        return new OrderResponse(saved);
    }

    public OrderResponse getOrder(Long orderId, Long userId) {
        return new OrderResponse(orderRepository.findOrderByIdAndUserId(orderId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ORDER_NOT_FOUND)));
    }

    public Page<OrderResponse> getUserOrders(Long userId, Pageable pageable) {
        return orderRepository.findOrdersByUserId(userId, pageable).map(OrderResponse::new);
    }

    @Transactional // 사용자 주문 취소
    public OrderResponse cancelOrder(Long orderId, Long userId) {
        Order order = orderRepository.findOrderByIdAndUserId(orderId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ORDER_NOT_FOUND));

        // 현재는 결제 완료 주문은 취소 불가
        if (order.getStatus() != Order.OrderStatus.PENDING) {
            throw new BusinessException(ErrorCode.ORDER_CANNOT_BE_CANCELLED);
        }

        order.updateStatus(Order.OrderStatus.CANCELLED);
        restoreStock(order.getItems()); // 재고 복구
        return new OrderResponse(order);
    }

    // Called by OrderSagaListener when payment succeeds
    @Transactional
    public void confirmOrder(Long orderId) { // 결제 성공 후 주문 PAID 처리
        orderRepository.findById(orderId).ifPresent(order -> {
            order.updateStatus(Order.OrderStatus.PAID);
            log.info("Order confirmed (payment completed): orderId={}", orderId);
        });
    }

    // Called by OrderSagaListener when payment fails — cancel order and restore stock
    // cancelByPaymentFailure()가 직접 재고를 복구하지 않고 복구 정보만 반환하는 이유는 @Transactional 범위 때문입니다. DB 작업(CANCELLED 상태 저장)과 외부 HTTP 호출(재고 복구)을
    //  같은 트랜잭션 안에 섞지 않기 위해 분리했습니다.
    @Transactional
    public List<StockRestoreInfo> cancelByPaymentFailure(Long orderId) { // 결제 실패 후 주문 취소 및 복구할 재고 목록 반환
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ORDER_NOT_FOUND));

        order.updateStatus(Order.OrderStatus.CANCELLED);
        log.info("Order cancelled by payment failure: orderId={}", orderId);

        return order.getItems().stream()
                .map(i -> new StockRestoreInfo(i.getProductId(), i.getQuantity()))
                .toList(); // 재고 복구 정보 반환(호출자가 복구 실행)
    }

    // 재고 복구 실패 시 예외를 던지지 않고 로그만 남깁니다. 이미 주문은 CANCELLED 처리가 완료된 상태이기 때문에 재고 복구 실패가 주문 취소를 되돌릴 수는 없습니다. 실무에서는 이런
    //  경우를 위해 Dead Letter Queue나 별도 보상 스케줄러를 두기도 합니다
    public void restoreStockByInfoList(List<StockRestoreInfo> items) {
        items.forEach(info -> {
            try {
                productClient.increaseStock(info.productId(), new StockRequest(info.quantity()));
            } catch (Exception e) {
                log.error("Stock restoration failed: productId={}, quantity={}", info.productId(), info.quantity(), e);
            }
        });
    }

    public record StockRestoreInfo(Long productId, int quantity) {}

    // Decreases stock for each item. If any call fails, compensates already-decreased items.
    // 재고 감소 및 보상 트랜잭션
    private void decreaseStockWithCompensation(List<CreateOrderRequest.OrderItemRequest> items) {
        List<CreateOrderRequest.OrderItemRequest> succeeded = new ArrayList<>();
        try {
            for (CreateOrderRequest.OrderItemRequest item : items) {
                productClient.decreaseStock(item.getProductId(), new StockRequest(item.getQuantity())); // 상품별로 순차 감소
                succeeded.add(item); // 성공한 것만 기록
            }
        } catch (BusinessException e) {
            // 중간에 실패하면 성공한 것들만 다시 복구
            succeeded.forEach(item ->
                    productClient.increaseStock(item.getProductId(), new StockRequest(item.getQuantity())));
            throw e; // 예외를 다시 던져서 createOrder() 도 실패 처리
        }
    }

    private void restoreStock(List<OrderItem> items) {
        items.forEach(item -> {
            try {
                productClient.increaseStock(item.getProductId(), new StockRequest(item.getQuantity()));
            } catch (Exception e) {
                log.error("Stock restoration failed: productId={}", item.getProductId(), e);
            }
        });
    }

    private OrderEvent buildOrderEvent(Order order) {
        List<OrderEvent.OrderItemInfo> items = order.getItems().stream()
                .map(i -> OrderEvent.OrderItemInfo.builder()
                        .productId(i.getProductId())
                        .productName(i.getProductName())
                        .quantity(i.getQuantity())
                        .unitPrice(i.getUnitPrice())
                        .build())
                .toList();

        return OrderEvent.builder()
                .orderId(order.getId())
                .userId(order.getUserId())
                .totalAmount(order.getTotalAmount())
                .status(order.getStatus().name())
                .items(items)
                .build();
    }
}
