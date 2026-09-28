package com.sigma.cmms.exception;

/** El usuario autenticado no puede operar sobre este recurso concreto (HTTP 403). */
public class ForbiddenOperationException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public ForbiddenOperationException(String message) {
        super(message);
    }
}
