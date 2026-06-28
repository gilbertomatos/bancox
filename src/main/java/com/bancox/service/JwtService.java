package com.bancox.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.UUID;

@Service
public class JwtService {

    private final SecretKey key;
    private final long expirationMs;

    public JwtService(
        @Value("${app.jwt.secret}") String secret,
        @Value("${app.jwt.expiration-ms}") long expirationMs
    ) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expirationMs = expirationMs;
    }

    public String gerarToken(UUID userId, UUID contaId, String perfil) {
        return Jwts.builder()
            .subject(userId.toString())
            .claim("conta_id", contaId != null ? contaId.toString() : null)
            .claim("perfil", perfil)
            .issuedAt(new Date())
            .expiration(new Date(System.currentTimeMillis() + expirationMs))
            .signWith(key)
            .compact();
    }

    public boolean isValido(String token) {
        try {
            parsearClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    public Claims parsearClaims(String token) {
        return Jwts.parser()
            .verifyWith(key)
            .build()
            .parseSignedClaims(token)
            .getPayload();
    }

    public UUID extrairUserId(String token) {
        return UUID.fromString(parsearClaims(token).getSubject());
    }

    public UUID extrairContaId(String token) {
        String contaIdStr = parsearClaims(token).get("conta_id", String.class);
        return contaIdStr != null ? UUID.fromString(contaIdStr) : null;
    }

    public String extrairPerfil(String token) {
        return parsearClaims(token).get("perfil", String.class);
    }

    public long getExpirationMs() {
        return expirationMs;
    }
}
