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
        decreaseStockWithCompensation(request.getItems());

        BigDecimal totalAmount = request.getItems().stream()
                .map(i -> i.getUnitPrice().multiply(BigDecimal.valueOf(i.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Order order = Order.builder()
                .userId(userId)
                .totalAmount(totalAmount)
                .deliveryAddress(request.getDeliveryAddress())
                .build();

        for (CreateOrderRequest.OrderItemRequest itemReq : request.getItems()) {
            order.addItem(OrderItem.builder()
                    .productId(itemReq.getProductId())
                    .productName(itemReq.getProductName())
                    .quantity(itemReq.getQuantity())
                    .unitPrice(itemReq.getUnitPrice())
                    .build());
        }

        Order saved = orderRepository.save(order);

        kafkaTemplate.send(ORDER_TOPIC, String.valueOf(saved.getId()), buildOrderEvent(saved));
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

    @Transactional
    public OrderResponse cancelOrder(Long orderId, Long userId) {
        Order order = orderRepository.findOrderByIdAndUserId(orderId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ORDER_NOT_FOUND));

        if (order.getStatus() != Order.OrderStatus.PENDING) {
            throw new BusinessException(ErrorCode.ORDER_CANNOT_BE_CANCELLED);
        }

        order.updateStatus(Order.OrderStatus.CANCELLED);
        restoreStock(order.getItems());
        return new OrderResponse(order);
    }

    // Called by OrderSagaListener when payment succeeds
    @Transactional
    public void confirmOrder(Long orderId) {
        orderRepository.findById(orderId).ifPresent(order -> {
            order.updateStatus(Order.OrderStatus.PAID);
            log.info("Order confirmed (payment completed): orderId={}", orderId);
        });
    }

    // Called by OrderSagaListener when payment fails — cancel order and restore stock
    @Transactional
    public List<StockRestoreInfo> cancelByPaymentFailure(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ORDER_NOT_FOUND));

        order.updateStatus(Order.OrderStatus.CANCELLED);
        log.info("Order cancelled by payment failure: orderId={}", orderId);

        return order.getItems().stream()
                .map(i -> new StockRestoreInfo(i.getProductId(), i.getQuantity()))
                .toList();
    }

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
    private void decreaseStockWithCompensation(List<CreateOrderRequest.OrderItemRequest> items) {
        List<CreateOrderRequest.OrderItemRequest> succeeded = new ArrayList<>();
        try {
            for (CreateOrderRequest.OrderItemRequest item : items) {
                productClient.decreaseStock(item.getProductId(), new StockRequest(item.getQuantity()));
                succeeded.add(item);
            }
        } catch (BusinessException e) {
            succeeded.forEach(item ->
                    productClient.increaseStock(item.getProductId(), new StockRequest(item.getQuantity())));
            throw e;
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
