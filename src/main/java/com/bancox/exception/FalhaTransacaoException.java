package com.bancox.exception;

public class FalhaTransacaoException extends BancoxException {
    public FalhaTransacaoException() {
        super("Transferência cancelada. Nenhum valor foi movimentado.");
    }
}
