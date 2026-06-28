package com.bancox.exception;

/**
 * Base de todas as exceptions de negócio do BancoX.
 * Toda exception de negócio deve estender esta classe (LSP — DA-17).
 * GlobalExceptionHandler captura esta classe como fallback de negócio.
 *
 * Hierarquia:
 *   BancoxException
 *     ├── ContaNaoEncontradaException
 *     ├── ValorInvalidoException
 *     ├── SaldoInsuficienteException
 *     └── (adicionar conforme cada UC for implementado)
 */
public abstract class BancoxException extends RuntimeException {

    protected BancoxException(String mensagem) {
        super(mensagem);
    }
}
