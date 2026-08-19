package com.example.solution.service;

import com.example.solution.model.IdempotencyRecord;
import com.example.solution.model.PaymentRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.junit.jupiter.api.Assertions.*;

class IdempotencyServiceTest {

    private IdempotencyService idempotencyService;

    @BeforeEach
    void setUp() {
        idempotencyService = new IdempotencyService();
    }

    @Test
    void whenDuplicateRequestWithSameKeyAndBody_returnsCachedResponseWithHeader() {
        String key = "unique-key-101";
        PaymentRequest request = new PaymentRequest(100, "Rwf");

        // First call - processed normally
        ResponseEntity<String> firstResponse = idempotencyService.process(key, request);
        assertEquals(HttpStatus.OK, firstResponse.getStatusCode());
        assertEquals("Charged 100 Rwf", firstResponse.getBody());
        // Verify X-Cache-Hit header is NOT present on the first request
        assertNull(firstResponse.getHeaders().getFirst("X-Cache-Hit"));

        // Second call - should return cached response with X-Cache-Hit header
        ResponseEntity<String> secondResponse = idempotencyService.process(key, request);
        assertEquals(HttpStatus.OK, secondResponse.getStatusCode());
        assertEquals("Charged 100 Rwf", secondResponse.getBody());
        // Verify X-Cache-Hit header IS present and set to "true"
        assertEquals("true", secondResponse.getHeaders().getFirst("X-Cache-Hit"));
    }

    @Test
    void whenSameKeyUsedWithDifferentPayload_returns422UnprocessableEntity() {
        String key = "unique-key-202";
        PaymentRequest originalRequest = new PaymentRequest(100, "Rwf");
        PaymentRequest modifiedRequest = new PaymentRequest(500, "Rwf");

        // First call with original payload (100 Rwf)
        ResponseEntity<String> firstResponse = idempotencyService.process(key, originalRequest);
        assertEquals(HttpStatus.OK, firstResponse.getStatusCode());

        // Second call with modified payload (500 Rwf) using the same key
        ResponseEntity<String> secondResponse = idempotencyService.process(key, modifiedRequest);

        assertEquals(HttpStatus.UNPROCESSABLE_CONTENT, secondResponse.getStatusCode());
        assertEquals("Idempotency key already used for a different request body.", secondResponse.getBody());
    }

    @Test
    void whenConcurrentRequestsWithSameKey_secondRequestWaitsAndReceivesCachedResponse() throws Exception {
        String key = "concurrent-key-303";
        PaymentRequest request = new PaymentRequest(200, "Rwf");

        ExecutorService executor = Executors.newFixedThreadPool(2);

        // Submit two requests concurrently with the exact same key
        Future<ResponseEntity<String>> future1 = executor.submit(() -> idempotencyService.process(key, request));
        Future<ResponseEntity<String>> future2 = executor.submit(() -> idempotencyService.process(key, request));

        ResponseEntity<String> response1 = future1.get();
        ResponseEntity<String> response2 = future2.get();

        assertEquals(HttpStatus.OK, response1.getStatusCode());
        assertEquals(HttpStatus.OK, response2.getStatusCode());
        assertEquals("Charged 200 Rwf", response1.getBody());
        assertEquals("Charged 200 Rwf", response2.getBody());

        executor.shutdown();
    }

    @Test
    void whenRecordIsOlderThanTwoHours_itIsEvictedAndProcessedFresh() {
        String key = "expired-key-404";
        PaymentRequest request = new PaymentRequest(300, "Rwf");

        // we put an expired record into the cache
        IdempotencyRecord expiredRecord = IdempotencyRecord.builder()
                .status(IdempotencyRecord.Status.COMPLETED)
                .requestPayload(request)
                .response(ResponseEntity.ok("Stale Response"))
                .createdAt(Instant.now().minus(3, ChronoUnit.HOURS))
                .build();

        idempotencyService.cache.put(key, expiredRecord);

        // it should be processed fresh instead of a slate response
        ResponseEntity<String> response = idempotencyService.process(key, request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("Charged 300 Rwf", response.getBody());
        // should not get a slate response
        assertNull(response.getHeaders().getFirst("X-Cache-Hit"));
    }
}