package com.sigma.cmms.model;

import java.util.EnumSet;
import java.util.Set;

/** Ciclo de vida de una orden: ABIERTA -> ASIGNADA -> EN_PROCESO -> CERRADA, o CANCELADA. */
public enum WorkOrderStatus {
    ABIERTA, ASIGNADA, EN_PROCESO, CERRADA, CANCELADA;

    /** Estatus que todavia requieren trabajo. */
    public static Set<WorkOrderStatus> pending() {
        return EnumSet.of(ABIERTA, ASIGNADA, EN_PROCESO);
    }
}
