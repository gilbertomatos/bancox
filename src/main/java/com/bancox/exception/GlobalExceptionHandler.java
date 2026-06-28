package com.bancox.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    private ResponseEntity<Map<String, String>> erro(HttpStatus status, String codigo, String mensagem) {
        return ResponseEntity.status(status).body(Map.of(
            "status", "erro",
            "erro", codigo,
            "mensagem", mensagem
        ));
    }

    // ─── UC-00 ────────────────────────────────────────────────────────────────

    @ExceptionHandler(CredenciaisInvalidasException.class)
    public ResponseEntity<?> handleCredenciais(CredenciaisInvalidasException ex) {
        return erro(HttpStatus.UNAUTHORIZED, "CREDENCIAIS_INVALIDAS", ex.getMessage());
    }

    // ─── UC-01 / UC-02 ────────────────────────────────────────────────────────

    @ExceptionHandler(ContaNaoEncontradaException.class)
    public ResponseEntity<?> handleContaNaoEncontrada(ContaNaoEncontradaException ex) {
        return erro(HttpStatus.NOT_FOUND, "CONTA_NAO_ENCONTRADA", ex.getMessage());
    }

    @ExceptionHandler(ValorInvalidoException.class)
    public ResponseEntity<?> handleValorInvalido(ValorInvalidoException ex) {
        return erro(HttpStatus.UNPROCESSABLE_ENTITY, "VALOR_INVALIDO", ex.getMessage());
    }

    @ExceptionHandler(SaldoInsuficienteException.class)
    public ResponseEntity<?> handleSaldoInsuficiente(SaldoInsuficienteException ex) {
        return erro(HttpStatus.UNPROCESSABLE_ENTITY, "SALDO_INSUFICIENTE", ex.getMessage());
    }

    @ExceptionHandler(ContaBloqueadaException.class)
    public ResponseEntity<?> handleContaBloqueada(ContaBloqueadaException ex) {
        return erro(HttpStatus.UNPROCESSABLE_ENTITY, "CONTA_BLOQUEADA", ex.getMessage());
    }

    // ─── UC-03 ────────────────────────────────────────────────────────────────

    @ExceptionHandler(ContasIdenticasException.class)
    public ResponseEntity<?> handleContasIdenticas(ContasIdenticasException ex) {
        return erro(HttpStatus.UNPROCESSABLE_ENTITY, "CONTAS_IDENTICAS", ex.getMessage());
    }

    @ExceptionHandler(ContaDestinoNaoEncontradaException.class)
    public ResponseEntity<?> handleContaDestinoNaoEncontrada(ContaDestinoNaoEncontradaException ex) {
        return erro(HttpStatus.NOT_FOUND, "CONTA_DESTINO_NAO_ENCONTRADA", ex.getMessage());
    }

    @ExceptionHandler(ContaDestinoBloqueadaException.class)
    public ResponseEntity<?> handleContaDestinoBloqueada(ContaDestinoBloqueadaException ex) {
        return erro(HttpStatus.UNPROCESSABLE_ENTITY, "CONTA_DESTINO_BLOQUEADA", ex.getMessage());
    }

    @ExceptionHandler(FalhaTransacaoException.class)
    public ResponseEntity<?> handleFalhaTransacao(FalhaTransacaoException ex) {
        log.error("Falha na transação atômica: {}", ex.getMessage());
        return erro(HttpStatus.INTERNAL_SERVER_ERROR, "FALHA_TRANSACAO", ex.getMessage());
    }

    // ─── UC-04 ────────────────────────────────────────────────────────────────

    @ExceptionHandler(IntervaloInvalidoException.class)
    public ResponseEntity<?> handleIntervaloInvalido(IntervaloInvalidoException ex) {
        return erro(HttpStatus.UNPROCESSABLE_ENTITY, "INTERVALO_INVALIDO", ex.getMessage());
    }

    // ─── UC-05 ────────────────────────────────────────────────────────────────

    @ExceptionHandler(ContaJaBloqueadaException.class)
    public ResponseEntity<?> handleContaJaBloqueada(ContaJaBloqueadaException ex) {
        return erro(HttpStatus.UNPROCESSABLE_ENTITY, "CONTA_JA_BLOQUEADA", ex.getMessage());
    }

    @ExceptionHandler(ContaJaAtivaException.class)
    public ResponseEntity<?> handleContaJaAtiva(ContaJaAtivaException ex) {
        return erro(HttpStatus.UNPROCESSABLE_ENTITY, "CONTA_JA_ATIVA", ex.getMessage());
    }

    @ExceptionHandler(MotivoInvalidoException.class)
    public ResponseEntity<?> handleMotivoInvalido(MotivoInvalidoException ex) {
        return erro(HttpStatus.UNPROCESSABLE_ENTITY, "MOTIVO_INVALIDO", ex.getMessage());
    }

    // ─── Ownership / Acesso ───────────────────────────────────────────────────

    @ExceptionHandler(AcessoNegadoException.class)
    public ResponseEntity<?> handleAcessoNegado(AcessoNegadoException ex) {
        return erro(HttpStatus.FORBIDDEN, "ACESSO_NEGADO", ex.getMessage());
    }

    @ExceptionHandler(org.springframework.security.access.AccessDeniedException.class)
    public ResponseEntity<?> handleSpringAcessoNegado(Exception ex) {
        return erro(HttpStatus.FORBIDDEN, "ACESSO_NEGADO", "Acesso não autorizado.");
    }

    @ExceptionHandler(org.springframework.security.core.AuthenticationException.class)
    public ResponseEntity<?> handleAutenticacao(Exception ex) {
        return erro(HttpStatus.UNAUTHORIZED, "TOKEN_INVALIDO", "Token de autenticação inválido.");
    }

    // ─── Validação de entrada ─────────────────────────────────────────────────

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<?> handleValidacao(MethodArgumentNotValidException ex) {
        String campo = ex.getBindingResult().getFieldErrors().stream()
            .findFirst()
            .map(e -> e.getField())
            .orElse("campo");
        return erro(HttpStatus.BAD_REQUEST, "CAMPO_FALTANTE", "Campo obrigatório ausente ou inválido: " + campo);
    }

    // ─── Fallback de negócio ──────────────────────────────────────────────────

    @ExceptionHandler(BancoxException.class)
    public ResponseEntity<?> handleBancox(BancoxException ex) {
        log.warn("Erro de negócio não mapeado: {}", ex.getClass().getSimpleName());
        return erro(HttpStatus.UNPROCESSABLE_ENTITY, "ERRO_NEGOCIO", ex.getMessage());
    }

    // ─── Fallback global ──────────────────────────────────────────────────────

    @ExceptionHandler(Exception.class)
    public ResponseEntity<?> handleErroInterno(Exception ex) {
        log.error("Erro interno não mapeado", ex);
        return erro(HttpStatus.INTERNAL_SERVER_ERROR, "ERRO_INTERNO", "Ocorreu um erro interno. Tente novamente.");
    }
}
