package com.example.solution.service;

import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class RateLimiterService {

    private final int limitForPeriod;
    private final Map<String, List<Long>> clientRequests = new ConcurrentHashMap<>();
    private static final long ONE_MINUTE_MILLIS = 60_000;

    public RateLimiterService() {
        this.limitForPeriod = 10;
    }

    public RateLimiterService(int limitForPeriod) {
        this.limitForPeriod = limitForPeriod;
    }

    public boolean tryConsume(String clientId) {
        long now = Instant.now().toEpochMilli();
        List<Long> timestamps = clientRequests.computeIfAbsent(clientId, k -> new ArrayList<>());

        synchronized (timestamps) {
            timestamps.removeIf(timestamp -> now - timestamp > ONE_MINUTE_MILLIS);

            if (timestamps.size() < limitForPeriod) {
                timestamps.add(now);
                return true;
            }
            return false;
        }
    }
}