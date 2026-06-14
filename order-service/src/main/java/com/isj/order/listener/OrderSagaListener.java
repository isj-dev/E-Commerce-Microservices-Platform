package com.isj.order.listener;

import com.isj.common.event.PaymentEvent;
import com.isj.order.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderSagaListener {

    private final OrderService orderService;

    @KafkaListener(
            topics = "payment-events",
            groupId = "order-saga-group",
            containerFactory = "paymentEventKafkaListenerContainerFactory"
    )
    public void handlePaymentEvent(PaymentEvent event) {
        log.info("Payment event received: orderId={}, status={}", event.getOrderId(), event.getStatus());

        switch (event.getStatus()) {
            case "COMPLETED" -> orderService.confirmOrder(event.getOrderId());
            case "FAILED" -> {
                List<OrderService.StockRestoreInfo> items =
                        orderService.cancelByPaymentFailure(event.getOrderId());
                orderService.restoreStockByInfoList(items);
            }
            default -> log.warn("Unhandled payment status: {}", event.getStatus());
        }
    }
}
