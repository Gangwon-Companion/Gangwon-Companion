package com.gangwon.companion.global.security;

import org.springframework.stereotype.Service;

import java.util.concurrent.ConcurrentHashMap;

@Service
public class TokenBlacklistService {

    private final ConcurrentHashMap<String, Long> blockedTokens = new ConcurrentHashMap<>();

    public void block(String token, long expirationTime) {
        blockedTokens.put(token, expirationTime);
    }

    public boolean isBlocked(String token) {
        Long expirationTime = blockedTokens.get(token);
        if (expirationTime == null) {
            return false;
        }
        if (expirationTime <= System.currentTimeMillis()) {
            blockedTokens.remove(token);
            return false;
        }
        return true;
    }
}
