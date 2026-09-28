package com.sigma.cmms.repositories;

import com.sigma.cmms.model.WorkOrderStatus;

/** Conteo de ordenes por estatus. */
public record StatusCount(WorkOrderStatus status, Long total) {
}
