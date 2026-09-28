package com.sigma.cmms.model;

import static com.sigma.cmms.support.TestData.TODAY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.sigma.cmms.exception.InvalidStateTransitionException;
import com.sigma.cmms.support.TestData;
import java.math.BigDecimal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class WorkOrderTest {

    private User technician;
    private WorkOrder order;

    @BeforeEach
    void setUp() {
        technician = TestData.user(5, Role.TECNICO);
        order = TestData.workOrder(1, TestData.asset(1));
    }

    @Test
    void should_changeToAssigned_when_openOrderIsAssigned() {
        // Act
        order.assignTo(technician);

        // Assert
        assertThat(order.getStatus()).isEqualTo(WorkOrderStatus.ASIGNADA);
        assertThat(order.isAssignedTo(5L)).isTrue();
    }

    @Test
    void should_rejectStart_when_orderIsNotAssigned() {
        // Act + Assert
        assertThatThrownBy(() -> order.start(TODAY.atTime(8, 0)))
                .isInstanceOf(InvalidStateTransitionException.class)
                .hasMessageContaining("ABIERTA");
    }

    @Test
    void should_putAssetInMaintenance_when_orderStarts() {
        // Arrange
        order.assignTo(technician);

        // Act
        order.start(TODAY.atTime(8, 0));

        // Assert
        assertThat(order.getStatus()).isEqualTo(WorkOrderStatus.EN_PROCESO);
        assertThat(order.getAsset().getStatus()).isEqualTo(AssetStatus.EN_MANTENIMIENTO);
    }

    @Test
    void should_restoreAssetToOperative_when_orderCloses() {
        // Arrange
        order.assignTo(technician);
        order.start(TODAY.atTime(8, 0));

        // Act
        order.close(TODAY.atTime(11, 0), new BigDecimal("2.50"), "Se cambio el rodamiento");

        // Assert
        assertThat(order.getStatus()).isEqualTo(WorkOrderStatus.CERRADA);
        assertThat(order.getAsset().getStatus()).isEqualTo(AssetStatus.OPERATIVO);
        assertThat(order.getClosedAt()).isEqualTo(TODAY.atTime(11, 0));
    }

    @Test
    void should_rejectClose_when_orderIsNotInProgress() {
        // Arrange
        order.assignTo(technician);

        // Act + Assert
        assertThatThrownBy(() -> order.close(TODAY.atTime(11, 0), BigDecimal.ONE, "notas"))
                .isInstanceOf(InvalidStateTransitionException.class);
    }

    @Test
    void should_rejectCancel_when_orderIsInProgress() {
        // Arrange
        order.assignTo(technician);
        order.start(TODAY.atTime(8, 0));

        // Act + Assert
        assertThatThrownBy(() -> order.cancel("duplicada"))
                .isInstanceOf(InvalidStateTransitionException.class);
    }

    @Test
    void should_beOverdue_when_pendingAndDueDateHasPassed() {
        // Arrange
        order.setDueDate(TODAY.minusDays(1));

        // Act + Assert
        assertThat(order.isOverdue(TODAY)).isTrue();
    }

    @Test
    void should_notBeOverdue_when_orderIsCancelled() {
        // Arrange
        order.setDueDate(TODAY.minusDays(1));
        order.cancel("Reporte duplicado");

        // Act + Assert
        assertThat(order.isOverdue(TODAY)).isFalse();
    }

    @Test
    void should_sumPartsCost_when_partsAreAdded() {
        // Arrange
        order.addPart(TestData.sparePart(1, 10, 2), 2);
        order.addPart(TestData.sparePart(2, 10, 2), 1);

        // Act
        BigDecimal cost = order.partsCost();

        // Assert
        assertThat(cost).isEqualByComparingTo("300.00");
    }
}
