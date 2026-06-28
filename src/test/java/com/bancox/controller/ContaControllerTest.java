package com.bancox.controller;

import com.bancox.entity.*;
import com.bancox.repository.*;
import com.bancox.service.JwtService;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@Rollback
class ContaControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired JwtService jwtService;
    @Autowired UsuarioRepository usuarioRepository;
    @Autowired ClienteRepository clienteRepository;
    @Autowired ContaRepository contaRepository;
    @Autowired PasswordEncoder passwordEncoder;

    private UUID contaId;
    private Cookie jwtCookie;

    @BeforeEach
    void setUp() {
        var cliente = clienteRepository.save(ClienteEntity.builder()
            .nome("Maria Silva").cpf("11122233344").criadoEm(Instant.now()).build());

        var conta = contaRepository.save(ContaEntity.builder()
            .clienteId(cliente.getId())
            .saldo(new BigDecimal("1000.00"))
            .status(StatusConta.ATIVA)
            .criadoEm(Instant.now())
            .build());
        contaId = conta.getId();

        var usuario = usuarioRepository.save(UsuarioEntity.builder()
            .cpf("11122233344")
            .senhaHash(passwordEncoder.encode("senha123"))
            .perfil(Perfil.CORRENTISTA)
            .contaId(contaId)
            .ativo(true)
            .criadoEm(Instant.now())
            .build());

        String token = jwtService.gerarToken(usuario.getId(), contaId, "CORRENTISTA");
        jwtCookie = new Cookie("bancox_token", token);
    }

    @Test
    void deveRealizarCreditoComSucesso() throws Exception {
        mockMvc.perform(post("/contas/{id}/credito", contaId)
                .cookie(jwtCookie)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"valor\": 500.00}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("sucesso"))
            .andExpect(jsonPath("$.operacao").value("credito"))
            .andExpect(jsonPath("$.saldo_anterior").value(1000.00))
            .andExpect(jsonPath("$.saldo_atual").value(1500.00));
    }

    @Test
    void deveRealizarDebitoComSucesso() throws Exception {
        mockMvc.perform(post("/contas/{id}/debito", contaId)
                .cookie(jwtCookie)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"valor\": 300.00}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("sucesso"))
            .andExpect(jsonPath("$.operacao").value("debito"))
            .andExpect(jsonPath("$.saldo_atual").value(700.00));
    }

    @Test
    void deveRetornar401SemCookie() throws Exception {
        mockMvc.perform(post("/contas/{id}/credito", contaId)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .content("{\"valor\": 100.00}"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void deveRetornar403QuandoOwnershipViolado() throws Exception {
        UUID outraContaId = UUID.randomUUID();

        mockMvc.perform(post("/contas/{id}/credito", outraContaId)
                .cookie(jwtCookie)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"valor\": 100.00}"))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.erro").value("ACESSO_NEGADO"));
    }

    @Test
    void deveRetornar422QuandoSaldoInsuficiente() throws Exception {
        mockMvc.perform(post("/contas/{id}/debito", contaId)
                .cookie(jwtCookie)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"valor\": 9999.00}"))
            .andExpect(status().isUnprocessableEntity())
            .andExpect(jsonPath("$.erro").value("SALDO_INSUFICIENTE"));
    }

    @Test
    void deveRetornarExtrato() throws Exception {
        mockMvc.perform(get("/contas/{id}/extrato", contaId)
                .cookie(jwtCookie)
                .accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("sucesso"))
            .andExpect(jsonPath("$.operacao").value("extrato"))
            .andExpect(jsonPath("$.saldo_atual").exists());
    }
}
