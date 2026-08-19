package com.example.solution.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.http.ResponseEntity;

import java.time.Instant;
import java.util.concurrent.CompletableFuture;

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
    private ResponseEntity<String> response;

    @Builder.Default
    private CompletableFuture<ResponseEntity<String>> completionFuture = new CompletableFuture<>();

    @Builder.Default
    private Instant createdAt = Instant.now();
}