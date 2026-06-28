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
class TransferenciaControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired JwtService jwtService;
    @Autowired UsuarioRepository usuarioRepository;
    @Autowired ClienteRepository clienteRepository;
    @Autowired ContaRepository contaRepository;
    @Autowired PasswordEncoder passwordEncoder;

    private UUID contaOrigemId;
    private UUID contaDestinoId;
    private Cookie jwtCookie;

    @BeforeEach
    void setUp() {
        var clienteOrigem = clienteRepository.save(ClienteEntity.builder()
            .nome("João").cpf("11111111111").criadoEm(Instant.now()).build());
        var clienteDestino = clienteRepository.save(ClienteEntity.builder()
            .nome("Maria").cpf("22222222222").criadoEm(Instant.now()).build());

        var origem = contaRepository.save(ContaEntity.builder()
            .clienteId(clienteOrigem.getId())
            .saldo(new BigDecimal("1000.00")).status(StatusConta.ATIVA).criadoEm(Instant.now()).build());
        contaOrigemId = origem.getId();

        var destino = contaRepository.save(ContaEntity.builder()
            .clienteId(clienteDestino.getId())
            .saldo(new BigDecimal("200.00")).status(StatusConta.ATIVA).criadoEm(Instant.now()).build());
        contaDestinoId = destino.getId();

        var usuario = usuarioRepository.save(UsuarioEntity.builder()
            .cpf("11111111111")
            .senhaHash(passwordEncoder.encode("senha"))
            .perfil(Perfil.CORRENTISTA)
            .contaId(contaOrigemId)
            .ativo(true).criadoEm(Instant.now()).build());

        String token = jwtService.gerarToken(usuario.getId(), contaOrigemId, "CORRENTISTA");
        jwtCookie = new Cookie("bancox_token", token);
    }

    @Test
    void deveRealizarTransferenciaComSucesso() throws Exception {
        mockMvc.perform(post("/transferencias")
                .cookie(jwtCookie)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"contaDestino\": \"" + contaDestinoId + "\", \"valor\": 200.00}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("sucesso"))
            .andExpect(jsonPath("$.operacao").value("transferencia"))
            .andExpect(jsonPath("$.saldo_atual").value(800.00));
    }

    @Test
    void deveRetornar422ComContasIdenticas() throws Exception {
        mockMvc.perform(post("/transferencias")
                .cookie(jwtCookie)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"contaDestino\": \"" + contaOrigemId + "\", \"valor\": 100.00}"))
            .andExpect(status().isUnprocessableEntity())
            .andExpect(jsonPath("$.erro").value("CONTAS_IDENTICAS"));
    }

    @Test
    void deveRetornar422ComSaldoInsuficiente() throws Exception {
        mockMvc.perform(post("/transferencias")
                .cookie(jwtCookie)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"contaDestino\": \"" + contaDestinoId + "\", \"valor\": 9999.00}"))
            .andExpect(status().isUnprocessableEntity())
            .andExpect(jsonPath("$.erro").value("SALDO_INSUFICIENTE"));
    }

    @Test
    void deveRetornar404ComContaDestinoInexistente() throws Exception {
        String contaInexistente = UUID.randomUUID().toString();
        mockMvc.perform(post("/transferencias")
                .cookie(jwtCookie)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"contaDestino\": \"" + contaInexistente + "\", \"valor\": 100.00}"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.erro").value("CONTA_DESTINO_NAO_ENCONTRADA"));
    }
}
