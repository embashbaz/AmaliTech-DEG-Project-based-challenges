package com.example.solution.service;

import com.example.solution.model.IdempotencyRecord;
import com.example.solution.model.PaymentRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

@Service
public class IdempotencyService {

    // this should be private but is public right now because one of the test access it
    final Map<String, IdempotencyRecord> cache = new ConcurrentHashMap<>();
    private static final long TTL_HOURS = 2;

    public ResponseEntity<String> process(String idempotencyKey, PaymentRequest request) {
        cache.computeIfPresent(idempotencyKey, (key, record) -> {
            if (isExpired(record)) {
                return null; // Removes the entry from the map
            }
            return record;
        });

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

    // Background job running every 5 hour to clean up old keys
    @Scheduled(fixedRate = 5, timeUnit = TimeUnit.HOURS)
    public void evictExpiredEntries() {
        cache.entrySet().removeIf(entry -> isExpired(entry.getValue()));
    }

    private boolean isExpired(IdempotencyRecord record) {
        return record.getCreatedAt().plus(TTL_HOURS, ChronoUnit.HOURS).isBefore(Instant.now());
    }
}