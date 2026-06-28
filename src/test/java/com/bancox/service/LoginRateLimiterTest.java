package com.bancox.service;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class LoginRateLimiterTest {

    @Test
    void devePermitirAte5TentativasPorMinuto() {
        var rateLimiter = new LoginRateLimiter();

        for (int i = 0; i < 5; i++) {
            assertThat(rateLimiter.tryAcquire("12345678901")).isTrue();
        }
    }

    @Test
    void deveBloquearApos5TentativasNoMesmoCpf() {
        var rateLimiter = new LoginRateLimiter();

        for (int i = 0; i < 5; i++) {
            rateLimiter.tryAcquire("12345678901");
        }

        assertThat(rateLimiter.tryAcquire("12345678901")).isFalse();
    }

    @Test
    void deveManterbucketsIndependentesParaCpfsDiferentes() {
        var rateLimiter = new LoginRateLimiter();

        for (int i = 0; i < 5; i++) {
            rateLimiter.tryAcquire("11111111111");
        }

        assertThat(rateLimiter.tryAcquire("22222222222")).isTrue();
    }
}
