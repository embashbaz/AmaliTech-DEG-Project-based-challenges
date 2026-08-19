package com.example.solution.service;

import com.example.solution.model.PaymentRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

@Service
public class IdempotencyService {

    public ResponseEntity process(String idempotencyKey, PaymentRequest request) {

        return ResponseEntity.ok("Charged " + request.getAmount() + " " + request.getCurrency());
    }
}
