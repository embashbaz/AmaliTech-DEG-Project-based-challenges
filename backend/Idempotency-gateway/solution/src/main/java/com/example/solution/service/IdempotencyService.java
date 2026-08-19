package com.example.solution.service;

import com.example.solution.model.IdempotencyRecord;
import com.example.solution.model.PaymentRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class IdempotencyService {

    private final Map<String, IdempotencyRecord> cache = new ConcurrentHashMap<>();

    public ResponseEntity<String> process(String idempotencyKey, PaymentRequest request) {
        IdempotencyRecord newRecord = IdempotencyRecord.builder()
                .status(IdempotencyRecord.Status.IN_FLIGHT)
                .requestPayload(request)
                .build();

        // if another thread already put a record here, we get it back.
        IdempotencyRecord existingRecord = cache.putIfAbsent(idempotencyKey, newRecord);

        if (existingRecord != null) {
            // Key already exists
            if (!isPayloadMatching(existingRecord.getRequestPayload(), request)) {
                return ResponseEntity
                        .status(HttpStatus.UNPROCESSABLE_CONTENT)
                        .body("Idempotency key already used for a different request body.");
            }

            if (existingRecord.getStatus() == IdempotencyRecord.Status.IN_FLIGHT) {
                // Request is currently processing in another thread
                try {
                    ResponseEntity<String> completedResponse = existingRecord.getCompletionFuture().join();
                    return attachCacheHitHeader(completedResponse);
                } catch (Exception e) {
                    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Concurrent execution failed.");
                }
            }

            // Already completed - return cached response
            return attachCacheHitHeader(existingRecord.getResponse());
        }

        // successfully claimed the key
        try {
            // Simulate processing work
            //Todo: we should remove this in production
            Thread.sleep(100);
            ResponseEntity<String> response = ResponseEntity.ok("Charged " + request.getAmount() + " " + request.getCurrency());

            // Update record status and complete the future so waiting threads can proceed
            newRecord.setStatus(IdempotencyRecord.Status.COMPLETED);
            newRecord.setResponse(response);
            newRecord.getCompletionFuture().complete(response);

            return response;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            cache.remove(idempotencyKey);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Request processing interrupted.");
        }
    }

    private boolean isPayloadMatching(PaymentRequest cached, PaymentRequest incoming) {
        if (cached == null || incoming == null) return false;
        return cached.getAmount() == incoming.getAmount()
                && Objects.equals(cached.getCurrency(), incoming.getCurrency());
    }

    private ResponseEntity<String> attachCacheHitHeader(ResponseEntity<String> cachedResponse) {
        HttpHeaders headers = new HttpHeaders();
        headers.putAll(cachedResponse.getHeaders());
        headers.add("X-Cache-Hit", "true");

        return new ResponseEntity<>(
                cachedResponse.getBody(),
                headers,
                cachedResponse.getStatusCode()
        );
    }
}