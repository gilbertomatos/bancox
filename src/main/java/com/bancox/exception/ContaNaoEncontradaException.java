package com.bancox.exception;

public class ContaNaoEncontradaException extends BancoxException {
    public ContaNaoEncontradaException() {
        super("Conta informada não existe.");
    }
}
