package com.sigma.cmms.exception;

/** El recurso solicitado no existe (HTTP 404). */
public class NotFoundException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public NotFoundException(String resource, Object id) {
        super(resource + " con id " + id + " no existe");
    }
}
