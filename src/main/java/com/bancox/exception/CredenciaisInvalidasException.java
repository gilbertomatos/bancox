package com.bancox.exception;

public class CredenciaisInvalidasException extends BancoxException {
    public CredenciaisInvalidasException() {
        super("CPF ou senha incorretos.");
    }
}
