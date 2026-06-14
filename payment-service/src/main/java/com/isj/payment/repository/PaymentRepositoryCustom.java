package com.isj.payment.repository;

import com.isj.payment.domain.Payment;

import java.util.Optional;

public interface PaymentRepositoryCustom {

    Optional<Payment> findPaymentByOrderId(Long orderId);
}
