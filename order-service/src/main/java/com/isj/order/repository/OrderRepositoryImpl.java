package com.isj.order.repository;

import com.isj.order.domain.Order;
import com.isj.order.domain.QOrder;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

@RequiredArgsConstructor
public class OrderRepositoryImpl implements OrderRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    private final QOrder order = QOrder.order;

    @Override
    public Optional<Order> findOrderByIdAndUserId(Long orderId, Long userId) {
        return Optional.ofNullable(
                queryFactory.selectFrom(order)
                        .where(
                                order.id.eq(orderId),
                                order.userId.eq(userId)
                        )
                        .fetchOne()
        );
    }

    @Override
    public Page<Order> findOrdersByUserId(Long userId, Pageable pageable) {
        List<Order> content = queryFactory
                .selectFrom(order)
                .where(order.userId.eq(userId))
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .orderBy(order.createdAt.desc())
                .fetch();

        long total = Optional.ofNullable(
                queryFactory.select(order.count())
                        .from(order)
                        .where(order.userId.eq(userId))
                        .fetchOne()
        ).orElse(0L);

        return new PageImpl<>(content, pageable, total);
    }
}
