package com.bancox.exception;

public class ContaJaBloqueadaException extends BancoxException {
    public ContaJaBloqueadaException() {
        super("A conta já está bloqueada.");
    }
}
