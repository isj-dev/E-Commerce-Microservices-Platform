package com.isj.common.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum ErrorCode {

    // Common
    INVALID_INPUT(HttpStatus.BAD_REQUEST, "C001", "Invalid input"),
    RESOURCE_NOT_FOUND(HttpStatus.NOT_FOUND, "C002", "Resource not found"),
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "C003", "Internal server error"),

    // User
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "U001", "User not found"),
    EMAIL_ALREADY_EXISTS(HttpStatus.CONFLICT, "U002", "Email already exists"),
    INVALID_PASSWORD(HttpStatus.UNAUTHORIZED, "U003", "Invalid password"),
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "U004", "Unauthorized"),
    INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "U005", "Invalid token"),

    // Product
    PRODUCT_NOT_FOUND(HttpStatus.NOT_FOUND, "P001", "Product not found"),
    INSUFFICIENT_STOCK(HttpStatus.BAD_REQUEST, "P002", "Insufficient stock"),
    PRODUCT_SERVICE_UNAVAILABLE(HttpStatus.SERVICE_UNAVAILABLE, "P003", "Product service unavailable"),

    // Order
    ORDER_NOT_FOUND(HttpStatus.NOT_FOUND, "O001", "Order not found"),
    ORDER_CANNOT_BE_CANCELLED(HttpStatus.BAD_REQUEST, "O002", "Order cannot be cancelled"),

    // Payment
    PAYMENT_FAILED(HttpStatus.BAD_REQUEST, "PAY001", "Payment failed"),
    PAYMENT_NOT_FOUND(HttpStatus.NOT_FOUND, "PAY002", "Payment not found");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;

    ErrorCode(HttpStatus httpStatus, String code, String message) {
        this.httpStatus = httpStatus;
        this.code = code;
        this.message = message;
    }
}
