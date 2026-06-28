package com.bancox.controller;

import com.bancox.entity.StatusConta;
import com.bancox.service.ContaGestaoService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.UUID;

@Controller
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminViewController {

    private final ContaGestaoService contaGestaoService;

    @GetMapping("/contas")
    public String listarContas(
            @RequestParam(required = false) String status,
            Model model) {

        if ("ATIVA".equals(status)) {
            model.addAttribute("contas", contaGestaoService.listarPorStatus(StatusConta.ATIVA));
        } else if ("BLOQUEADA".equals(status)) {
            model.addAttribute("contas", contaGestaoService.listarPorStatus(StatusConta.BLOQUEADA));
        } else {
            model.addAttribute("contas", contaGestaoService.listarTodas());
        }
        model.addAttribute("filtroStatus", status);
        return "operador/gestao-contas";
    }

    @PostMapping("/contas/bloquear")
    public String bloquear(
            @RequestParam UUID contaId,
            @RequestParam String motivo,
            RedirectAttributes redirectAttrs) {

        try {
            contaGestaoService.bloquear(contaId, motivo);
            redirectAttrs.addFlashAttribute("sucesso", "Conta bloqueada com sucesso.");
        } catch (Exception e) {
            redirectAttrs.addFlashAttribute("erro", e.getMessage());
        }
        return "redirect:/admin/contas";
    }

    @PostMapping("/contas/desbloquear")
    public String desbloquear(
            @RequestParam UUID contaId,
            @RequestParam String motivo,
            RedirectAttributes redirectAttrs) {

        try {
            contaGestaoService.desbloquear(contaId, motivo);
            redirectAttrs.addFlashAttribute("sucesso", "Conta desbloqueada com sucesso.");
        } catch (Exception e) {
            redirectAttrs.addFlashAttribute("erro", e.getMessage());
        }
        return "redirect:/admin/contas";
    }
}
