package com.bancox.service;

import com.bancox.dto.response.OperacaoResponse;
import com.bancox.entity.LancamentoEntity;
import com.bancox.entity.TipoLancamento;
import com.bancox.exception.*;
import com.bancox.repository.ContaRepository;
import com.bancox.repository.LancamentoRepository;
import com.bancox.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ContaService {

    private final ContaRepository contaRepository;
    private final LancamentoRepository lancamentoRepository;

    @Transactional
    public OperacaoResponse credito(UUID contaId, UserPrincipal principal, BigDecimal valor) {
        verificarOwnership(contaId, principal);

        var conta = contaRepository.findById(contaId)
            .orElseThrow(ContaNaoEncontradaException::new);

        if (conta.isBloqueada()) throw new ContaBloqueadaException();
        if (valor.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ValorInvalidoException("O valor do crédito deve ser maior que zero.");
        }

        MDC.put("conta_id", contaId.toString());
        try {
            BigDecimal saldoAnterior = conta.getSaldo();
            BigDecimal novoSaldo = saldoAnterior.add(valor);
            Instant agora = Instant.now();

            lancamentoRepository.save(LancamentoEntity.builder()
                .contaId(contaId)
                .tipo(TipoLancamento.CREDITO)
                .valor(valor)
                .saldoApos(novoSaldo)
                .criadoEm(agora)
                .build());

            conta.atualizarSaldo(novoSaldo);
            contaRepository.save(conta);

            return OperacaoResponse.builder()
                .status("sucesso")
                .operacao("credito")
                .valor(valor)
                .saldoAnterior(saldoAnterior)
                .saldoAtual(novoSaldo)
                .timestamp(agora)
                .build();
        } finally {
            MDC.remove("conta_id");
        }
    }

    @Transactional
    public OperacaoResponse debito(UUID contaId, UserPrincipal principal, BigDecimal valor) {
        verificarOwnership(contaId, principal);

        var conta = contaRepository.findById(contaId)
            .orElseThrow(ContaNaoEncontradaException::new);

        if (conta.isBloqueada()) throw new ContaBloqueadaException();
        if (valor.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ValorInvalidoException("O valor do débito deve ser maior que zero.");
        }
        if (conta.getSaldo().compareTo(valor) < 0) throw new SaldoInsuficienteException();

        MDC.put("conta_id", contaId.toString());
        try {
            BigDecimal saldoAnterior = conta.getSaldo();
            BigDecimal novoSaldo = saldoAnterior.subtract(valor);
            Instant agora = Instant.now();

            lancamentoRepository.save(LancamentoEntity.builder()
                .contaId(contaId)
                .tipo(TipoLancamento.DEBITO)
                .valor(valor)
                .saldoApos(novoSaldo)
                .criadoEm(agora)
                .build());

            conta.atualizarSaldo(novoSaldo);
            contaRepository.save(conta);

            return OperacaoResponse.builder()
                .status("sucesso")
                .operacao("debito")
                .valor(valor)
                .saldoAnterior(saldoAnterior)
                .saldoAtual(novoSaldo)
                .timestamp(agora)
                .build();
        } finally {
            MDC.remove("conta_id");
        }
    }

    private void verificarOwnership(UUID contaId, UserPrincipal principal) {
        if (!principal.possuiConta(contaId)) {
            throw new AcessoNegadoException();
        }
    }
}
