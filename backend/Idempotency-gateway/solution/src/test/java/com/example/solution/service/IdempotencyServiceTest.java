package com.example.solution.service;

import com.example.solution.model.PaymentRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

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
}