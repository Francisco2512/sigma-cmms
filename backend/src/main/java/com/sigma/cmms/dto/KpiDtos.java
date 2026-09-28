package com.sigma.cmms.dto;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/** Indicadores de desempeno del mantenimiento. Los valores nulos significan "sin datos suficientes". */
public final class KpiDtos {

    private KpiDtos() {
    }

    public record AssetKpi(Long assetId, String code, String name, int failures, Double mttrHours,
            Double availabilityPct) {
    }

    public record KpiResponse(int periodDays, LocalDate from, LocalDate to, Double mtbfHours, Double mttrHours,
            Double availabilityPct, Double preventiveCompliancePct, int failures, long openOrders,
            long overdueOrders, long partsBelowReorder, Map<String, Long> ordersByStatus,
            List<AssetKpi> topAssets) {
    }
}
