package com.sigma.cmms.model;

import static com.sigma.cmms.support.TestData.TODAY;
import static org.assertj.core.api.Assertions.assertThat;

import com.sigma.cmms.support.TestData;
import org.junit.jupiter.api.Test;

class PreventivePlanTest {

    @Test
    void should_moveToNextCycle_when_planIsDueToday() {
        // Arrange
        PreventivePlan plan = TestData.plan(1, TODAY, 30);

        // Act
        int skipped = plan.advancePast(TODAY);

        // Assert
        assertThat(plan.getNextDueDate()).isEqualTo(TODAY.plusDays(30));
        assertThat(skipped).isZero();
    }

    @Test
    void should_skipMissedCycles_when_planIsSeveralCyclesLate() {
        // Arrange: vencido hace 25 dias con frecuencia de 10 -> cubre 3 ciclos, se omiten 2
        PreventivePlan plan = TestData.plan(1, TODAY.minusDays(25), 10);

        // Act
        int skipped = plan.advancePast(TODAY);

        // Assert
        assertThat(plan.getNextDueDate()).isEqualTo(TODAY.plusDays(5));
        assertThat(skipped).isEqualTo(2);
    }

    @Test
    void should_notBeDue_when_planIsPaused() {
        // Arrange
        PreventivePlan plan = TestData.plan(1, TODAY.minusDays(1), 15);
        plan.setStatus(PlanStatus.PAUSADO);

        // Act + Assert
        assertThat(plan.isDue(TODAY)).isFalse();
    }
}
