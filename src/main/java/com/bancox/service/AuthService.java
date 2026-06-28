package com.bancox.service;

import com.bancox.exception.CredenciaisInvalidasException;
import com.bancox.repository.UsuarioRepository;
import jakarta.servlet.http.Cookie;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final LoginRateLimiter rateLimiter;

    @Value("${app.jwt.cookie.name:bancox_token}")
    private String cookieName;

    @Value("${app.jwt.cookie.http-only:true}")
    private boolean httpOnly;

    @Value("${app.jwt.cookie.secure:false}")
    private boolean secure;

    @Value("${app.jwt.cookie.max-age:900}")
    private int maxAge;

    public Cookie login(String cpf, String senha) {
        if (!rateLimiter.tryAcquire(cpf)) {
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,
                "Muitas tentativas de login. Tente novamente em 1 minuto.");
        }

        var usuario = usuarioRepository.findByCpf(cpf)
            .filter(u -> u.isAtivo())
            .filter(u -> passwordEncoder.matches(senha, u.getSenhaHash()))
            .orElseThrow(CredenciaisInvalidasException::new);

        String token = jwtService.gerarToken(
            usuario.getId(),
            usuario.getContaId(),
            usuario.getPerfil().name()
        );

        Cookie cookie = new Cookie(cookieName, token);
        cookie.setHttpOnly(httpOnly);
        cookie.setSecure(secure);
        cookie.setPath("/");
        cookie.setMaxAge(maxAge);
        return cookie;
    }

    public Cookie criarCookieLogout() {
        Cookie cookie = new Cookie(cookieName, "");
        cookie.setHttpOnly(httpOnly);
        cookie.setSecure(secure);
        cookie.setPath("/");
        cookie.setMaxAge(0);
        return cookie;
    }
}
