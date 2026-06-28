package com.bancox.exception;

public class MotivoInvalidoException extends BancoxException {
    public MotivoInvalidoException() {
        super("O motivo é obrigatório e deve ter no máximo 255 caracteres.");
    }
}
