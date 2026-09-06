package com.zosh.service.ai;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class ChatRateLimiter {

    @Value("${app.ai.rate-limit-per-minute:60}")
    private int maxRequestsPerMinute;

    private final Map<String, Deque<Long>> clientRequestTimestamps = new ConcurrentHashMap<>();

    public synchronized boolean isAllowed(String clientKey) {
        long now = System.currentTimeMillis();
        long oneMinuteAgo = now - 60_000L;

        Deque<Long> timestamps = clientRequestTimestamps.computeIfAbsent(clientKey, k -> new ArrayDeque<>());

        while (!timestamps.isEmpty() && timestamps.peekFirst() < oneMinuteAgo) {
            timestamps.pollFirst();
        }

        if (timestamps.size() >= maxRequestsPerMinute) {
            return false;
        }

        timestamps.addLast(now);
        return true;
    }
}
