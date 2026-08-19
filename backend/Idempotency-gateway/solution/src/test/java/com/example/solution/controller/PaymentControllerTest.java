package com.example.solution.controller;

import com.example.solution.model.PaymentRequest;
import com.example.solution.service.IdempotencyService;
import com.example.solution.service.RateLimiterService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PaymentController.class)
class PaymentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private IdempotencyService idempotencyService;

    @MockitoBean
    private RateLimiterService rateLimiterService;

    @BeforeEach
    void setUp() {
        when(rateLimiterService.tryConsume(anyString())).thenReturn(true);
    }

    @Test
    void whenMissingIdempotencyKey_thenReturns400BadRequest() throws Exception {
        String requestBody = "{\"amount\": 100, \"currency\": \"Rwf\"}";
        mockMvc.perform(post("/process-payment")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest());
    }

    @Test
    void whenValidRequestWithKey_thenReturns200Ok() throws Exception {
        String requestBody = "{\"amount\": 100, \"currency\": \"Rwf\"}";
        String idempotencyKey = "key-12345";

        when(idempotencyService.process(eq(idempotencyKey), any(PaymentRequest.class)))
                .thenReturn(ResponseEntity.ok("Charged 100 Rwf"));

        mockMvc.perform(post("/process-payment")
                        .header("Idempotency-Key", idempotencyKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(content().string("Charged 100 Rwf"));
    }
}