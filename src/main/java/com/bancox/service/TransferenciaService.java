package com.bancox.service;

import com.bancox.dto.response.OperacaoResponse;
import com.bancox.entity.LancamentoEntity;
import com.bancox.entity.TipoLancamento;
import com.bancox.entity.TransferenciaEntity;
import com.bancox.exception.*;
import com.bancox.repository.ContaRepository;
import com.bancox.repository.LancamentoRepository;
import com.bancox.repository.TransferenciaRepository;
import com.bancox.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TransferenciaService {

    private static final Logger log = LoggerFactory.getLogger(TransferenciaService.class);

    private final ContaRepository contaRepository;
    private final LancamentoRepository lancamentoRepository;
    private final TransferenciaRepository transferenciaRepository;

    // Toda a validação ocorre ANTES de qualquer escrita (DA-05)
    @Transactional
    public OperacaoResponse transferir(
            UUID contaOrigemId,
            UserPrincipal principal,
            UUID contaDestinoId,
            BigDecimal valor) {

        if (!principal.possuiConta(contaOrigemId)) throw new AcessoNegadoException();
        if (contaOrigemId.equals(contaDestinoId)) throw new ContasIdenticasException();

        var contaOrigem = contaRepository.findById(contaOrigemId)
            .orElseThrow(ContaNaoEncontradaException::new);
        var contaDestino = contaRepository.findById(contaDestinoId)
            .orElseThrow(ContaDestinoNaoEncontradaException::new);

        if (contaOrigem.isBloqueada()) throw new ContaBloqueadaException();
        if (contaDestino.isBloqueada()) throw new ContaDestinoBloqueadaException();

        if (valor.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ValorInvalidoException("O valor da transferência deve ser maior que zero.");
        }
        if (contaOrigem.getSaldo().compareTo(valor) < 0) throw new SaldoInsuficienteException();

        UUID transferId = UUID.randomUUID();
        MDC.put("transfer_id", transferId.toString());
        MDC.put("conta_id", contaOrigemId.toString());

        try {
            Instant agora = Instant.now();

            BigDecimal saldoOrigem = contaOrigem.getSaldo();
            BigDecimal novoSaldoOrigem = saldoOrigem.subtract(valor);
            BigDecimal novoSaldoDestino = contaDestino.getSaldo().add(valor);

            // saveAndFlush garante INSERT imediato antes dos lançamentos que referenciam transfer_id por FK
            TransferenciaEntity transferencia = transferenciaRepository.saveAndFlush(
                TransferenciaEntity.builder()
                    .id(transferId)
                    .contaOrigem(contaOrigemId)
                    .contaDestino(contaDestinoId)
                    .valor(valor)
                    .criadoEm(agora)
                    .build()
            );

            lancamentoRepository.save(LancamentoEntity.builder()
                .contaId(contaOrigemId)
                .tipo(TipoLancamento.TRANSFERENCIA)
                .valor(valor)
                .saldoApos(novoSaldoOrigem)
                .transferId(transferencia.getId())
                .criadoEm(agora)
                .build());

            lancamentoRepository.save(LancamentoEntity.builder()
                .contaId(contaDestinoId)
                .tipo(TipoLancamento.TRANSFERENCIA)
                .valor(valor)
                .saldoApos(novoSaldoDestino)
                .transferId(transferencia.getId())
                .criadoEm(agora)
                .build());

            contaOrigem.atualizarSaldo(novoSaldoOrigem);
            contaDestino.atualizarSaldo(novoSaldoDestino);
            contaRepository.save(contaOrigem);
            contaRepository.save(contaDestino);

            return OperacaoResponse.builder()
                .status("sucesso")
                .operacao("transferencia")
                .valor(valor)
                .saldoAnterior(saldoOrigem)
                .saldoAtual(novoSaldoOrigem)
                .timestamp(agora)
                .build();

        } catch (BancoxException e) {
            throw e;
        } catch (Exception e) {
            log.error("Falha na transação transfer_id={}: {}", transferId, e.getMessage());
            throw new FalhaTransacaoException();
        } finally {
            MDC.remove("transfer_id");
            MDC.remove("conta_id");
        }
    }
}
