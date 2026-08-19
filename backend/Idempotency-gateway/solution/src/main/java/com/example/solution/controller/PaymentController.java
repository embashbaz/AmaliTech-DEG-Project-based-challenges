package com.example.solution.controller;


import com.example.solution.model.PaymentRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/process-payment")
public class PaymentController {

    @PostMapping
    public ResponseEntity processPayment(
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @RequestBody PaymentRequest request) {

        return ResponseEntity.ok("Charged " + request.getAmount() + " " + request.getCurrency());
    }
}
