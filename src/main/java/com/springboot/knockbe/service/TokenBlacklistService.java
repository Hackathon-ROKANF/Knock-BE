package com.springboot.knockbe.service;

import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class TokenBlacklistService {
    private final Map<String, Long> blacklist = new ConcurrentHashMap<>(); // token -> expiry epoch millis

    public void blacklist(String token, long expiryMillis) {
        blacklist.put(token, expiryMillis);
    }

    public boolean isBlacklisted(String token) {
        if (token == null) return false;
        Long exp = blacklist.get(token);
        if (exp == null) return false;
        if (exp < Instant.now().toEpochMilli()) { // expired -> cleanup
            blacklist.remove(token);
            return false;
        }
        return true;
    }
}

