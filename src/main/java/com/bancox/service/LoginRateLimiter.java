package com.bancox.service;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

// Máximo 5 tentativas de login por CPF por minuto (DA-15)
@Component
public class LoginRateLimiter {

    private final ConcurrentMap<String, Bucket> buckets = new ConcurrentHashMap<>();

    public boolean tryAcquire(String cpf) {
        Bucket bucket = buckets.computeIfAbsent(cpf, this::criarBucket);
        return bucket.tryConsume(1);
    }

    private Bucket criarBucket(String cpf) {
        Bandwidth limite = Bandwidth.builder()
            .capacity(5)
            .refillIntervally(5, Duration.ofMinutes(1))
            .build();
        return Bucket.builder().addLimit(limite).build();
    }
}
