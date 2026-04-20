package com.airline.reservation.controller;

import com.airline.reservation.dto.payment.PaymentRequest;
import com.airline.reservation.dto.payment.PaymentResponse;
import com.airline.reservation.service.PaymentService;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping
    public PaymentResponse processPayment(@Valid @RequestBody PaymentRequest request, Authentication authentication) {
        return paymentService.processPayment(authentication.getName(), request);
    }
}
