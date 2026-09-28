package com.sigma.cmms.services;

import static com.sigma.cmms.support.TestData.CLOCK;
import static com.sigma.cmms.support.TestData.TODAY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.sigma.cmms.dto.KpiDtos.KpiResponse;
import com.sigma.cmms.model.WorkOrderStatus;
import com.sigma.cmms.model.WorkOrderType;
import com.sigma.cmms.repositories.AssetRepository;
import com.sigma.cmms.repositories.PreventiveDue;
import com.sigma.cmms.repositories.RepairInterval;
import com.sigma.cmms.repositories.SparePartRepository;
import com.sigma.cmms.repositories.StatusCount;
import com.sigma.cmms.repositories.WorkOrderRepository;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class KpiServiceTest {

    @Mock
    private WorkOrderRepository workOrderRepository;
    @Mock
    private AssetRepository assetRepository;
    @Mock
    private SparePartRepository sparePartRepository;

    private KpiService service;

    @BeforeEach
    void setUp() {
        service = new KpiService(workOrderRepository, assetRepository, sparePartRepository, CLOCK);
    }

    private static RepairInterval repair(long assetId, int hours) {
        LocalDateTime failure = TODAY.minusDays(5).atTime(8, 0);
        return new RepairInterval(assetId, "ACT-" + assetId, "Activo " + assetId, failure, failure.plusHours(hours));
    }

    private void stubCounters() {
        when(workOrderRepository.countByStatus()).thenReturn(List.of(
                new StatusCount(WorkOrderStatus.ABIERTA, 2L), new StatusCount(WorkOrderStatus.EN_PROCESO, 1L)));
        when(workOrderRepository.countByStatusInAndDueDateBefore(anyCollection(), eq(TODAY))).thenReturn(1L);
        when(sparePartRepository.countBelowReorderPoint()).thenReturn(3L);
    }

    @Test
    void should_calculateFleetIndicators_when_thereAreRepairs() {
        // Arrange: 2 activos x 30 dias = 1440 h; 2 fallas de 4 y 6 h
        when(workOrderRepository.findRepairsSince(eq(WorkOrderType.CORRECTIVA), eq(WorkOrderStatus.CERRADA), any()))
                .thenReturn(List.of(repair(1, 4), repair(2, 6)));
        when(assetRepository.count()).thenReturn(2L);
        when(workOrderRepository.findPreventiveDue(any(), any(), any(), any())).thenReturn(List.of());
        stubCounters();

        // Act
        KpiResponse kpis = service.compute(30);

        // Assert
        assertThat(kpis.failures()).isEqualTo(2);
        assertThat(kpis.mttrHours()).isEqualTo(5.0);
        assertThat(kpis.mtbfHours()).isEqualTo(715.0);
        assertThat(kpis.availabilityPct()).isEqualTo(99.3);
        assertThat(kpis.preventiveCompliancePct()).isNull();
    }

    @Test
    void should_fillMissingStatusesWithZero_when_countingOrders() {
        // Arrange
        when(workOrderRepository.findRepairsSince(any(), any(), any())).thenReturn(List.of());
        when(assetRepository.count()).thenReturn(5L);
        when(workOrderRepository.findPreventiveDue(any(), any(), any(), any())).thenReturn(List.of());
        stubCounters();

        // Act
        KpiResponse kpis = service.compute(90);

        // Assert
        assertThat(kpis.ordersByStatus()).containsEntry("ABIERTA", 2L).containsEntry("CERRADA", 0L).hasSize(5);
        assertThat(kpis.openOrders()).isEqualTo(3);
        assertThat(kpis.overdueOrders()).isEqualTo(1);
        assertThat(kpis.partsBelowReorder()).isEqualTo(3);
    }

    @Test
    void should_rankAssetsByFailures_when_buildingTopList() {
        // Arrange
        when(workOrderRepository.findRepairsSince(any(), any(), any()))
                .thenReturn(List.of(repair(1, 2), repair(2, 3), repair(2, 5)));
        when(assetRepository.count()).thenReturn(2L);
        when(workOrderRepository.findPreventiveDue(any(), any(), any(), any())).thenReturn(List.of(
                new PreventiveDue(TODAY.minusDays(3), WorkOrderStatus.CERRADA, TODAY.minusDays(3).atTime(10, 0))));
        stubCounters();

        // Act
        KpiResponse kpis = service.compute(30);

        // Assert
        assertThat(kpis.topAssets()).extracting("code").containsExactly("ACT-2", "ACT-1");
        assertThat(kpis.topAssets().get(0).mttrHours()).isEqualTo(4.0);
        assertThat(kpis.preventiveCompliancePct()).isEqualTo(100.0);
    }
}
