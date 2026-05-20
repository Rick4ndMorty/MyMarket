package com.tradestation.common.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Map;

public class JwtUtil {

    private static final String DEFAULT_SECRET = "tradestation-jwt-secret-key-2024-min-length-256-bits-xxxxx";
    private static final long DEFAULT_EXPIRATION = 86400000L; // 24h

    private static SecretKey getKey(String secret) {
        byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        // Ensure key is at least 256 bits for HS256
        if (keyBytes.length < 32) {
            byte[] padded = new byte[32];
            System.arraycopy(keyBytes, 0, padded, 0, keyBytes.length);
            keyBytes = padded;
        }
        return Keys.hmacShaKeyFor(keyBytes);
    }

    public static String generateToken(Long userId, String username, String role) {
        return generateToken(userId, username, role, DEFAULT_SECRET, DEFAULT_EXPIRATION);
    }

    public static String generateToken(Long userId, String username, String role, String secret, long expiration) {
        return Jwts.builder()
                .setSubject(String.valueOf(userId))
                .claim("username", username)
                .claim("role", role)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(getKey(secret), SignatureAlgorithm.HS256)
                .compact();
    }

    public static Claims parseToken(String token) {
        return parseToken(token, DEFAULT_SECRET);
    }

    public static Claims parseToken(String token, String secret) {
        return Jwts.parserBuilder()
                .setSigningKey(getKey(secret))
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    public static Long getUserId(Claims claims) {
        return Long.valueOf(claims.getSubject());
    }

    public static String getUsername(Claims claims) {
        return claims.get("username", String.class);
    }

    public static String getRole(Claims claims) {
        return claims.get("role", String.class);
    }
}
