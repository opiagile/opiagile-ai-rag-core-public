package com.opiagile.supportai.webhook;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class WhatsAppRateLimiter {

    private final WhatsAppProperties properties;
    private final Clock clock;
    private final Map<String, Deque<Instant>> hitsByPhone = new ConcurrentHashMap<>();

    @Autowired
    public WhatsAppRateLimiter(WhatsAppProperties properties) {
        this(properties, Clock.systemUTC());
    }

    WhatsAppRateLimiter(WhatsAppProperties properties, Clock clock) {
        this.properties = properties;
        this.clock = clock;
    }

    public boolean allow(String phone) {
        String normalized = PhoneNumberMasker.normalize(phone);
        Instant now = Instant.now(clock);
        Instant windowStart = now.minusSeconds(60);
        Deque<Instant> hits = hitsByPhone.computeIfAbsent(normalized, ignored -> new ArrayDeque<>());
        synchronized (hits) {
            while (!hits.isEmpty() && hits.peekFirst().isBefore(windowStart)) {
                hits.removeFirst();
            }
            if (hits.size() >= properties.getRateLimitPerMinute()) {
                return false;
            }
            hits.addLast(now);
            return true;
        }
    }
}
