package com.bancox.exception;

public class ContaDestinoNaoEncontradaException extends BancoxException {
    public ContaDestinoNaoEncontradaException() {
        super("Conta de destino não encontrada.");
    }
}
