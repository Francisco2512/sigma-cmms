package com.sigma.cmms.exception;

/** La operacion choca con el estado actual del recurso: duplicado o estado invalido (HTTP 409). */
public class ConflictException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public ConflictException(String message) {
        super(message);
    }
}
