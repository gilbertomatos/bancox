package com.bancox.controller;

import com.bancox.dto.response.LancamentoResponse;
import com.bancox.entity.ContaEntity;
import com.bancox.repository.ContaRepository;
import com.bancox.repository.LancamentoRepository;
import com.bancox.security.UserPrincipal;
import com.bancox.service.ContaService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.util.List;

@Controller
@RequiredArgsConstructor
public class DashboardController {

    private final ContaRepository contaRepository;
    private final LancamentoRepository lancamentoRepository;
    private final ContaService contaService;

    @GetMapping("/")
    public String dashboard(@AuthenticationPrincipal UserPrincipal principal, Model model) {
        ContaEntity conta = contaRepository.findById(principal.contaId()).orElseThrow();

        List<LancamentoResponse> preview = lancamentoRepository
            .findTopByContaId(principal.contaId(),
                PageRequest.of(0, 5, Sort.by(Sort.Direction.DESC, "criadoEm")))
            .stream()
            .map(l -> LancamentoResponse.builder()
                .tipo(l.getTipo().name().toLowerCase())
                .valor(l.getValor())
                .saldoApos(l.getSaldoApos())
                .timestamp(l.getCriadoEm())
                .transferId(l.getTransferId())
                .build())
            .toList();

        model.addAttribute("conta", conta);
        model.addAttribute("preview", preview);
        return "correntista/dashboard";
    }

    @PostMapping("/contas/credito")
    public String credito(
            @RequestParam BigDecimal valor,
            @AuthenticationPrincipal UserPrincipal principal,
            RedirectAttributes redirectAttrs) {

        try {
            var result = contaService.credito(principal.contaId(), principal, valor);
            redirectAttrs.addFlashAttribute("sucesso",
                "Depósito de " + result.getValor() + " realizado. Saldo atual: " + result.getSaldoAtual());
        } catch (Exception e) {
            redirectAttrs.addFlashAttribute("erro", e.getMessage());
        }
        return "redirect:/";
    }

    @PostMapping("/contas/debito")
    public String debito(
            @RequestParam BigDecimal valor,
            @AuthenticationPrincipal UserPrincipal principal,
            RedirectAttributes redirectAttrs) {

        try {
            var result = contaService.debito(principal.contaId(), principal, valor);
            redirectAttrs.addFlashAttribute("sucesso",
                "Saque de " + result.getValor() + " realizado. Saldo atual: " + result.getSaldoAtual());
        } catch (Exception e) {
            redirectAttrs.addFlashAttribute("erro", e.getMessage());
        }
        return "redirect:/";
    }
}
