package com.bancox.service;

import com.bancox.entity.Perfil;
import com.bancox.entity.UsuarioEntity;
import com.bancox.exception.CredenciaisInvalidasException;
import com.bancox.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock UsuarioRepository usuarioRepository;
    @Mock PasswordEncoder passwordEncoder;
    @Mock JwtService jwtService;
    @Mock LoginRateLimiter rateLimiter;
    @InjectMocks AuthService authService;

    private UsuarioEntity usuario;

    @BeforeEach
    void setUp() {
        // Injetar valores de configuração manualmente (sem Spring context)
        ReflectionTestUtils.setField(authService, "cookieName", "bancox_token");
        ReflectionTestUtils.setField(authService, "httpOnly", true);
        ReflectionTestUtils.setField(authService, "secure", false);
        ReflectionTestUtils.setField(authService, "maxAge", 900);

        usuario = UsuarioEntity.builder()
            .id(UUID.randomUUID())
            .cpf("12345678901")
            .senhaHash("$2a$12$hashed")
            .perfil(Perfil.CORRENTISTA)
            .contaId(UUID.randomUUID())
            .ativo(true)
            .criadoEm(Instant.now())
            .build();
    }

    @Test
    void deveRealizarLoginComSucesso() {
        when(rateLimiter.tryAcquire("12345678901")).thenReturn(true);
        when(usuarioRepository.findByCpf("12345678901")).thenReturn(Optional.of(usuario));
        when(passwordEncoder.matches("senha123", "$2a$12$hashed")).thenReturn(true);
        when(jwtService.gerarToken(any(), any(), any())).thenReturn("jwt-token");

        var cookie = authService.login("12345678901", "senha123");

        assertThat(cookie.getName()).isEqualTo("bancox_token");
        assertThat(cookie.getValue()).isEqualTo("jwt-token");
        assertThat(cookie.isHttpOnly()).isTrue();
        assertThat(cookie.getMaxAge()).isEqualTo(900);
    }

    @Test
    void deveRejeitarLoginComCpfInexistente() {
        when(rateLimiter.tryAcquire("99999999999")).thenReturn(true);
        when(usuarioRepository.findByCpf("99999999999")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login("99999999999", "qualquer"))
            .isInstanceOf(CredenciaisInvalidasException.class);
    }

    @Test
    void deveRejeitarLoginComSenhaErrada() {
        when(rateLimiter.tryAcquire("12345678901")).thenReturn(true);
        when(usuarioRepository.findByCpf("12345678901")).thenReturn(Optional.of(usuario));
        when(passwordEncoder.matches("senhaErrada", "$2a$12$hashed")).thenReturn(false);

        assertThatThrownBy(() -> authService.login("12345678901", "senhaErrada"))
            .isInstanceOf(CredenciaisInvalidasException.class);
    }

    @Test
    void deveRejeitarLoginComUsuarioInativo() {
        var usuarioInativo = UsuarioEntity.builder()
            .id(UUID.randomUUID()).cpf("12345678901")
            .senhaHash("$2a$12$hashed").perfil(Perfil.CORRENTISTA)
            .ativo(false).criadoEm(Instant.now()).build();

        when(rateLimiter.tryAcquire("12345678901")).thenReturn(true);
        when(usuarioRepository.findByCpf("12345678901")).thenReturn(Optional.of(usuarioInativo));

        assertThatThrownBy(() -> authService.login("12345678901", "senha123"))
            .isInstanceOf(CredenciaisInvalidasException.class);
    }

    @Test
    void deveBloquearLoginAposRateLimitExcedido() {
        when(rateLimiter.tryAcquire("12345678901")).thenReturn(false);

        assertThatThrownBy(() -> authService.login("12345678901", "qualquer"))
            .isInstanceOf(ResponseStatusException.class)
            .satisfies(e -> assertThat(((ResponseStatusException) e).getStatusCode())
                .isEqualTo(HttpStatus.TOO_MANY_REQUESTS));

        verify(usuarioRepository, never()).findByCpf(any());
    }

    @Test
    void deveCriarCookieDeLogout() {
        var cookie = authService.criarCookieLogout();

        assertThat(cookie.getName()).isEqualTo("bancox_token");
        assertThat(cookie.getValue()).isEmpty();
        assertThat(cookie.getMaxAge()).isEqualTo(0);
        assertThat(cookie.isHttpOnly()).isTrue();
    }
}
