package com.bancox.exception;

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
class GlobalExceptionHandlerTest {

    @Autowired MockMvc mockMvc;
    @Autowired JwtService jwtService;
    @Autowired UsuarioRepository usuarioRepository;
    @Autowired ClienteRepository clienteRepository;
    @Autowired ContaRepository contaRepository;
    @Autowired PasswordEncoder passwordEncoder;

    private UUID contaId;
    private Cookie correntistaCookie;
    private Cookie operadorCookie;

    @BeforeEach
    void setUp() {
        var cliente = clienteRepository.save(ClienteEntity.builder()
            .nome("Teste Handler").cpf("55566677788").criadoEm(Instant.now()).build());

        var conta = contaRepository.save(ContaEntity.builder()
            .clienteId(cliente.getId())
            .saldo(new BigDecimal("100.00"))
            .status(StatusConta.ATIVA)
            .criadoEm(Instant.now())
            .build());
        contaId = conta.getId();

        var correntista = usuarioRepository.save(UsuarioEntity.builder()
            .cpf("55566677788")
            .senhaHash(passwordEncoder.encode("senha"))
            .perfil(Perfil.CORRENTISTA)
            .contaId(contaId)
            .ativo(true)
            .criadoEm(Instant.now())
            .build());

        var operador = usuarioRepository.save(UsuarioEntity.builder()
            .cpf("99977766655")
            .senhaHash(passwordEncoder.encode("senha"))
            .perfil(Perfil.OPERADOR)
            .ativo(true)
            .criadoEm(Instant.now())
            .build());

        correntistaCookie = new Cookie("bancox_token",
            jwtService.gerarToken(correntista.getId(), contaId, "CORRENTISTA"));
        operadorCookie = new Cookie("bancox_token",
            jwtService.gerarToken(operador.getId(), null, "OPERADOR"));
    }

    @Test
    void deveRetornar404QuandoContaNaoEncontrada() throws Exception {
        // Criar usuário com contaId que não existe no banco
        UUID contaInexistente = UUID.randomUUID();
        var usuario = usuarioRepository.save(UsuarioEntity.builder()
            .cpf("44455566677")
            .senhaHash(passwordEncoder.encode("senha"))
            .perfil(Perfil.CORRENTISTA)
            .contaId(contaInexistente)
            .ativo(true)
            .criadoEm(Instant.now())
            .build());

        var cookie = new Cookie("bancox_token",
            jwtService.gerarToken(usuario.getId(), contaInexistente, "CORRENTISTA"));

        mockMvc.perform(post("/contas/{id}/credito", contaInexistente)
                .cookie(cookie)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"valor\": 100.00}"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.erro").value("CONTA_NAO_ENCONTRADA"));
    }

    @Test
    void deveRetornar422QuandoOperacaoEmContaBloqueada() throws Exception {
        var contaBloqueada = contaRepository.save(ContaEntity.builder()
            .clienteId(UUID.randomUUID())
            .saldo(new BigDecimal("500.00"))
            .status(StatusConta.BLOQUEADA)
            .criadoEm(Instant.now())
            .build());

        var usuario = usuarioRepository.save(UsuarioEntity.builder()
            .cpf("11199988877")
            .senhaHash(passwordEncoder.encode("senha"))
            .perfil(Perfil.CORRENTISTA)
            .contaId(contaBloqueada.getId())
            .ativo(true)
            .criadoEm(Instant.now())
            .build());

        var cookie = new Cookie("bancox_token",
            jwtService.gerarToken(usuario.getId(), contaBloqueada.getId(), "CORRENTISTA"));

        mockMvc.perform(post("/contas/{id}/credito", contaBloqueada.getId())
                .cookie(cookie)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"valor\": 100.00}"))
            .andExpect(status().isUnprocessableEntity())
            .andExpect(jsonPath("$.erro").value("CONTA_BLOQUEADA"));
    }

    @Test
    void deveRetornar422QuandoIntervaloInvalido() throws Exception {
        mockMvc.perform(get("/contas/{id}/extrato", contaId)
                .cookie(correntistaCookie)
                .param("dataInicio", "2026-06-01")
                .param("dataFim", "2026-05-01")
                .accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isUnprocessableEntity())
            .andExpect(jsonPath("$.erro").value("INTERVALO_INVALIDO"));
    }

    @Test
    void deveRetornar422QuandoContaJaBloqueada() throws Exception {
        var contaBloqueada = contaRepository.save(ContaEntity.builder()
            .clienteId(UUID.randomUUID())
            .saldo(BigDecimal.ZERO)
            .status(StatusConta.BLOQUEADA)
            .criadoEm(Instant.now())
            .build());

        mockMvc.perform(patch("/contas/{id}/bloquear", contaBloqueada.getId())
                .cookie(operadorCookie)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"motivo\": \"Tentar bloquear novamente\"}"))
            .andExpect(status().isUnprocessableEntity())
            .andExpect(jsonPath("$.erro").value("CONTA_JA_BLOQUEADA"));
    }

    @Test
    void deveRetornar422QuandoContaJaAtiva() throws Exception {
        mockMvc.perform(patch("/contas/{id}/desbloquear", contaId)
                .cookie(operadorCookie)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"motivo\": \"Tentar desbloquear ativa\"}"))
            .andExpect(status().isUnprocessableEntity())
            .andExpect(jsonPath("$.erro").value("CONTA_JA_ATIVA"));
    }

    @Test
    void deveRetornar422QuandoMotivoInvalido() throws Exception {
        mockMvc.perform(patch("/contas/{id}/bloquear", contaId)
                .cookie(operadorCookie)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"motivo\": \"\"}"))
            .andExpect(status().isBadRequest());
    }

    @Test
    void deveRetornar400QuandoCampoObrigatorioAusente() throws Exception {
        mockMvc.perform(post("/contas/{id}/credito", contaId)
                .cookie(correntistaCookie)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.erro").value("CAMPO_FALTANTE"));
    }
}
