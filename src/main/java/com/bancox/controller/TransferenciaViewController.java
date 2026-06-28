package com.bancox.controller;

import com.bancox.repository.ContaRepository;
import com.bancox.security.UserPrincipal;
import com.bancox.service.TransferenciaService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.util.UUID;

@Controller
@RequestMapping("/transferencia")
@RequiredArgsConstructor
public class TransferenciaViewController {

    private final ContaRepository contaRepository;
    private final TransferenciaService transferenciaService;

    @GetMapping
    public String formulario(@AuthenticationPrincipal UserPrincipal principal, Model model) {
        var conta = contaRepository.findById(principal.contaId()).orElseThrow();
        model.addAttribute("saldoAtual", conta.getSaldo());
        return "correntista/transferencia";
    }

    // Etapa 2: validar e exibir confirmação (dados em hidden fields — sem sessão, DA-26)
    @PostMapping
    public String confirmarPreview(
            @RequestParam String contaDestino,
            @RequestParam BigDecimal valor,
            @AuthenticationPrincipal UserPrincipal principal,
            Model model) {

        try {
            UUID contaDestinoId = UUID.fromString(contaDestino);
            var contaOrigem = contaRepository.findById(principal.contaId()).orElseThrow();
            contaRepository.findById(contaDestinoId)
                .orElseThrow(com.bancox.exception.ContaDestinoNaoEncontradaException::new);

            if (contaOrigem.getSaldo().compareTo(valor) < 0) {
                throw new com.bancox.exception.SaldoInsuficienteException();
            }

            model.addAttribute("contaDestino", contaDestino);
            model.addAttribute("valor", valor);
            model.addAttribute("saldoAnterior", contaOrigem.getSaldo());
            model.addAttribute("saldoApos", contaOrigem.getSaldo().subtract(valor));
            return "correntista/transferencia-confirmacao";

        } catch (Exception e) {
            model.addAttribute("saldoAtual", contaRepository.findById(principal.contaId())
                .map(c -> c.getSaldo()).orElse(BigDecimal.ZERO));
            model.addAttribute("erro", e.getMessage());
            return "correntista/transferencia";
        }
    }

    // Etapa 3: executar transferência
    @PostMapping("/confirmar")
    public String executar(
            @RequestParam String contaDestino,
            @RequestParam BigDecimal valor,
            @AuthenticationPrincipal UserPrincipal principal,
            Model model,
            RedirectAttributes redirectAttrs) {

        try {
            UUID contaDestinoId = UUID.fromString(contaDestino);
            var result = transferenciaService.transferir(
                principal.contaId(), principal, contaDestinoId, valor);

            redirectAttrs.addFlashAttribute("transferSucesso", true);
            redirectAttrs.addFlashAttribute("valor", result.getValor());
            redirectAttrs.addFlashAttribute("timestamp", result.getTimestamp());
            return "redirect:/transferencia/comprovante";

        } catch (Exception e) {
            model.addAttribute("contaDestino", contaDestino);
            model.addAttribute("valor", valor);
            model.addAttribute("saldoAnterior", contaRepository.findById(principal.contaId())
                .map(c -> c.getSaldo()).orElse(BigDecimal.ZERO));
            model.addAttribute("saldoApos", contaRepository.findById(principal.contaId())
                .map(c -> c.getSaldo().subtract(valor)).orElse(BigDecimal.ZERO));
            model.addAttribute("erro", e.getMessage());
            return "correntista/transferencia-confirmacao";
        }
    }

    @GetMapping("/comprovante")
    public String comprovante(@AuthenticationPrincipal UserPrincipal principal, Model model) {
        if (!model.containsAttribute("transferSucesso")) {
            return "redirect:/";
        }
        return "correntista/transferencia-comprovante";
    }
}
