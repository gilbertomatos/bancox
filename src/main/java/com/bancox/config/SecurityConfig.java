package com.bancox.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import jakarta.servlet.http.HttpServletResponse;

// JWT em cookie httpOnly — stateless (DA-26)
// CSRF desabilitado — SameSite=Strict no cookie protege (DA-26)
// Sessão STATELESS — sem HttpSession (DA-26)
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtCookieFilter jwtCookieFilter;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        return http
            .csrf(csrf -> csrf.disable())
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth

                // Endpoints públicos
                .requestMatchers(
                    "/auth/login",
                    "/login",
                    "/swagger-ui/**",
                    "/api-docs/**",
                    "/favicon.ico",
                    "/error",
                    "/static/**",
                    "/css/**",
                    "/js/**",
                    "/img/**"
                ).permitAll()

                // Actuator — health e info públicos (DA-20)
                .requestMatchers(
                    "/actuator/health",
                    "/actuator/info"
                ).permitAll()

                // Actuator — demais requerem ADMIN (DA-20)
                .requestMatchers("/actuator/**").hasRole("ADMIN")

                // Operações administrativas
                .requestMatchers(
                    "/contas/*/bloquear",
                    "/contas/*/desbloquear",
                    "/admin/**"
                ).hasAnyRole("OPERADOR", "ADMIN")

                // Demais endpoints requerem autenticação
                .anyRequest().hasAnyRole("CORRENTISTA", "OPERADOR", "ADMIN")
            )
            .exceptionHandling(ex -> ex
                .authenticationEntryPoint((request, response, authException) -> {
                    String accept = request.getHeader("Accept");
                    boolean isApiRequest = accept != null && accept.contains("application/json");
                    boolean isHtmx = request.getHeader("HX-Request") != null;
                    if (isApiRequest && !isHtmx) {
                        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                        response.setContentType("application/json;charset=UTF-8");
                        response.getWriter().write(
                            "{\"status\":\"erro\",\"erro\":\"TOKEN_AUSENTE\",\"mensagem\":\"Token de autenticação não informado.\"}"
                        );
                    } else {
                        response.sendRedirect("/login");
                    }
                })
            )
            .addFilterBefore(jwtCookieFilter, UsernamePasswordAuthenticationFilter.class)
            .build();
    }
}
