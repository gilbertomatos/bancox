package com.bancox.service;

import com.bancox.entity.ContaEntity;
import com.bancox.entity.StatusConta;
import com.bancox.exception.*;
import com.bancox.repository.ContaRepository;
import com.bancox.repository.LancamentoRepository;
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
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ContaServiceTest {

    @Mock ContaRepository contaRepository;
    @Mock LancamentoRepository lancamentoRepository;
    @InjectMocks ContaService contaService;

    private UUID contaId;
    private UserPrincipal principal;
    private ContaEntity contaAtiva;

    @BeforeEach
    void setUp() {
        contaId = UUID.randomUUID();
        principal = new UserPrincipal(UUID.randomUUID(), contaId, "CORRENTISTA");
        contaAtiva = ContaEntity.builder()
            .id(contaId)
            .clienteId(UUID.randomUUID())
            .saldo(new BigDecimal("1000.00"))
            .status(StatusConta.ATIVA)
            .criadoEm(Instant.now())
            .build();
    }

    // ─── UC-01: Crédito ──────────────────────────────────────────────────────

    @Test
    void deveRealizarCreditoComSucesso() {
        when(contaRepository.findById(contaId)).thenReturn(Optional.of(contaAtiva));
        when(contaRepository.save(any())).thenReturn(contaAtiva);

        var result = contaService.credito(contaId, principal, new BigDecimal("500.00"));

        assertThat(result.getOperacao()).isEqualTo("credito");
        assertThat(result.getValor()).isEqualByComparingTo("500.00");
        assertThat(result.getSaldoAnterior()).isEqualByComparingTo("1000.00");
        assertThat(result.getSaldoAtual()).isEqualByComparingTo("1500.00");
        assertThat(result.getStatus()).isEqualTo("sucesso");
        verify(lancamentoRepository).save(any());
    }

    @Test
    void devePermitirCreditoEmContaComSaldoZero() {
        var contaZerada = ContaEntity.builder()
            .id(contaId).clienteId(UUID.randomUUID())
            .saldo(BigDecimal.ZERO).status(StatusConta.ATIVA).criadoEm(Instant.now()).build();
        when(contaRepository.findById(contaId)).thenReturn(Optional.of(contaZerada));
        when(contaRepository.save(any())).thenReturn(contaZerada);

        var result = contaService.credito(contaId, principal, new BigDecimal("100.00"));

        assertThat(result.getSaldoAnterior()).isEqualByComparingTo("0.00");
        assertThat(result.getSaldoAtual()).isEqualByComparingTo("100.00");
    }

    @Test
    void deveRejeitarCreditoComValorZero() {
        when(contaRepository.findById(contaId)).thenReturn(Optional.of(contaAtiva));

        assertThatThrownBy(() -> contaService.credito(contaId, principal, BigDecimal.ZERO))
            .isInstanceOf(ValorInvalidoException.class);
        verify(lancamentoRepository, never()).save(any());
    }

    @Test
    void deveRejeitarCreditoComValorNegativo() {
        when(contaRepository.findById(contaId)).thenReturn(Optional.of(contaAtiva));

        assertThatThrownBy(() -> contaService.credito(contaId, principal, new BigDecimal("-10.00")))
            .isInstanceOf(ValorInvalidoException.class);
    }

    @Test
    void deveRejeitarCreditoEmContaNaoEncontrada() {
        when(contaRepository.findById(contaId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> contaService.credito(contaId, principal, new BigDecimal("100.00")))
            .isInstanceOf(ContaNaoEncontradaException.class);
    }

    @Test
    void deveRejeitarCreditoEmContaBloqueada() {
        var contaBloqueada = ContaEntity.builder()
            .id(contaId).clienteId(UUID.randomUUID())
            .saldo(BigDecimal.ZERO).status(StatusConta.BLOQUEADA).criadoEm(Instant.now()).build();
        when(contaRepository.findById(contaId)).thenReturn(Optional.of(contaBloqueada));

        assertThatThrownBy(() -> contaService.credito(contaId, principal, new BigDecimal("100.00")))
            .isInstanceOf(ContaBloqueadaException.class);
    }

    @Test
    void deveRejeitarCreditoQuandoOwnershipViolado() {
        var outroContaId = UUID.randomUUID();
        var principalOutraConta = new UserPrincipal(UUID.randomUUID(), outroContaId, "CORRENTISTA");

        assertThatThrownBy(() -> contaService.credito(contaId, principalOutraConta, new BigDecimal("100.00")))
            .isInstanceOf(AcessoNegadoException.class);
        verify(contaRepository, never()).findById(any());
    }

    // ─── UC-02: Débito ───────────────────────────────────────────────────────

    @Test
    void deveRealizarDebitoComSucesso() {
        when(contaRepository.findById(contaId)).thenReturn(Optional.of(contaAtiva));
        when(contaRepository.save(any())).thenReturn(contaAtiva);

        var result = contaService.debito(contaId, principal, new BigDecimal("300.00"));

        assertThat(result.getOperacao()).isEqualTo("debito");
        assertThat(result.getSaldoAnterior()).isEqualByComparingTo("1000.00");
        assertThat(result.getSaldoAtual()).isEqualByComparingTo("700.00");
    }

    @Test
    void devePermitirDebitoExatoDoSaldoDisponivel() {
        when(contaRepository.findById(contaId)).thenReturn(Optional.of(contaAtiva));
        when(contaRepository.save(any())).thenReturn(contaAtiva);

        var result = contaService.debito(contaId, principal, new BigDecimal("1000.00"));

        assertThat(result.getSaldoAtual()).isEqualByComparingTo("0.00");
    }

    @Test
    void deveRejeitarDebitoComSaldoInsuficiente() {
        when(contaRepository.findById(contaId)).thenReturn(Optional.of(contaAtiva));

        assertThatThrownBy(() -> contaService.debito(contaId, principal, new BigDecimal("1000.01")))
            .isInstanceOf(SaldoInsuficienteException.class);
        verify(lancamentoRepository, never()).save(any());
    }

    @Test
    void deveRejeitarDebitoComValorInvalido() {
        when(contaRepository.findById(contaId)).thenReturn(Optional.of(contaAtiva));

        assertThatThrownBy(() -> contaService.debito(contaId, principal, BigDecimal.ZERO))
            .isInstanceOf(ValorInvalidoException.class);
    }

    @Test
    void deveRejeitarDebitoEmContaBloqueada() {
        var contaBloqueada = ContaEntity.builder()
            .id(contaId).clienteId(UUID.randomUUID())
            .saldo(new BigDecimal("1000.00")).status(StatusConta.BLOQUEADA).criadoEm(Instant.now()).build();
        when(contaRepository.findById(contaId)).thenReturn(Optional.of(contaBloqueada));

        assertThatThrownBy(() -> contaService.debito(contaId, principal, new BigDecimal("100.00")))
            .isInstanceOf(ContaBloqueadaException.class);
    }

    @Test
    void deveRejeitarDebitoQuandoOwnershipViolado() {
        var principalErrado = new UserPrincipal(UUID.randomUUID(), UUID.randomUUID(), "CORRENTISTA");

        assertThatThrownBy(() -> contaService.debito(contaId, principalErrado, new BigDecimal("100.00")))
            .isInstanceOf(AcessoNegadoException.class);
    }

    @Test
    void deveSalvarLancamentoComDadosCorretos() {
        when(contaRepository.findById(contaId)).thenReturn(Optional.of(contaAtiva));
        when(contaRepository.save(any())).thenReturn(contaAtiva);

        contaService.credito(contaId, principal, new BigDecimal("200.00"));

        var lancamentoCaptor = ArgumentCaptor.forClass(com.bancox.entity.LancamentoEntity.class);
        verify(lancamentoRepository).save(lancamentoCaptor.capture());
        var lancamento = lancamentoCaptor.getValue();

        assertThat(lancamento.getContaId()).isEqualTo(contaId);
        assertThat(lancamento.getValor()).isEqualByComparingTo("200.00");
        assertThat(lancamento.getSaldoApos()).isEqualByComparingTo("1200.00");
        assertThat(lancamento.getTipo()).isEqualTo(com.bancox.entity.TipoLancamento.CREDITO);
        assertThat(lancamento.getTransferId()).isNull();
    }
}
