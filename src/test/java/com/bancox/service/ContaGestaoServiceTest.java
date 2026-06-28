package com.bancox.service;

import com.bancox.dto.response.ContaResumoResponse;
import com.bancox.entity.ClienteEntity;
import com.bancox.entity.ContaEntity;
import com.bancox.entity.StatusConta;
import com.bancox.exception.*;
import com.bancox.repository.ClienteRepository;
import com.bancox.repository.ContaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ContaGestaoServiceTest {

    @Mock ContaRepository contaRepository;
    @Mock ClienteRepository clienteRepository;
    @InjectMocks ContaGestaoService contaGestaoService;

    private UUID contaId;
    private ContaEntity contaAtiva;
    private ContaEntity contaBloqueada;

    @BeforeEach
    void setUp() {
        contaId = UUID.randomUUID();
        contaAtiva = ContaEntity.builder()
            .id(contaId).clienteId(UUID.randomUUID())
            .saldo(BigDecimal.ZERO).status(StatusConta.ATIVA).criadoEm(Instant.now()).build();
        contaBloqueada = ContaEntity.builder()
            .id(contaId).clienteId(UUID.randomUUID())
            .saldo(BigDecimal.ZERO).status(StatusConta.BLOQUEADA).criadoEm(Instant.now()).build();
    }

    @Test
    void deveBloqueaContaAtiva() {
        when(contaRepository.findById(contaId)).thenReturn(Optional.of(contaAtiva));
        when(contaRepository.save(any())).thenReturn(contaAtiva);

        var result = contaGestaoService.bloquear(contaId, "Suspeita de fraude");

        assertThat(result.getOperacao()).isEqualTo("bloqueio");
        assertThat(result.getNovoStatus()).isEqualTo("BLOQUEADA");
        assertThat(result.getContaId()).isEqualTo(contaId);
        verify(contaRepository).save(any());
    }

    @Test
    void deveRejeitarBloqueioDeContaJaBloqueada() {
        when(contaRepository.findById(contaId)).thenReturn(Optional.of(contaBloqueada));

        assertThatThrownBy(() -> contaGestaoService.bloquear(contaId, "Motivo qualquer"))
            .isInstanceOf(ContaJaBloqueadaException.class);
        verify(contaRepository, never()).save(any());
    }

    @Test
    void deveDesbloquearContaBloqueada() {
        when(contaRepository.findById(contaId)).thenReturn(Optional.of(contaBloqueada));
        when(contaRepository.save(any())).thenReturn(contaBloqueada);

        var result = contaGestaoService.desbloquear(contaId, "Verificação concluída");

        assertThat(result.getOperacao()).isEqualTo("desbloqueio");
        assertThat(result.getNovoStatus()).isEqualTo("ATIVA");
    }

    @Test
    void deveRejeitarDesbloqueioDeContaJaAtiva() {
        when(contaRepository.findById(contaId)).thenReturn(Optional.of(contaAtiva));

        assertThatThrownBy(() -> contaGestaoService.desbloquear(contaId, "Motivo qualquer"))
            .isInstanceOf(ContaJaAtivaException.class);
    }

    @Test
    void deveRejeitarBloqueioComMotivoVazio() {
        assertThatThrownBy(() -> contaGestaoService.bloquear(contaId, ""))
            .isInstanceOf(MotivoInvalidoException.class);
        verify(contaRepository, never()).findById(any());
    }

    @Test
    void deveRejeitarBloqueioComMotivoNulo() {
        assertThatThrownBy(() -> contaGestaoService.bloquear(contaId, null))
            .isInstanceOf(MotivoInvalidoException.class);
    }

    @Test
    void deveRejeitarBloqueioComMotivoAcima255Chars() {
        String motivoLongo = "x".repeat(256);
        assertThatThrownBy(() -> contaGestaoService.bloquear(contaId, motivoLongo))
            .isInstanceOf(MotivoInvalidoException.class);
    }

    @Test
    void deveRejeitarBloqueioDeContaNaoEncontrada() {
        when(contaRepository.findById(contaId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> contaGestaoService.bloquear(contaId, "Motivo válido"))
            .isInstanceOf(ContaNaoEncontradaException.class);
    }

    @Test
    void deveListarTodasAsContas() {
        UUID clienteId = UUID.randomUUID();
        var cliente = ClienteEntity.builder()
            .id(clienteId).nome("João Silva").cpf("11111111111").criadoEm(Instant.now()).build();

        when(contaRepository.findAllByOrderByCriadoEmDesc()).thenReturn(List.of(contaAtiva));
        when(clienteRepository.findById(contaAtiva.getClienteId())).thenReturn(Optional.of(cliente));

        List<ContaResumoResponse> result = contaGestaoService.listarTodas();

        assertThat(result).hasSize(1);
        verify(contaRepository).findAllByOrderByCriadoEmDesc();
    }

    @Test
    void deveListarContasPorStatus() {
        UUID clienteId = UUID.randomUUID();
        var cliente = ClienteEntity.builder()
            .id(clienteId).nome("Maria").cpf("22222222222").criadoEm(Instant.now()).build();

        when(contaRepository.findByStatusOrderByCriadoEmDesc(StatusConta.ATIVA))
            .thenReturn(List.of(contaAtiva));
        when(clienteRepository.findById(contaAtiva.getClienteId())).thenReturn(Optional.of(cliente));

        List<ContaResumoResponse> result = contaGestaoService.listarPorStatus(StatusConta.ATIVA);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getClienteNome()).isEqualTo("Maria");
    }

    @Test
    void deveRetornarNomeVazioQuandoClienteNaoEncontrado() {
        when(contaRepository.findAllByOrderByCriadoEmDesc()).thenReturn(List.of(contaAtiva));
        when(clienteRepository.findById(any())).thenReturn(Optional.empty());

        List<ContaResumoResponse> result = contaGestaoService.listarTodas();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getClienteNome()).isEmpty();
        assertThat(result.get(0).getClienteCpf()).isEmpty();
    }
}
