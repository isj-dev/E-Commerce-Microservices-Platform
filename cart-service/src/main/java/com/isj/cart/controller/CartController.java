package com.isj.cart.controller;

import com.isj.common.dto.ApiResponse;
import com.isj.cart.dto.CartItemRequest;
import com.isj.cart.dto.CartResponse;
import com.isj.cart.service.CartService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/cart")
@RequiredArgsConstructor
public class CartController {

    private final CartService cartService;

    @GetMapping
    public ApiResponse<CartResponse> getCart(@RequestHeader("X-User-Id") Long userId) {
        return ApiResponse.ok(cartService.getCart(userId));
    }

    @PostMapping("/items")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<CartResponse> addItem(
            @RequestHeader("X-User-Id") Long userId,
            @Valid @RequestBody CartItemRequest request) {
        return ApiResponse.ok(cartService.addItem(userId, request));
    }

    @PutMapping("/items/{productId}")
    public ApiResponse<CartResponse> updateItem(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable Long productId,
            @RequestParam int quantity) {
        return ApiResponse.ok(cartService.updateItem(userId, productId, quantity));
    }

    @DeleteMapping("/items/{productId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeItem(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable Long productId) {
        cartService.removeItem(userId, productId);
    }

    @DeleteMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void clearCart(@RequestHeader("X-User-Id") Long userId) {
        cartService.clearCart(userId);
    }
}
