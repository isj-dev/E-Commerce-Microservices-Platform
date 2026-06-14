package com.isj.cart.service;

import com.isj.cart.dto.CartItemRequest;
import com.isj.cart.dto.CartResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class CartService {

    private final RedisTemplate<String, Object> redisTemplate;

    @Value("${cart.ttl-hours:24}")
    private long ttlHours;

    private String cartKey(Long userId) {
        return "cart:" + userId;
    }

    public CartResponse addItem(Long userId, CartItemRequest request) {
        String key = cartKey(userId);
        String field = String.valueOf(request.getProductId());

        Object existing = redisTemplate.opsForHash().get(key, field);
        int currentQty = existing != null ? Integer.parseInt(existing.toString()) : 0;
        int newQty = currentQty + request.getQuantity();

        redisTemplate.opsForHash().put(key, field, String.valueOf(newQty));
        redisTemplate.expire(key, ttlHours, TimeUnit.HOURS);

        return getCart(userId);
    }

    public CartResponse getCart(Long userId) {
        String key = cartKey(userId);
        Map<Object, Object> entries = redisTemplate.opsForHash().entries(key);

        List<CartResponse.CartItemResponse> items = entries.entrySet().stream()
                .map(e -> CartResponse.CartItemResponse.builder()
                        .productId(Long.parseLong(e.getKey().toString()))
                        .productName("Product-" + e.getKey())
                        .quantity(Integer.parseInt(e.getValue().toString()))
                        .unitPrice(BigDecimal.ZERO)
                        .subtotal(BigDecimal.ZERO)
                        .build())
                .collect(Collectors.toList());

        return CartResponse.builder()
                .userId(userId)
                .items(items)
                .totalAmount(BigDecimal.ZERO)
                .totalItems(items.stream().mapToInt(CartResponse.CartItemResponse::getQuantity).sum())
                .build();
    }

    public void removeItem(Long userId, Long productId) {
        redisTemplate.opsForHash().delete(cartKey(userId), String.valueOf(productId));
    }

    public void clearCart(Long userId) {
        redisTemplate.delete(cartKey(userId));
    }

    public CartResponse updateItem(Long userId, Long productId, int quantity) {
        String key = cartKey(userId);
        if (quantity <= 0) {
            redisTemplate.opsForHash().delete(key, String.valueOf(productId));
        } else {
            redisTemplate.opsForHash().put(key, String.valueOf(productId), String.valueOf(quantity));
            redisTemplate.expire(key, ttlHours, TimeUnit.HOURS);
        }
        return getCart(userId);
    }
}
