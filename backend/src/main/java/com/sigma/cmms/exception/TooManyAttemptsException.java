package com.sigma.cmms.exception;

/** Usuario bloqueado temporalmente por intentos fallidos de inicio de sesion (HTTP 429). */
public class TooManyAttemptsException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public TooManyAttemptsException(long minutes) {
        super("Cuenta bloqueada temporalmente. Intente de nuevo en " + minutes + " minutos");
    }
}
