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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@Rollback
class AdminContaControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired JwtService jwtService;
    @Autowired UsuarioRepository usuarioRepository;
    @Autowired ClienteRepository clienteRepository;
    @Autowired ContaRepository contaRepository;
    @Autowired PasswordEncoder passwordEncoder;

    private UUID contaId;
    private Cookie operadorCookie;

    @BeforeEach
    void setUp() {
        var cliente = clienteRepository.save(ClienteEntity.builder()
            .nome("Carlos Operado").cpf("33344455566").criadoEm(Instant.now()).build());

        var conta = contaRepository.save(ContaEntity.builder()
            .clienteId(cliente.getId())
            .saldo(BigDecimal.ZERO)
            .status(StatusConta.ATIVA)
            .criadoEm(Instant.now())
            .build());
        contaId = conta.getId();

        var operador = usuarioRepository.save(UsuarioEntity.builder()
            .cpf("99988877766")
            .senhaHash(passwordEncoder.encode("senha"))
            .perfil(Perfil.OPERADOR)
            .ativo(true)
            .criadoEm(Instant.now())
            .build());

        String token = jwtService.gerarToken(operador.getId(), null, "OPERADOR");
        operadorCookie = new Cookie("bancox_token", token);
    }

    @Test
    void deveBloqueaContaComSucesso() throws Exception {
        mockMvc.perform(patch("/contas/{id}/bloquear", contaId)
                .cookie(operadorCookie)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"motivo\": \"Suspeita de fraude\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("sucesso"))
            .andExpect(jsonPath("$.novo_status").value("BLOQUEADA"));
    }

    @Test
    void deveDesbloquearContaComSucesso() throws Exception {
        var contaBloqueada = contaRepository.save(ContaEntity.builder()
            .clienteId(UUID.randomUUID())
            .saldo(BigDecimal.ZERO)
            .status(StatusConta.BLOQUEADA)
            .criadoEm(Instant.now())
            .build());

        mockMvc.perform(patch("/contas/{id}/desbloquear", contaBloqueada.getId())
                .cookie(operadorCookie)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"motivo\": \"Verificação concluída\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.novo_status").value("ATIVA"));
    }

    @Test
    void deveRetornar403SemPerfilOperador() throws Exception {
        var correntista = usuarioRepository.save(UsuarioEntity.builder()
            .cpf("11122233399")
            .senhaHash(passwordEncoder.encode("senha"))
            .perfil(Perfil.CORRENTISTA)
            .contaId(contaId)
            .ativo(true)
            .criadoEm(Instant.now())
            .build());

        String token = jwtService.gerarToken(correntista.getId(), contaId, "CORRENTISTA");
        var correntistaCookie = new Cookie("bancox_token", token);

        mockMvc.perform(patch("/contas/{id}/bloquear", contaId)
                .cookie(correntistaCookie)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"motivo\": \"Teste\"}"))
            .andExpect(status().isForbidden());
    }
}
