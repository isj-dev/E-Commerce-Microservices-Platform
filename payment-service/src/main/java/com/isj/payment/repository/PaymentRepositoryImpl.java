package com.isj.payment.repository;

import com.isj.payment.domain.Payment;
import com.isj.payment.domain.QPayment;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;

import java.util.Optional;

@RequiredArgsConstructor
public class PaymentRepositoryImpl implements PaymentRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    private final QPayment payment = QPayment.payment;

    @Override
    public Optional<Payment> findPaymentByOrderId(Long orderId) {
        return Optional.ofNullable(
                queryFactory.selectFrom(payment)
                        .where(payment.orderId.eq(orderId))
                        .fetchOne()
        );
    }
}
