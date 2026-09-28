package com.sigma.cmms.exception;

/** La solicitud es valida en forma pero viola una regla de negocio (HTTP 422). */
public class BusinessRuleException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public BusinessRuleException(String message) {
        super(message);
    }
}
