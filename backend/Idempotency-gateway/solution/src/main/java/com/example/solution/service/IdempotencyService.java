package com.example.solution.service;

import com.example.solution.model.IdempotencyRecord;
import com.example.solution.model.PaymentRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class IdempotencyService {

    private final Map<String, IdempotencyRecord> cache = new ConcurrentHashMap<>();

    public ResponseEntity process(String idempotencyKey, PaymentRequest request) {
        IdempotencyRecord existingRecord = cache.get(idempotencyKey);

        if (existingRecord != null && existingRecord.getStatus() == IdempotencyRecord.Status.COMPLETED) {
            // Duplicate request
            HttpHeaders headers = new HttpHeaders();
            headers.putAll(existingRecord.getResponse().getHeaders());
            headers.add("X-Cache-Hit", "true");

            return new ResponseEntity<>(
                    existingRecord.getResponse().getBody(),
                    headers,
                    existingRecord.getResponse().getStatusCode()
            );
        }

        // First-time request
        ResponseEntity response = ResponseEntity.ok("Charged " + request.getAmount() + " " + request.getCurrency());

        // Save completed record to cache
        IdempotencyRecord record = IdempotencyRecord.builder()
                .status(IdempotencyRecord.Status.COMPLETED)
                .requestPayload(request)
                .response(response)
                .build();

        cache.put(idempotencyKey, record);

        return response;
    }
}