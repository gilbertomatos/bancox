package com.bancox.exception;

public class SaldoInsuficienteException extends BancoxException {
    public SaldoInsuficienteException() {
        super("Saldo disponível insuficiente para esta operação.");
    }
}
