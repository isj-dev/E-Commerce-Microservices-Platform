package com.isj.order.repository;

import com.isj.order.domain.Order;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;

public interface OrderRepositoryCustom {

    Optional<Order> findOrderByIdAndUserId(Long orderId, Long userId);

    Page<Order> findOrdersByUserId(Long userId, Pageable pageable);
}
