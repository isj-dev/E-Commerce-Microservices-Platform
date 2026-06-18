package com.isj.notification.service;

import com.isj.common.event.PaymentEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    @KafkaListener(topics = "payment-events", groupId = "notification-group")
    public void handlePaymentEvent(PaymentEvent event) {
        log.info("Received payment event: orderId={}, status={}", event.getOrderId(), event.getStatus());

        // NullPointerException 방지용 리터럴 사용
        if ("COMPLETED".equals(event.getStatus())) {
            sendOrderConfirmation(event);
        } else if ("FAILED".equals(event.getStatus())) {
            sendPaymentFailureNotification(event);
        }
    }

    private void sendOrderConfirmation(PaymentEvent event) {
        // 메시지 발행 로직
        log.info("[NOTIFICATION] Order #{} payment confirmed. Amount: {}. User: {}",
                event.getOrderId(), event.getAmount(), event.getUserId());
    }

    private void sendPaymentFailureNotification(PaymentEvent event) {
        log.warn("[NOTIFICATION] Order #{} payment failed. Reason: {}. User: {}",
                event.getOrderId(), event.getFailureReason(), event.getUserId());
    }
}
