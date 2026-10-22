package com.thoonidev.portfolio.service;

import java.time.Clock;
import java.util.HashMap;
import java.util.Map;

import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Autowired;

import com.thoonidev.portfolio.config.ContactProperties;

/** Límites en memoria para una sola instancia. No confía en cabeceras de IP del visitante. */
@Component
public class ContactRateLimiter {
    private static final int MAX_CLIENTS = 10_000;
    private final Map<String, Window> clients = new HashMap<>();
    private final ContactProperties properties;
    private final Clock clock;
    private Window global;

    @Autowired
    public ContactRateLimiter(ContactProperties properties) {
        this(properties, Clock.systemUTC());
    }

    ContactRateLimiter(ContactProperties properties, Clock clock) {
        this.properties = properties;
        this.clock = clock;
    }

    /** Cero indica permiso; otro valor es el tiempo de espera en segundos. */
    public synchronized long retryAfter(String client) {
        long now = clock.millis();
        clients.entrySet().removeIf(entry -> entry.getValue().expiresAt <= now);
        if (global == null || global.expiresAt <= now) {
            global = new Window(now + properties.window().toMillis());
        }
        Window window = clients.get(client);
        if (window == null) {
            if (clients.size() >= MAX_CLIENTS) return Math.max(1, properties.window().toSeconds());
            window = new Window(now + properties.window().toMillis());
            clients.put(client, window);
        }
        if (window.count >= properties.maxRequests()) return secondsUntil(window.expiresAt, now);
        if (global.count >= properties.globalMaxRequests()) return secondsUntil(global.expiresAt, now);
        window.count++;
        global.count++;
        return 0;
    }

    private long secondsUntil(long expiresAt, long now) {
        return Math.max(1, (expiresAt - now + 999) / 1000);
    }

    private static final class Window {
        private final long expiresAt;
        private int count;

        private Window(long expiresAt) { this.expiresAt = expiresAt; }
    }
}
