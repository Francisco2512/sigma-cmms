package com.sigma.cmms.repositories;

import com.sigma.cmms.model.WorkOrderStatus;
import java.time.LocalDate;
import java.time.LocalDateTime;

/** Proyeccion de una orden preventiva con vencimiento en el periodo evaluado. */
public record PreventiveDue(LocalDate dueDate, WorkOrderStatus status, LocalDateTime closedAt) {
}
