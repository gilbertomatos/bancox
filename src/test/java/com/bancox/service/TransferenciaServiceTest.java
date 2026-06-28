package com.bancox.service;

import com.bancox.entity.ContaEntity;
import com.bancox.entity.StatusConta;
import com.bancox.entity.TransferenciaEntity;
import com.bancox.exception.*;
import com.bancox.repository.ContaRepository;
import com.bancox.repository.LancamentoRepository;
import com.bancox.repository.TransferenciaRepository;
import com.bancox.security.UserPrincipal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
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
class TransferenciaServiceTest {

    @Mock ContaRepository contaRepository;
    @Mock LancamentoRepository lancamentoRepository;
    @Mock TransferenciaRepository transferenciaRepository;
    @InjectMocks TransferenciaService transferenciaService;

    private UUID contaOrigemId;
    private UUID contaDestinoId;
    private UserPrincipal principal;
    private ContaEntity contaOrigem;
    private ContaEntity contaDestino;

    @BeforeEach
    void setUp() {
        contaOrigemId = UUID.randomUUID();
        contaDestinoId = UUID.randomUUID();
        principal = new UserPrincipal(UUID.randomUUID(), contaOrigemId, "CORRENTISTA");

        contaOrigem = ContaEntity.builder()
            .id(contaOrigemId).clienteId(UUID.randomUUID())
            .saldo(new BigDecimal("1000.00")).status(StatusConta.ATIVA).criadoEm(Instant.now()).build();

        contaDestino = ContaEntity.builder()
            .id(contaDestinoId).clienteId(UUID.randomUUID())
            .saldo(new BigDecimal("200.00")).status(StatusConta.ATIVA).criadoEm(Instant.now()).build();
    }

    @Test
    void deveRealizarTransferenciaComSucesso() {
        when(contaRepository.findById(contaOrigemId)).thenReturn(Optional.of(contaOrigem));
        when(contaRepository.findById(contaDestinoId)).thenReturn(Optional.of(contaDestino));
        when(transferenciaRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        var result = transferenciaService.transferir(
            contaOrigemId, principal, contaDestinoId, new BigDecimal("200.00"));

        assertThat(result.getOperacao()).isEqualTo("transferencia");
        assertThat(result.getValor()).isEqualByComparingTo("200.00");
        assertThat(result.getSaldoAnterior()).isEqualByComparingTo("1000.00");
        assertThat(result.getSaldoAtual()).isEqualByComparingTo("800.00");

        verify(lancamentoRepository, times(2)).save(any());
        verify(contaRepository, times(2)).save(any());
    }

    @Test
    void devePersistirTransferenciaComMesmoTransferId() {
        when(contaRepository.findById(contaOrigemId)).thenReturn(Optional.of(contaOrigem));
        when(contaRepository.findById(contaDestinoId)).thenReturn(Optional.of(contaDestino));
        when(transferenciaRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        transferenciaService.transferir(contaOrigemId, principal, contaDestinoId, new BigDecimal("100.00"));

        var lancamentoCaptor = ArgumentCaptor.forClass(com.bancox.entity.LancamentoEntity.class);
        verify(lancamentoRepository, times(2)).save(lancamentoCaptor.capture());
        List<com.bancox.entity.LancamentoEntity> lancamentos = lancamentoCaptor.getAllValues();

        assertThat(lancamentos).hasSize(2);
        assertThat(lancamentos.get(0).getTransferId()).isNotNull();
        assertThat(lancamentos.get(0).getTransferId()).isEqualTo(lancamentos.get(1).getTransferId());
    }

    @Test
    void deveRejeitarTransferenciaComContasIdenticas() {
        assertThatThrownBy(() ->
            transferenciaService.transferir(contaOrigemId, principal, contaOrigemId, new BigDecimal("100.00")))
            .isInstanceOf(ContasIdenticasException.class);
        verify(contaRepository, never()).findById(any());
    }

    @Test
    void deveRejeitarTransferenciaComOwnershipViolado() {
        var principalErrado = new UserPrincipal(UUID.randomUUID(), UUID.randomUUID(), "CORRENTISTA");

        assertThatThrownBy(() ->
            transferenciaService.transferir(contaOrigemId, principalErrado, contaDestinoId, new BigDecimal("100.00")))
            .isInstanceOf(AcessoNegadoException.class);
    }

    @Test
    void deveRejeitarTransferenciaComContaOrigemNaoEncontrada() {
        when(contaRepository.findById(contaOrigemId)).thenReturn(Optional.empty());

        assertThatThrownBy(() ->
            transferenciaService.transferir(contaOrigemId, principal, contaDestinoId, new BigDecimal("100.00")))
            .isInstanceOf(ContaNaoEncontradaException.class);
    }

    @Test
    void deveRejeitarTransferenciaComContaDestinoNaoEncontrada() {
        when(contaRepository.findById(contaOrigemId)).thenReturn(Optional.of(contaOrigem));
        when(contaRepository.findById(contaDestinoId)).thenReturn(Optional.empty());

        assertThatThrownBy(() ->
            transferenciaService.transferir(contaOrigemId, principal, contaDestinoId, new BigDecimal("100.00")))
            .isInstanceOf(ContaDestinoNaoEncontradaException.class);
    }

    @Test
    void deveRejeitarTransferenciaComSaldoInsuficiente() {
        when(contaRepository.findById(contaOrigemId)).thenReturn(Optional.of(contaOrigem));
        when(contaRepository.findById(contaDestinoId)).thenReturn(Optional.of(contaDestino));

        assertThatThrownBy(() ->
            transferenciaService.transferir(contaOrigemId, principal, contaDestinoId, new BigDecimal("1000.01")))
            .isInstanceOf(SaldoInsuficienteException.class);
        verify(transferenciaRepository, never()).save(any());
    }

    @Test
    void deveRejeitarTransferenciaComValorInvalido() {
        when(contaRepository.findById(contaOrigemId)).thenReturn(Optional.of(contaOrigem));
        when(contaRepository.findById(contaDestinoId)).thenReturn(Optional.of(contaDestino));

        assertThatThrownBy(() ->
            transferenciaService.transferir(contaOrigemId, principal, contaDestinoId, BigDecimal.ZERO))
            .isInstanceOf(ValorInvalidoException.class);
    }

    @Test
    void deveRejeitarTransferenciaComContaOrigemBloqueada() {
        var contaBloqueada = ContaEntity.builder()
            .id(contaOrigemId).clienteId(UUID.randomUUID())
            .saldo(new BigDecimal("1000.00")).status(StatusConta.BLOQUEADA).criadoEm(Instant.now()).build();
        when(contaRepository.findById(contaOrigemId)).thenReturn(Optional.of(contaBloqueada));
        when(contaRepository.findById(contaDestinoId)).thenReturn(Optional.of(contaDestino));

        assertThatThrownBy(() ->
            transferenciaService.transferir(contaOrigemId, principal, contaDestinoId, new BigDecimal("100.00")))
            .isInstanceOf(ContaBloqueadaException.class);
    }

    @Test
    void deveRejeitarTransferenciaComContaDestinoBloqueada() {
        var destinoBloqueado = ContaEntity.builder()
            .id(contaDestinoId).clienteId(UUID.randomUUID())
            .saldo(BigDecimal.ZERO).status(StatusConta.BLOQUEADA).criadoEm(Instant.now()).build();
        when(contaRepository.findById(contaOrigemId)).thenReturn(Optional.of(contaOrigem));
        when(contaRepository.findById(contaDestinoId)).thenReturn(Optional.of(destinoBloqueado));

        assertThatThrownBy(() ->
            transferenciaService.transferir(contaOrigemId, principal, contaDestinoId, new BigDecimal("100.00")))
            .isInstanceOf(ContaDestinoBloqueadaException.class);
    }
}
