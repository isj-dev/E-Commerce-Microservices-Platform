package com.isj.payment.service;

import com.isj.common.exception.BusinessException;
import com.isj.common.exception.ErrorCode;
import com.isj.payment.domain.Payment;
import com.isj.common.event.OrderEvent;
import com.isj.common.event.PaymentEvent;
import com.isj.payment.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Random;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentService {

    private static final String PAYMENT_TOPIC = "payment-events";
    private static final double SUCCESS_RATE = 0.9;

    private final PaymentRepository paymentRepository;
    private final KafkaTemplate<String, PaymentEvent> kafkaTemplate;
    private final Random random = new Random();

    @KafkaListener(topics = "order-events", groupId = "payment-group")
    @Transactional
    public void processOrderEvent(OrderEvent event) {
        log.info("Received order event: orderId={}, status={}", event.getOrderId(), event.getStatus());

        if (!"PENDING".equals(event.getStatus())) {
            return;
        }

        Payment payment = Payment.builder()
                .orderId(event.getOrderId())
                .userId(event.getUserId())
                .amount(event.getTotalAmount())
                .build();


        boolean success = random.nextDouble() < SUCCESS_RATE; // PG사 연동 대신 90% 확률로 성공하는 Mock 구현
        if (success) {
            payment.complete();
            log.info("Payment completed for orderId={}", event.getOrderId());
        } else {
            payment.fail("Mock payment failure");
            log.warn("Payment failed for orderId={}", event.getOrderId());
        }

        Payment saved = paymentRepository.save(payment);

        PaymentEvent paymentEvent = PaymentEvent.builder()
                .paymentId(saved.getId())
                .orderId(saved.getOrderId())
                .userId(saved.getUserId())
                .amount(saved.getAmount())
                .status(saved.getStatus().name())
                .failureReason(saved.getFailureReason())
                .build();

        kafkaTemplate.send(PAYMENT_TOPIC, String.valueOf(saved.getOrderId()), paymentEvent);
        log.info("Payment event published: orderId={}, status={}", saved.getOrderId(), saved.getStatus());
    }

    public Payment getPaymentByOrderId(Long orderId) {
        return paymentRepository.findPaymentByOrderId(orderId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PAYMENT_NOT_FOUND));
    }
}
