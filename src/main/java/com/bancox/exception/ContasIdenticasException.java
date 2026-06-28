package com.bancox.exception;

public class ContasIdenticasException extends BancoxException {
    public ContasIdenticasException() {
        super("Conta de origem e destino não podem ser a mesma.");
    }
}
