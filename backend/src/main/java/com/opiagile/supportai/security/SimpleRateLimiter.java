package com.opiagile.supportai.security;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Component;

@Component
public class SimpleRateLimiter {

    private final Clock clock;
    private final Map<String, Deque<Instant>> hitsByKey = new ConcurrentHashMap<>();

    public SimpleRateLimiter() {
        this(Clock.systemUTC());
    }

    SimpleRateLimiter(Clock clock) {
        this.clock = clock;
    }

    public boolean allow(String key, int limitPerMinute) {
        Instant now = Instant.now(clock);
        Instant windowStart = now.minusSeconds(60);
        Deque<Instant> hits = hitsByKey.computeIfAbsent(key, ignored -> new ArrayDeque<>());
        synchronized (hits) {
            while (!hits.isEmpty() && hits.peekFirst().isBefore(windowStart)) {
                hits.removeFirst();
            }
            if (hits.size() >= limitPerMinute) {
                return false;
            }
            hits.addLast(now);
            return true;
        }
    }
}
