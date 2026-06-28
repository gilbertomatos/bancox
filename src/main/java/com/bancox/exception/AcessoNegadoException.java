package com.bancox.exception;

public class AcessoNegadoException extends BancoxException {
    public AcessoNegadoException() {
        super("Acesso não autorizado.");
    }
}
