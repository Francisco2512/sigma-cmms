package com.sigma.cmms.util;

import static com.sigma.cmms.support.TestData.TODAY;
import static org.assertj.core.api.Assertions.assertThat;

import com.sigma.cmms.model.WorkOrderStatus;
import com.sigma.cmms.repositories.PreventiveDue;
import com.sigma.cmms.repositories.RepairInterval;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;

class KpiCalculatorTest {

    private static RepairInterval repair(int hours) {
        LocalDateTime failure = TODAY.atTime(6, 0);
        return new RepairInterval(1L, "ACT-1", "Activo 1", failure, failure.plusHours(hours));
    }

    @Test
    void should_averageRepairTime_when_calculatingMttr() {
        // Arrange
        List<RepairInterval> repairs = List.of(repair(2), repair(4), repair(6));

        // Act
        Double mttr = KpiCalculator.mttr(repairs);

        // Assert
        assertThat(mttr).isEqualTo(4.0);
    }

    @Test
    void should_returnNull_when_thereAreNoRepairs() {
        // Act + Assert
        assertThat(KpiCalculator.mttr(List.of())).isNull();
        assertThat(KpiCalculator.mtbf(List.of(), 720)).isNull();
    }

    @Test
    void should_discountDowntime_when_calculatingMtbf() {
        // Arrange: 720 h observadas, 2 fallas de 10 h -> 700 h operativas / 2
        List<RepairInterval> repairs = List.of(repair(10), repair(10));

        // Act
        Double mtbf = KpiCalculator.mtbf(repairs, 720);

        // Assert
        assertThat(mtbf).isEqualTo(350.0);
    }

    @Test
    void should_combineMtbfAndMttr_when_calculatingAvailability() {
        // Act
        Double availability = KpiCalculator.availability(95.0, 5.0);

        // Assert
        assertThat(availability).isEqualTo(95.0);
    }

    @Test
    void should_reportFullAvailability_when_thereAreNoFailures() {
        // Act + Assert
        assertThat(KpiCalculator.availability(null, null)).isEqualTo(100.0);
    }

    @Test
    void should_countOnlyOnTimeClosures_when_calculatingPreventiveCompliance() {
        // Arrange
        List<PreventiveDue> dues = List.of(
                new PreventiveDue(TODAY.minusDays(10), WorkOrderStatus.CERRADA, TODAY.minusDays(10).atTime(12, 0)),
                new PreventiveDue(TODAY.minusDays(8), WorkOrderStatus.CERRADA, TODAY.minusDays(6).atTime(9, 0)),
                new PreventiveDue(TODAY.minusDays(5), WorkOrderStatus.ASIGNADA, null),
                new PreventiveDue(TODAY.minusDays(2), WorkOrderStatus.CERRADA, TODAY.minusDays(3).atTime(9, 0)));

        // Act
        Double compliance = KpiCalculator.preventiveCompliance(dues);

        // Assert: 2 de 4 cerradas a tiempo
        assertThat(compliance).isEqualTo(50.0);
    }

    @Test
    void should_returnZeroHours_when_closureIsBeforeFailure() {
        // Act
        double hours = KpiCalculator.hoursBetween(TODAY.atTime(10, 0), TODAY.atTime(9, 0));

        // Assert
        assertThat(hours).isZero();
    }
}
