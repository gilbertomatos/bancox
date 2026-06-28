package com.bancox.exception;

public class IntervaloInvalidoException extends BancoxException {
    public IntervaloInvalidoException() {
        super("A data de início deve ser anterior à data de fim.");
    }
}
