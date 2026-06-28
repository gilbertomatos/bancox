package com.bancox.controller;

import com.bancox.entity.Perfil;
import com.bancox.service.AuthService;
import com.bancox.service.JwtService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.ui.Model;
import org.springframework.web.server.ResponseStatusException;

@Controller
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final JwtService jwtService;

    @GetMapping("/login")
    public String loginPage(
            @RequestParam(required = false) String error,
            @RequestParam(required = false) String logout,
            Model model) {

        if (error != null) model.addAttribute("erro", "CPF ou senha incorretos.");
        if (logout != null) model.addAttribute("sucesso", "Você saiu com sucesso.");
        return "auth/login";
    }

    @PostMapping("/auth/login")
    public String login(
            @RequestParam String cpf,
            @RequestParam String senha,
            HttpServletResponse response) {

        try {
            Cookie cookie = authService.login(cpf, senha);
            response.addCookie(cookie);

            // Redirecionar por perfil
            String token = cookie.getValue();
            String perfil = jwtService.extrairPerfil(token);

            if (Perfil.CORRENTISTA.name().equals(perfil)) {
                return "redirect:/";
            } else {
                return "redirect:/admin/contas";
            }
        } catch (ResponseStatusException e) {
            if (e.getStatusCode() == HttpStatus.TOO_MANY_REQUESTS) {
                return "redirect:/login?error=ratelimit";
            }
            return "redirect:/login?error=true";
        } catch (Exception e) {
            return "redirect:/login?error=true";
        }
    }

    @PostMapping("/auth/logout")
    public String logout(HttpServletResponse response) {
        response.addCookie(authService.criarCookieLogout());
        return "redirect:/login?logout=true";
    }
}
