package com.sigma.cmms.exception;

/** Transicion no permitida en el ciclo de vida de una orden de trabajo. */
public class InvalidStateTransitionException extends ConflictException {

    private static final long serialVersionUID = 1L;

    public InvalidStateTransitionException(String message) {
        super(message);
    }
}
