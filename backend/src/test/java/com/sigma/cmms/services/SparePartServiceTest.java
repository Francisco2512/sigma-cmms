package com.sigma.cmms.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.sigma.cmms.dto.SparePartDtos.SparePartRequest;
import com.sigma.cmms.dto.SparePartDtos.SparePartResponse;
import com.sigma.cmms.exception.ConflictException;
import com.sigma.cmms.exception.NotFoundException;
import com.sigma.cmms.repositories.SparePartRepository;
import com.sigma.cmms.support.TestData;
import java.math.BigDecimal;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SparePartServiceTest {

    @Mock
    private SparePartRepository sparePartRepository;

    @InjectMocks
    private SparePartService service;

    @Test
    void should_rejectCreation_when_skuAlreadyExists() {
        // Arrange
        when(sparePartRepository.existsBySku("ROD-6205")).thenReturn(true);
        SparePartRequest request = new SparePartRequest("ROD-6205", "Rodamiento", "pza", 5, 2, BigDecimal.TEN);

        // Act + Assert
        assertThatThrownBy(() -> service.create(request)).isInstanceOf(ConflictException.class);
        verify(sparePartRepository, never()).save(any());
    }

    @Test
    void should_increaseStockAndClearAlert_when_restocking() {
        // Arrange
        when(sparePartRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(TestData.sparePart(1, 2, 4)));

        // Act
        SparePartResponse updated = service.restock(1L, 10);

        // Assert
        assertThat(updated.stock()).isEqualTo(12);
        assertThat(updated.belowReorderPoint()).isFalse();
    }

    @Test
    void should_throwNotFound_when_restockingUnknownPart() {
        // Arrange
        when(sparePartRepository.findByIdForUpdate(9L)).thenReturn(Optional.empty());

        // Act + Assert
        assertThatThrownBy(() -> service.restock(9L, 1)).isInstanceOf(NotFoundException.class);
    }
}
