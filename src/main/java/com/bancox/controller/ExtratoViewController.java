package com.bancox.controller;

import com.bancox.dto.response.ExtratoResponse;
import com.bancox.dto.response.LancamentoResponse;
import com.bancox.security.UserPrincipal;
import com.bancox.service.ExtratoService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/extrato")
@RequiredArgsConstructor
public class ExtratoViewController {

    private final ExtratoService extratoService;

    @GetMapping
    public String extrato(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataInicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataFim,
            @AuthenticationPrincipal UserPrincipal principal,
            Model model) {

        try {
            ExtratoResponse extrato = extratoService.extrato(
                principal.contaId(), principal, dataInicio, dataFim);

            boolean limiteAtingido = extrato.getTotalLancamentos() >= ExtratoService.MAX_LANCAMENTOS_EXTRATO;

            // Dados para gráfico Chart.js
            var labelsGrafico = new ArrayList<String>();
            var creditosGrafico = new ArrayList<java.math.BigDecimal>();
            var debitosGrafico = new ArrayList<java.math.BigDecimal>();

            calcularDadosGrafico(extrato.getLancamentos(), labelsGrafico, creditosGrafico, debitosGrafico);

            model.addAttribute("lancamentos", extrato.getLancamentos());
            model.addAttribute("saldoAtual", extrato.getSaldoAtual());
            model.addAttribute("periodo", extrato.getPeriodo());
            model.addAttribute("totalLancamentos", extrato.getTotalLancamentos());
            model.addAttribute("limiteAtingido", limiteAtingido);
            model.addAttribute("dataInicio", dataInicio);
            model.addAttribute("dataFim", dataFim);
            model.addAttribute("labelsGrafico", labelsGrafico);
            model.addAttribute("creditosGrafico", creditosGrafico);
            model.addAttribute("debitosGrafico", debitosGrafico);

        } catch (Exception e) {
            model.addAttribute("erro", e.getMessage());
        }

        return "correntista/extrato";
    }

    private void calcularDadosGrafico(
            List<LancamentoResponse> lancamentos,
            List<String> labels,
            List<java.math.BigDecimal> creditos,
            List<java.math.BigDecimal> debitos) {

        var porDia = lancamentos.stream()
            .collect(Collectors.groupingBy(l ->
                l.getTimestamp().atZone(java.time.ZoneOffset.UTC).toLocalDate()));

        porDia.entrySet().stream()
            .sorted(java.util.Map.Entry.comparingByKey())
            .forEach(entry -> {
                labels.add(entry.getKey().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM")));
                creditos.add(entry.getValue().stream()
                    .filter(l -> "credito".equals(l.getTipo()) || "transferencia".equals(l.getTipo()) && l.getSaldoApos().compareTo(l.getValor()) > 0)
                    .map(LancamentoResponse::getValor)
                    .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add));
                debitos.add(entry.getValue().stream()
                    .filter(l -> "debito".equals(l.getTipo()))
                    .map(LancamentoResponse::getValor)
                    .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add));
            });
    }
}
