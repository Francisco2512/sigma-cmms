package com.sigma.cmms.services;

import com.sigma.cmms.dto.KpiDtos.AssetKpi;
import com.sigma.cmms.dto.KpiDtos.KpiResponse;
import com.sigma.cmms.model.WorkOrderStatus;
import com.sigma.cmms.model.WorkOrderType;
import com.sigma.cmms.repositories.AssetRepository;
import com.sigma.cmms.repositories.PreventiveDue;
import com.sigma.cmms.repositories.RepairInterval;
import com.sigma.cmms.repositories.SparePartRepository;
import com.sigma.cmms.repositories.StatusCount;
import com.sigma.cmms.repositories.WorkOrderRepository;
import com.sigma.cmms.util.KpiCalculator;
import java.time.Clock;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Tablero de indicadores del area de mantenimiento (RF-08). */
@Service
@RequiredArgsConstructor
public class KpiService {

    private static final int TOP_ASSETS = 5;
    private static final double HOURS_PER_DAY = 24.0;

    private final WorkOrderRepository workOrderRepository;
    private final AssetRepository assetRepository;
    private final SparePartRepository sparePartRepository;
    private final Clock clock;

    /**
     * Calcula los indicadores de los ultimos {@code days} dias.
     *
     * @param days tamano de la ventana de observacion
     * @return indicadores globales y los activos con mas fallas
     */
    @Transactional(readOnly = true)
    public KpiResponse compute(int days) {
        LocalDate today = LocalDate.now(clock);
        LocalDate from = today.minusDays(days);
        double periodHours = days * HOURS_PER_DAY;

        List<RepairInterval> repairs = workOrderRepository.findRepairsSince(
                WorkOrderType.CORRECTIVA, WorkOrderStatus.CERRADA, from.atStartOfDay());
        long assets = Math.max(assetRepository.count(), 1);
        Double mttr = KpiCalculator.mttr(repairs);
        Double mtbf = KpiCalculator.mtbf(repairs, periodHours * assets);

        List<PreventiveDue> dues = workOrderRepository.findPreventiveDue(
                WorkOrderType.PREVENTIVA, WorkOrderStatus.CANCELADA, from, today);

        Map<String, Long> byStatus = ordersByStatus();
        long open = WorkOrderStatus.pending().stream().mapToLong(s -> byStatus.get(s.name())).sum();

        return new KpiResponse(days, from, today, mtbf, mttr, KpiCalculator.availability(mtbf, mttr),
                KpiCalculator.preventiveCompliance(dues), repairs.size(), open,
                workOrderRepository.countByStatusInAndDueDateBefore(WorkOrderStatus.pending(), today),
                sparePartRepository.countBelowReorderPoint(), byStatus, topAssets(repairs, periodHours));
    }

    private Map<String, Long> ordersByStatus() {
        Map<WorkOrderStatus, Long> counts = new EnumMap<>(WorkOrderStatus.class);
        for (WorkOrderStatus status : WorkOrderStatus.values()) {
            counts.put(status, 0L);
        }
        for (StatusCount row : workOrderRepository.countByStatus()) {
            counts.put(row.status(), row.total());
        }
        Map<String, Long> result = new LinkedHashMap<>();
        counts.forEach((status, total) -> result.put(status.name(), total));
        return result;
    }

    private static List<AssetKpi> topAssets(List<RepairInterval> repairs, double periodHours) {
        Map<Long, List<RepairInterval>> byAsset = repairs.stream()
                .collect(Collectors.groupingBy(RepairInterval::assetId));
        return byAsset.values().stream()
                .map(list -> {
                    RepairInterval first = list.get(0);
                    Double mttr = KpiCalculator.mttr(list);
                    Double mtbf = KpiCalculator.mtbf(list, periodHours);
                    return new AssetKpi(first.assetId(), first.assetCode(), first.assetName(), list.size(),
                            mttr, KpiCalculator.availability(mtbf, mttr));
                })
                .sorted(Comparator.comparingInt(AssetKpi::failures).reversed()
                        .thenComparing(AssetKpi::code))
                .limit(TOP_ASSETS)
                .toList();
    }
}
