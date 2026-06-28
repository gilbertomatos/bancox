package com.bancox.service;

import com.bancox.entity.ContaEntity;
import com.bancox.entity.LancamentoEntity;
import com.bancox.entity.StatusConta;
import com.bancox.entity.TipoLancamento;
import com.bancox.exception.AcessoNegadoException;
import com.bancox.exception.ContaNaoEncontradaException;
import com.bancox.exception.IntervaloInvalidoException;
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
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ExtratoServiceTest {

    @Mock ContaRepository contaRepository;
    @Mock LancamentoRepository lancamentoRepository;
    @InjectMocks ExtratoService extratoService;

    private UUID contaId;
    private UserPrincipal principal;
    private ContaEntity conta;

    @BeforeEach
    void setUp() {
        contaId = UUID.randomUUID();
        principal = new UserPrincipal(UUID.randomUUID(), contaId, "CORRENTISTA");
        conta = ContaEntity.builder()
            .id(contaId).clienteId(UUID.randomUUID())
            .saldo(new BigDecimal("500.00")).status(StatusConta.ATIVA).criadoEm(Instant.now()).build();
    }

    @Test
    void deveRetornarExtratoComSucesso() {
        when(contaRepository.findById(contaId)).thenReturn(Optional.of(conta));
        when(lancamentoRepository.findByContaIdAndPeriodo(any(), any(), any(), any()))
            .thenReturn(List.of(criarLancamento(TipoLancamento.CREDITO, "100.00", "600.00")));

        var result = extratoService.extrato(contaId, principal, null, null);

        assertThat(result.getStatus()).isEqualTo("sucesso");
        assertThat(result.getOperacao()).isEqualTo("extrato");
        assertThat(result.getTotalLancamentos()).isEqualTo(1);
        assertThat(result.getSaldoAtual()).isEqualByComparingTo("500.00");
        assertThat(result.getLancamentos()).hasSize(1);
        assertThat(result.getLancamentos().get(0).getTipo()).isEqualTo("credito");
    }

    @Test
    void deveRetornarListaVaziaQuandoSemLancamentos() {
        when(contaRepository.findById(contaId)).thenReturn(Optional.of(conta));
        when(lancamentoRepository.findByContaIdAndPeriodo(any(), any(), any(), any()))
            .thenReturn(List.of());

        var result = extratoService.extrato(contaId, principal, null, null);

        assertThat(result.getTotalLancamentos()).isEqualTo(0);
        assertThat(result.getLancamentos()).isEmpty();
    }

    @Test
    void deveTruncarPeriodoAcimaDe90Dias() {
        when(contaRepository.findById(contaId)).thenReturn(Optional.of(conta));
        when(lancamentoRepository.findByContaIdAndPeriodo(any(), any(), any(), any()))
            .thenReturn(List.of());

        LocalDate dataFim = LocalDate.now(ZoneOffset.UTC);
        LocalDate dataInicio = dataFim.minusDays(120); // 120 dias — excede 90

        var inicioCaptor = ArgumentCaptor.forClass(Instant.class);
        extratoService.extrato(contaId, principal, dataInicio, dataFim);

        verify(lancamentoRepository).findByContaIdAndPeriodo(eq(contaId), inicioCaptor.capture(), any(), any());

        // Deve ter truncado para 90 dias antes do dataFim
        Instant esperado = dataFim.minusDays(90).atStartOfDay(ZoneOffset.UTC).toInstant();
        assertThat(inicioCaptor.getValue()).isEqualTo(esperado);
    }

    @Test
    void deveRejeitarIntervaloInvalido() {
        when(contaRepository.findById(contaId)).thenReturn(Optional.of(conta));

        LocalDate inicio = LocalDate.now().plusDays(1);
        LocalDate fim = LocalDate.now();

        assertThatThrownBy(() -> extratoService.extrato(contaId, principal, inicio, fim))
            .isInstanceOf(IntervaloInvalidoException.class);
    }

    @Test
    void deveRejeitarQuandoContaNaoEncontrada() {
        when(contaRepository.findById(contaId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> extratoService.extrato(contaId, principal, null, null))
            .isInstanceOf(ContaNaoEncontradaException.class);
    }

    @Test
    void deveRejeitarQuandoOwnershipViolado() {
        var principalErrado = new UserPrincipal(UUID.randomUUID(), UUID.randomUUID(), "CORRENTISTA");

        assertThatThrownBy(() -> extratoService.extrato(contaId, principalErrado, null, null))
            .isInstanceOf(AcessoNegadoException.class);
        verify(contaRepository, never()).findById(any());
    }

    @Test
    void deveRetornarLancamentosEmOrdemCronologicaCrescente() {
        Instant t1 = Instant.parse("2026-06-01T10:00:00Z");
        Instant t2 = Instant.parse("2026-06-02T10:00:00Z");

        var lancamentoMaisRecente = LancamentoEntity.builder()
            .id(UUID.randomUUID()).contaId(contaId)
            .tipo(TipoLancamento.DEBITO).valor(new BigDecimal("50.00"))
            .saldoApos(new BigDecimal("450.00")).criadoEm(t2).build();

        var lancamentoMaisAntigo = LancamentoEntity.builder()
            .id(UUID.randomUUID()).contaId(contaId)
            .tipo(TipoLancamento.CREDITO).valor(new BigDecimal("100.00"))
            .saldoApos(new BigDecimal("500.00")).criadoEm(t1).build();

        // Repository retorna em DESC (mais recente primeiro)
        when(contaRepository.findById(contaId)).thenReturn(Optional.of(conta));
        when(lancamentoRepository.findByContaIdAndPeriodo(any(), any(), any(), any()))
            .thenReturn(List.of(lancamentoMaisRecente, lancamentoMaisAntigo));

        var result = extratoService.extrato(contaId, principal, null, null);

        // Service deve inverter para ordem ASC
        assertThat(result.getLancamentos().get(0).getTimestamp()).isEqualTo(t1);
        assertThat(result.getLancamentos().get(1).getTimestamp()).isEqualTo(t2);
    }

    private LancamentoEntity criarLancamento(TipoLancamento tipo, String valor, String saldoApos) {
        return LancamentoEntity.builder()
            .id(UUID.randomUUID()).contaId(contaId)
            .tipo(tipo)
            .valor(new BigDecimal(valor))
            .saldoApos(new BigDecimal(saldoApos))
            .criadoEm(Instant.now())
            .build();
    }
}
