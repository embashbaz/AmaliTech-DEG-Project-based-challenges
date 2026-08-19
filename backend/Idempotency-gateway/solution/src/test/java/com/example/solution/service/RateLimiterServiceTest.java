package com.example.solution.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class RateLimiterServiceTest {

    private RateLimiterService rateLimiterService;

    @BeforeEach
    void setUp() {
        // Fresh instance with a limit of 2 requests per minute for every test
        rateLimiterService = new RateLimiterService(2);
    }

    @Test
    void whenWithinLimit_allowsRequest() {
        String clientId = "client-abc";

        assertTrue(rateLimiterService.tryConsume(clientId));
        assertTrue(rateLimiterService.tryConsume(clientId));
    }

    @Test
    void whenLimitExceeded_blocksRequest() {
        String clientId = "client-xyz";

        // Allow first two requests
        assertTrue(rateLimiterService.tryConsume(clientId));
        assertTrue(rateLimiterService.tryConsume(clientId));

        // Third request should be rate-limited (denied)
        assertFalse(rateLimiterService.tryConsume(clientId));
    }
}