package com.example.solution.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.http.ResponseEntity;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class IdempotencyRecord {

    public enum Status {
        IN_FLIGHT,
        COMPLETED
    }

    private Status status;
    private PaymentRequest requestPayload;
    private ResponseEntity response;
    @Builder.Default
    private Instant createdAt = Instant.now();
}