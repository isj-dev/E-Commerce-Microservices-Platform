package com.isj.cart.service;

import com.isj.cart.client.ProductClient;
import com.isj.cart.client.ProductInfo;
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
    private final ProductClient productClient;

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
                .map(e -> {
                    Long productId = Long.parseLong(e.getKey().toString());
                    int quantity = Integer.parseInt(e.getValue().toString());
                    ProductInfo product = fetchProduct(productId);

                    String productName = product != null ? product.getName() : "Unknown Product";
                    BigDecimal unitPrice = product != null ? product.getPrice() : BigDecimal.ZERO;
                    BigDecimal subtotal = unitPrice.multiply(BigDecimal.valueOf(quantity));

                    return CartResponse.CartItemResponse.builder()
                            .productId(productId)
                            .productName(productName)
                            .quantity(quantity)
                            .unitPrice(unitPrice)
                            .subtotal(subtotal)
                            .build();
                })
                .collect(Collectors.toList());

        BigDecimal totalAmount = items.stream()
                .map(CartResponse.CartItemResponse::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return CartResponse.builder()
                .userId(userId)
                .items(items)
                .totalAmount(totalAmount)
                .totalItems(items.stream().mapToInt(CartResponse.CartItemResponse::getQuantity).sum())
                .build();
    }

    // product-service 장애 시 Circuit Breaker가 ProductClientFallback으로 빈 데이터를 반환
    private ProductInfo fetchProduct(Long productId) {
        try {
            return productClient.getProduct(productId).getData();
        } catch (Exception e) {
            log.error("Failed to fetch product info: productId={}", productId, e);
            return null;
        }
    }

    public void removeItem(Long userId, Long productId) {
        redisTemplate.opsForHash().delete(cartKey(userId), String.valueOf(productId));
    }

    public void clearCart(Long userId) {
        redisTemplate.delete(cartKey(userId));
    }

    // addItem()과 달리 누적이 아닌 덮어쓰기(수량을 3개에서 5로 바꾸면 그냥 5가 저장)
    public CartResponse updateItem(Long userId, Long productId, int quantity) {
        String key = cartKey(userId);
        if (quantity <= 0) {
            redisTemplate.opsForHash().delete(key, String.valueOf(productId)); // 수량 0 이하면 삭제
        } else {
            redisTemplate.opsForHash().put(key, String.valueOf(productId), String.valueOf(quantity));
            redisTemplate.expire(key, ttlHours, TimeUnit.HOURS);
        }
        return getCart(userId);
    }
}
