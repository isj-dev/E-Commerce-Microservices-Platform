package com.isj.payment.controller;

import com.isj.common.dto.ApiResponse;
import com.isj.payment.domain.Payment;
import com.isj.payment.service.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    @GetMapping("/orders/{orderId}")
    public ApiResponse<Payment> getPaymentByOrder(@PathVariable Long orderId) {
        return ApiResponse.ok(paymentService.getPaymentByOrderId(orderId));
    }
}
