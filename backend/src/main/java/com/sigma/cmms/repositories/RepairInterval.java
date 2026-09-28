package com.sigma.cmms.repositories;

import java.time.LocalDateTime;

/** Proyeccion de una reparacion correctiva cerrada: desde la falla hasta el cierre. */
public record RepairInterval(Long assetId, String assetCode, String assetName, LocalDateTime failureAt,
        LocalDateTime closedAt) {
}
