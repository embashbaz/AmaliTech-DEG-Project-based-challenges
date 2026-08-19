package com.example.solution.service;

import com.example.solution.model.IdempotencyRecord;
import com.example.solution.model.PaymentRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class IdempotencyService {

    private final Map<String, IdempotencyRecord> cache = new ConcurrentHashMap<>();

    public ResponseEntity<String> process(String idempotencyKey, PaymentRequest request) {
        IdempotencyRecord existingRecord = cache.get(idempotencyKey);

        if (existingRecord != null && existingRecord.getStatus() == IdempotencyRecord.Status.COMPLETED) {
            // Check if payload matches
            if (!isPayloadMatching(existingRecord.getRequestPayload(), request)) {
                return ResponseEntity
                        .status(HttpStatus.UNPROCESSABLE_CONTENT)
                        .body("Idempotency key already used for a different request body.");
            }

            // Duplicate request with matching payload
            HttpHeaders headers = new HttpHeaders();
            headers.putAll(existingRecord.getResponse().getHeaders());
            headers.add("X-Cache-Hit", "true");

            return new ResponseEntity<String>(
                    existingRecord.getResponse().getBody(),
                    headers,
                    existingRecord.getResponse().getStatusCode()
            );
        }

        // First-time request
        ResponseEntity<String> response = ResponseEntity.ok("Charged " + request.getAmount() + " " + request.getCurrency());

        IdempotencyRecord record = IdempotencyRecord.builder()
                .status(IdempotencyRecord.Status.COMPLETED)
                .requestPayload(request)
                .response(response)
                .build();

        cache.put(idempotencyKey, record);

        return response;
    }

    private boolean isPayloadMatching(PaymentRequest cached, PaymentRequest incoming) {
        if (cached == null || incoming == null) return false;
        return cached.getAmount() == incoming.getAmount()
                && Objects.equals(cached.getCurrency(), incoming.getCurrency());
    }
}