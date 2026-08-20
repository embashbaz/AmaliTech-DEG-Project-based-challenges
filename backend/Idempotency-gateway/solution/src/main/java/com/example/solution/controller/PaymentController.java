package com.example.solution.controller;

import com.example.solution.model.PaymentRequest;
import com.example.solution.service.IdempotencyService;
import com.example.solution.service.RateLimiterService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/process-payment")
@RequiredArgsConstructor
public class PaymentController {

    private final IdempotencyService idempotencyService;
    private final RateLimiterService rateLimiterService;

    @PostMapping
    public ResponseEntity<String> processPayment(
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @RequestHeader(value = "X-Client-Id", defaultValue = "default-client") String clientId,
            @RequestBody PaymentRequest request) {

        // Rate limit check
        if (!rateLimiterService.tryConsume(clientId)) {
            return ResponseEntity
                    .status(HttpStatus.TOO_MANY_REQUESTS)
                    .body("Rate limit exceeded. Please try again later.");
        }

        return idempotencyService.process(idempotencyKey, request);
    }
}