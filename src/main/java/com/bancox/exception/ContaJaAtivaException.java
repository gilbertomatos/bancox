package com.bancox.exception;

public class ContaJaAtivaException extends BancoxException {
    public ContaJaAtivaException() {
        super("A conta já está ativa.");
    }
}
