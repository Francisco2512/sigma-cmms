package com.sigma.cmms.security;

/** Expresiones de autorizacion reutilizadas en {@code @PreAuthorize}. */
public final class Authz {

    /** Planear: activos, planes preventivos, asignacion de ordenes. */
    public static final String CAN_PLAN = "hasAnyRole('ADMIN','JEFE_MANTENIMIENTO','PLANIFICADOR')";
    /** Supervisar: cancelar ordenes, dar de baja activos. */
    public static final String SUPERVISOR = "hasAnyRole('ADMIN','JEFE_MANTENIMIENTO')";
    /** Ejecutar ordenes en campo. */
    public static final String CAN_EXECUTE = "hasAnyRole('ADMIN','JEFE_MANTENIMIENTO','TECNICO')";
    /** Levantar ordenes (el tecnico reporta fallas). */
    public static final String CAN_REPORT = "hasAnyRole('ADMIN','JEFE_MANTENIMIENTO','PLANIFICADOR','TECNICO')";
    /** Movimientos de inventario. */
    public static final String CAN_STOCK = "hasAnyRole('ADMIN','ALMACENISTA')";

    private Authz() {
    }
}
