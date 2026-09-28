package com.sigma.cmms.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.sigma.cmms.exception.BusinessRuleException;
import com.sigma.cmms.support.TestData;
import org.junit.jupiter.api.Test;

class SparePartTest {

    @Test
    void should_decreaseStock_when_quantityIsAvailable() {
        // Arrange
        SparePart part = TestData.sparePart(1, 10, 3);

        // Act
        part.consume(4);

        // Assert
        assertThat(part.getStock()).isEqualTo(6);
    }

    @Test
    void should_rejectConsumption_when_stockIsInsufficient() {
        // Arrange
        SparePart part = TestData.sparePart(1, 2, 1);

        // Act + Assert
        assertThatThrownBy(() -> part.consume(3))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("disponible 2");
        assertThat(part.getStock()).isEqualTo(2);
    }

    @Test
    void should_flagReorder_when_stockEqualsReorderPoint() {
        // Arrange
        SparePart part = TestData.sparePart(1, 4, 4);

        // Act + Assert
        assertThat(part.isBelowReorderPoint()).isTrue();
    }

    @Test
    void should_notFlagReorder_when_stockIsAboveReorderPoint() {
        // Arrange
        SparePart part = TestData.sparePart(1, 5, 4);

        // Act + Assert
        assertThat(part.isBelowReorderPoint()).isFalse();
    }
}
