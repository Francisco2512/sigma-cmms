package com.sigma.cmms.util;

import com.sigma.cmms.model.WorkOrderStatus;
import com.sigma.cmms.repositories.PreventiveDue;
import com.sigma.cmms.repositories.RepairInterval;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import lombok.experimental.UtilityClass;

/**
 * Formulas de confiabilidad y mantenibilidad. Funciones puras, sin acceso a datos.
 *
 * <ul>
 *   <li>MTTR = tiempo total de reparacion / numero de reparaciones</li>
 *   <li>MTBF = tiempo operativo / numero de fallas</li>
 *   <li>Disponibilidad inherente = MTBF / (MTBF + MTTR)</li>
 * </ul>
 * Un resultado {@code null} indica que no hay datos para calcular el indicador.
 */
@UtilityClass
public class KpiCalculator {

    private static final double MINUTES_PER_HOUR = 60.0;
    private static final double FULL = 100.0;

    public static double repairHours(RepairInterval repair) {
        return hoursBetween(repair.failureAt(), repair.closedAt());
    }

    public static double hoursBetween(LocalDateTime start, LocalDateTime end) {
        if (start == null || end == null || end.isBefore(start)) {
            return 0.0;
        }
        return Duration.between(start, end).toMinutes() / MINUTES_PER_HOUR;
    }

    public static double totalRepairHours(List<RepairInterval> repairs) {
        return repairs.stream().mapToDouble(KpiCalculator::repairHours).sum();
    }

    public static Double mttr(List<RepairInterval> repairs) {
        if (repairs.isEmpty()) {
            return null;
        }
        return round1(totalRepairHours(repairs) / repairs.size());
    }

    /**
     * @param periodHours horas del periodo multiplicadas por el numero de activos observados
     */
    public static Double mtbf(List<RepairInterval> repairs, double periodHours) {
        if (repairs.isEmpty()) {
            return null;
        }
        double operatingHours = Math.max(periodHours - totalRepairHours(repairs), 0.0);
        return round1(operatingHours / repairs.size());
    }

    public static Double availability(Double mtbf, Double mttr) {
        if (mtbf == null || mttr == null) {
            return FULL;
        }
        if (mtbf + mttr == 0.0) {
            return 0.0;
        }
        return round1(mtbf / (mtbf + mttr) * FULL);
    }

    /** Porcentaje de preventivos vencidos en el periodo que se cerraron a mas tardar en su fecha. */
    public static Double preventiveCompliance(List<PreventiveDue> dues) {
        if (dues.isEmpty()) {
            return null;
        }
        long onTime = dues.stream().filter(KpiCalculator::closedOnTime).count();
        return round1(onTime * FULL / dues.size());
    }

    public static boolean closedOnTime(PreventiveDue due) {
        return due.status() == WorkOrderStatus.CERRADA && due.closedAt() != null
                && !due.closedAt().toLocalDate().isAfter(due.dueDate());
    }

    public static double round1(double value) {
        return Math.round(value * 10.0) / 10.0;
    }
}
