package com.example.solution.controller;

import com.example.solution.model.PaymentRequest;
import com.example.solution.service.IdempotencyService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/process-payment")
@RequiredArgsConstructor
public class PaymentController {

    private final IdempotencyService idempotencyService;

    @PostMapping
    public ResponseEntity processPayment(
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @RequestBody PaymentRequest request) {

        // Delegate the heavy lifting to the service
        return idempotencyService.process(idempotencyKey, request);
    }
}