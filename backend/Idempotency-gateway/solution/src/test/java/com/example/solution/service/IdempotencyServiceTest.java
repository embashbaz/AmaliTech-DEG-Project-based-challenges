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
        PaymentRequest request = new PaymentRequest(100, "GHS");

        // First call - processed normally
        ResponseEntity<String> firstResponse = idempotencyService.process(key, request);
        assertEquals(HttpStatus.OK, firstResponse.getStatusCode());
        assertEquals("Charged 100 GHS", firstResponse.getBody());
        // Verify X-Cache-Hit header is NOT present on the first request
        assertNull(firstResponse.getHeaders().getFirst("X-Cache-Hit"));

        // Second call - should return cached response with X-Cache-Hit header
        ResponseEntity<String> secondResponse = idempotencyService.process(key, request);
        assertEquals(HttpStatus.OK, secondResponse.getStatusCode());
        assertEquals("Charged 100 GHS", secondResponse.getBody());
        // Verify X-Cache-Hit header IS present and set to "true"
        assertEquals("true", secondResponse.getHeaders().getFirst("X-Cache-Hit"));
    }
}