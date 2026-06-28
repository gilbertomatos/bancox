package com.bancox.service;

import com.bancox.dto.response.ExtratoResponse;
import com.bancox.dto.response.LancamentoResponse;
import com.bancox.exception.AcessoNegadoException;
import com.bancox.exception.ContaNaoEncontradaException;
import com.bancox.exception.IntervaloInvalidoException;
import com.bancox.repository.ContaRepository;
import com.bancox.repository.LancamentoRepository;
import com.bancox.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ExtratoService {

    public static final int MAX_LANCAMENTOS_EXTRATO = 50;
    private static final int MAX_DIAS_EXTRATO = 90;
    private static final int DIAS_PADRAO = 30;
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private final ContaRepository contaRepository;
    private final LancamentoRepository lancamentoRepository;

    public ExtratoResponse extrato(
            UUID contaId,
            UserPrincipal principal,
            LocalDate dataInicio,
            LocalDate dataFim) {

        if (!principal.possuiConta(contaId)) throw new AcessoNegadoException();

        var conta = contaRepository.findById(contaId)
            .orElseThrow(ContaNaoEncontradaException::new);

        LocalDate hoje = LocalDate.now(ZoneOffset.UTC);
        if (dataFim == null) dataFim = hoje;
        if (dataInicio == null) dataInicio = dataFim.minusDays(DIAS_PADRAO);

        if (dataInicio.isAfter(dataFim)) throw new IntervaloInvalidoException();

        // Truncar silenciosamente se exceder 90 dias (DA-08)
        LocalDate limiteInicio = dataFim.minusDays(MAX_DIAS_EXTRATO);
        if (dataInicio.isBefore(limiteInicio)) dataInicio = limiteInicio;

        Instant inicio = dataInicio.atStartOfDay(ZoneOffset.UTC).toInstant();
        Instant fim = dataFim.atTime(23, 59, 59).atZone(ZoneOffset.UTC).toInstant();

        // Busca os mais recentes (DESC) limitado a 50, depois inverte para ordem cronológica
        var lancamentosDesc = new java.util.ArrayList<>(
            lancamentoRepository.findByContaIdAndPeriodo(
                contaId, inicio, fim,
                PageRequest.of(0, MAX_LANCAMENTOS_EXTRATO, Sort.by(Sort.Direction.DESC, "criadoEm"))
            )
        );

        Collections.reverse(lancamentosDesc);

        List<LancamentoResponse> lancamentos = lancamentosDesc.stream()
            .map(l -> LancamentoResponse.builder()
                .tipo(l.getTipo().name().toLowerCase())
                .valor(l.getValor())
                .saldoApos(l.getSaldoApos())
                .timestamp(l.getCriadoEm())
                .transferId(l.getTransferId())
                .build())
            .toList();

        String periodo = dataInicio.format(FMT) + " a " + dataFim.format(FMT);

        return ExtratoResponse.builder()
            .status("sucesso")
            .operacao("extrato")
            .periodo(periodo)
            .totalLancamentos(lancamentos.size())
            .lancamentos(lancamentos)
            .saldoAtual(conta.getSaldo())
            .build();
    }
}
