package com.isj.order.controller;

import com.isj.common.dto.ApiResponse;
import com.isj.order.dto.CreateOrderRequest;
import com.isj.order.dto.OrderResponse;
import com.isj.order.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<OrderResponse> createOrder(
            @RequestHeader("X-User-Id") Long userId,
            @Valid @RequestBody CreateOrderRequest request) {
        return ApiResponse.ok("Order created", orderService.createOrder(userId, request));
    }

    @GetMapping("/{orderId}")
    public ApiResponse<OrderResponse> getOrder(
            @PathVariable Long orderId,
            @RequestHeader("X-User-Id") Long userId) {
        return ApiResponse.ok(orderService.getOrder(orderId, userId));
    }

    @GetMapping
    public ApiResponse<Page<OrderResponse>> getUserOrders(
            @RequestHeader("X-User-Id") Long userId,
            @PageableDefault(size = 10) Pageable pageable) {
        return ApiResponse.ok(orderService.getUserOrders(userId, pageable));
    }

    @PostMapping("/{orderId}/cancel")
    public ApiResponse<OrderResponse> cancelOrder(
            @PathVariable Long orderId,
            @RequestHeader("X-User-Id") Long userId) {
        return ApiResponse.ok(orderService.cancelOrder(orderId, userId));
    }
}
