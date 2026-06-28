package com.bancox.exception;

public class ContaBloqueadaException extends BancoxException {
    public ContaBloqueadaException() {
        super("Esta operação não pode ser realizada em conta bloqueada.");
    }
}
