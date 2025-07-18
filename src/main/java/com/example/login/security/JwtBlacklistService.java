package com.example.login.security;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class JwtBlacklistService {

    // Map<token, expiryTime>
    private final Map<String, Instant> blacklist = new ConcurrentHashMap<>();

    public void blacklistToken(String token, Instant expiry) {
        blacklist.put(token, expiry);
    }

    public boolean isTokenBlacklisted(String token) {
        Instant expiry = blacklist.get(token);
        if (expiry == null) return false;

        // Remove if expired
        if (expiry.isBefore(Instant.now())) {
            blacklist.remove(token);
            return false;
        }
        return true;
    }
    // 🔄 Scheduled cleanup every hour (adjust as needed)
    @Scheduled(fixedRate = 10 * 60 * 1000) // every 10 minutes
    public void cleanupExpiredTokens() {
        Instant now = Instant.now();
        blacklist.entrySet().removeIf(entry -> entry.getValue().isBefore(now));
        System.out.println("✅ Cleaned up expired JWT tokens from blacklist");
    }
}
