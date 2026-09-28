package com.sigma.cmms.services;

import static com.sigma.cmms.support.TestData.CLOCK;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.sigma.cmms.dto.AssetDtos.AssetRequest;
import com.sigma.cmms.dto.AssetDtos.AssetResponse;
import com.sigma.cmms.dto.PageResponse;
import com.sigma.cmms.exception.BusinessRuleException;
import com.sigma.cmms.exception.ConflictException;
import com.sigma.cmms.exception.NotFoundException;
import com.sigma.cmms.model.Asset;
import com.sigma.cmms.model.Criticality;
import com.sigma.cmms.model.WorkOrderStatus;
import com.sigma.cmms.repositories.AssetRepository;
import com.sigma.cmms.repositories.WorkOrderRepository;
import com.sigma.cmms.support.TestData;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;

@ExtendWith(MockitoExtension.class)
class AssetServiceTest {

    @Mock
    private AssetRepository assetRepository;
    @Mock
    private WorkOrderRepository workOrderRepository;

    private AssetService service;

    @BeforeEach
    void setUp() {
        service = new AssetService(assetRepository, workOrderRepository, CLOCK);
    }

    private static AssetRequest request(String code) {
        return new AssetRequest(code, "Compresor", "Utilidades", null, Criticality.ALTA, null, null, null, null);
    }

    @Test
    void should_rejectCreation_when_codeAlreadyExists() {
        // Arrange
        when(assetRepository.existsByCode("CMP-001")).thenReturn(true);
        AssetRequest request = request("CMP-001");

        // Act + Assert
        assertThatThrownBy(() -> service.create(request)).isInstanceOf(ConflictException.class);
        verify(assetRepository, never()).save(any());
    }

    @Test
    void should_createOperativeAsset_when_statusIsOmitted() {
        // Arrange
        when(assetRepository.existsByCode("CMP-009")).thenReturn(false);
        when(assetRepository.save(any(Asset.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        var created = service.create(request("CMP-009"));

        // Assert
        assertThat(created.code()).isEqualTo("CMP-009");
        assertThat(created.status()).hasToString("OPERATIVO");
    }

    @Test
    void should_rejectDeletion_when_assetHasPendingOrders() {
        // Arrange
        when(assetRepository.findById(1L)).thenReturn(Optional.of(TestData.asset(1)));
        when(workOrderRepository.existsByAssetIdAndStatusIn(1L, WorkOrderStatus.pending())).thenReturn(true);

        // Act + Assert
        assertThatThrownBy(() -> service.delete(1L)).isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void should_markDeletionTime_when_assetHasNoPendingOrders() {
        // Arrange
        Asset asset = TestData.asset(1);
        when(assetRepository.findById(1L)).thenReturn(Optional.of(asset));
        when(workOrderRepository.existsByAssetIdAndStatusIn(1L, WorkOrderStatus.pending())).thenReturn(false);

        // Act
        service.delete(1L);

        // Assert
        assertThat(asset.getDeletedAt()).isEqualTo(LocalDateTime.now(CLOCK));
    }

    @Test
    void should_throwNotFound_when_assetDoesNotExist() {
        // Arrange
        when(assetRepository.findById(9L)).thenReturn(Optional.empty());

        // Act + Assert
        assertThatThrownBy(() -> service.get(9L)).isInstanceOf(NotFoundException.class);
    }

    @Test
    void should_rejectUpdate_when_codeBelongsToAnotherAsset() {
        // Arrange
        when(assetRepository.findById(1L)).thenReturn(Optional.of(TestData.asset(1)));
        when(assetRepository.existsByCodeAndIdNot("ACT-2", 1L)).thenReturn(true);
        AssetRequest request = request("ACT-2");

        // Act + Assert
        assertThatThrownBy(() -> service.update(1L, request)).isInstanceOf(ConflictException.class);
    }

    @Test
    void should_applyNewData_when_updatingAsset() {
        // Arrange
        when(assetRepository.findById(1L)).thenReturn(Optional.of(TestData.asset(1)));
        when(assetRepository.existsByCodeAndIdNot("ACT-1", 1L)).thenReturn(false);

        // Act
        AssetResponse updated = service.update(1L, request("ACT-1"));

        // Assert
        assertThat(updated.name()).isEqualTo("Compresor");
    }

    @Test
    void should_mapAssets_when_searching() {
        // Arrange
        when(assetRepository.findAll(org.mockito.ArgumentMatchers.<Specification<Asset>>any(),
                any(PageRequest.class))).thenReturn(new PageImpl<>(List.of(TestData.asset(1), TestData.asset(2))));

        // Act
        PageResponse<AssetResponse> page = service.search("act", null, null, PageRequest.of(0, 20));

        // Assert
        assertThat(page.content()).extracting(AssetResponse::code).containsExactly("ACT-1", "ACT-2");
    }

    @Test
    void should_returnHistory_when_assetExists() {
        // Arrange
        when(assetRepository.findById(1L)).thenReturn(Optional.of(TestData.asset(1)));
        when(workOrderRepository.findByAssetIdOrderByCreatedAtDesc(1L, PageRequest.of(0, 10)))
                .thenReturn(new PageImpl<>(List.of(TestData.workOrder(5, TestData.asset(1)))));

        // Act
        var history = service.history(1L, PageRequest.of(0, 10));

        // Assert
        assertThat(history.content()).singleElement().satisfies(order -> assertThat(order.id()).isEqualTo(5L));
    }
}
