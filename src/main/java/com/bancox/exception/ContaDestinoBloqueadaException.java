package com.bancox.exception;

public class ContaDestinoBloqueadaException extends BancoxException {
    public ContaDestinoBloqueadaException() {
        super("Conta de destino está bloqueada e não pode receber transferências.");
    }
}
