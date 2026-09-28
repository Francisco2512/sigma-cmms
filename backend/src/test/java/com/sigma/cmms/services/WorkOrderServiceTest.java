package com.sigma.cmms.services;

import static com.sigma.cmms.support.TestData.CLOCK;
import static com.sigma.cmms.support.TestData.TODAY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.sigma.cmms.dto.PageResponse;
import com.sigma.cmms.dto.WorkOrderDtos.CloseRequest;
import com.sigma.cmms.dto.WorkOrderDtos.PartUsage;
import com.sigma.cmms.dto.WorkOrderDtos.WorkOrderCreateRequest;
import com.sigma.cmms.dto.WorkOrderDtos.WorkOrderDetail;
import com.sigma.cmms.dto.WorkOrderDtos.WorkOrderSummary;
import com.sigma.cmms.exception.BusinessRuleException;
import com.sigma.cmms.exception.ForbiddenOperationException;
import com.sigma.cmms.exception.InvalidStateTransitionException;
import com.sigma.cmms.exception.NotFoundException;
import com.sigma.cmms.model.Priority;
import com.sigma.cmms.model.Role;
import com.sigma.cmms.model.SparePart;
import com.sigma.cmms.model.User;
import com.sigma.cmms.model.WorkOrder;
import com.sigma.cmms.model.WorkOrderStatus;
import com.sigma.cmms.model.WorkOrderType;
import com.sigma.cmms.repositories.SparePartRepository;
import com.sigma.cmms.repositories.UserRepository;
import com.sigma.cmms.repositories.WorkOrderRepository;
import com.sigma.cmms.security.CurrentUser;
import com.sigma.cmms.support.TestData;
import java.math.BigDecimal;
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
class WorkOrderServiceTest {

    @Mock
    private WorkOrderRepository workOrderRepository;
    @Mock
    private SparePartRepository sparePartRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private AssetService assetService;
    @Mock
    private UserService userService;

    private WorkOrderService service;
    private final CurrentUser chief = TestData.actor(2, Role.JEFE_MANTENIMIENTO);
    private final User technician = TestData.user(5, Role.TECNICO);

    @BeforeEach
    void setUp() {
        service = new WorkOrderService(workOrderRepository, sparePartRepository, userRepository, assetService,
                userService, CLOCK);
    }

    private void stubSave() {
        when(workOrderRepository.save(any(WorkOrder.class))).thenAnswer(invocation -> {
            WorkOrder order = invocation.getArgument(0);
            order.setId(10L);
            return order;
        });
    }

    private static WorkOrderCreateRequest correctiveRequest(Long assignedToId) {
        return new WorkOrderCreateRequest(1L, WorkOrderType.CORRECTIVA, Priority.ALTA, "Fuga de aceite", null,
                TODAY, assignedToId, null);
    }

    @Test
    void should_useCurrentTimeAsFailureTime_when_correctiveOrderOmitsIt() {
        // Arrange
        when(assetService.findEntity(1L)).thenReturn(TestData.asset(1));
        when(userRepository.getReferenceById(2L)).thenReturn(TestData.user(2, Role.JEFE_MANTENIMIENTO));
        stubSave();

        // Act
        WorkOrderDetail created = service.create(correctiveRequest(null), chief);

        // Assert
        assertThat(created.failureAt()).isEqualTo(LocalDateTime.now(CLOCK));
        assertThat(created.summary().status()).isEqualTo(WorkOrderStatus.ABIERTA);
    }

    @Test
    void should_buildCodeFromYearAndId_when_orderIsSaved() {
        // Arrange
        when(assetService.findEntity(1L)).thenReturn(TestData.asset(1));
        when(userRepository.getReferenceById(2L)).thenReturn(TestData.user(2, Role.JEFE_MANTENIMIENTO));
        stubSave();

        // Act
        WorkOrderDetail created = service.create(correctiveRequest(null), chief);

        // Assert
        assertThat(created.summary().code()).isEqualTo("OT-2026-00010");
    }

    @Test
    void should_assignTechnician_when_requestIncludesAssignee() {
        // Arrange
        when(assetService.findEntity(1L)).thenReturn(TestData.asset(1));
        when(userRepository.getReferenceById(2L)).thenReturn(TestData.user(2, Role.JEFE_MANTENIMIENTO));
        when(userService.findTechnician(5L)).thenReturn(technician);
        stubSave();

        // Act
        WorkOrderDetail created = service.create(correctiveRequest(5L), chief);

        // Assert
        assertThat(created.summary().status()).isEqualTo(WorkOrderStatus.ASIGNADA);
        assertThat(created.summary().assignedToId()).isEqualTo(5L);
    }

    @Test
    void should_forbidPreventiveCreation_when_actorIsTechnician() {
        // Arrange
        WorkOrderCreateRequest request = new WorkOrderCreateRequest(1L, WorkOrderType.PREVENTIVA, Priority.BAJA,
                "Rutina", null, TODAY, null, null);

        // Act + Assert
        assertThatThrownBy(() -> service.create(request, TestData.actor(5, Role.TECNICO)))
                .isInstanceOf(ForbiddenOperationException.class);
        verify(workOrderRepository, never()).save(any());
    }

    @Test
    void should_forbidStart_when_technicianIsNotTheAssignee() {
        // Arrange
        WorkOrder order = TestData.assignedOrder(1, technician);
        when(workOrderRepository.findWithDetailsById(1L)).thenReturn(Optional.of(order));

        // Act + Assert
        assertThatThrownBy(() -> service.start(1L, TestData.actor(6, Role.TECNICO)))
                .isInstanceOf(ForbiddenOperationException.class);
        assertThat(order.getStatus()).isEqualTo(WorkOrderStatus.ASIGNADA);
    }

    @Test
    void should_mergeRepeatedParts_when_orderIsClosed() {
        // Arrange
        WorkOrder order = TestData.inProgressOrder(1, technician);
        SparePart bearing = TestData.sparePart(7, 10, 2);
        when(workOrderRepository.findWithDetailsById(1L)).thenReturn(Optional.of(order));
        when(sparePartRepository.findByIdForUpdate(7L)).thenReturn(Optional.of(bearing));
        CloseRequest request = new CloseRequest(new BigDecimal("1.50"), "Cambio de rodamiento",
                List.of(new PartUsage(7L, 1), new PartUsage(7L, 2)));

        // Act
        WorkOrderDetail closed = service.close(1L, request, TestData.actor(5, Role.TECNICO));

        // Assert
        assertThat(bearing.getStock()).isEqualTo(7);
        assertThat(closed.parts()).singleElement().satisfies(line -> assertThat(line.quantity()).isEqualTo(3));
        assertThat(closed.summary().status()).isEqualTo(WorkOrderStatus.CERRADA);
    }

    @Test
    void should_rejectClose_when_partStockIsInsufficient() {
        // Arrange
        WorkOrder order = TestData.inProgressOrder(1, technician);
        when(workOrderRepository.findWithDetailsById(1L)).thenReturn(Optional.of(order));
        when(sparePartRepository.findByIdForUpdate(7L)).thenReturn(Optional.of(TestData.sparePart(7, 1, 1)));
        CloseRequest request = new CloseRequest(BigDecimal.ONE, "Cambio", List.of(new PartUsage(7L, 2)));
        CurrentUser actor = TestData.actor(5, Role.TECNICO);

        // Act + Assert
        assertThatThrownBy(() -> service.close(1L, request, actor))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("Existencia insuficiente");
    }

    @Test
    void should_throwNotFound_when_orderDoesNotExist() {
        // Arrange
        when(workOrderRepository.findWithDetailsById(404L)).thenReturn(Optional.empty());

        // Act + Assert
        assertThatThrownBy(() -> service.get(404L))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("404");
    }

    @Test
    void should_reassignTechnician_when_orderIsAssigned() {
        // Arrange
        WorkOrder order = TestData.assignedOrder(1, technician);
        User other = TestData.user(6, Role.TECNICO);
        when(workOrderRepository.findWithDetailsById(1L)).thenReturn(Optional.of(order));
        when(userService.findTechnician(6L)).thenReturn(other);

        // Act
        WorkOrderDetail assigned = service.assign(1L, 6L);

        // Assert
        assertThat(assigned.summary().assignedToId()).isEqualTo(6L);
    }

    @Test
    void should_keepReason_when_orderIsCancelled() {
        // Arrange
        WorkOrder order = TestData.workOrder(1, TestData.asset(1));
        when(workOrderRepository.findWithDetailsById(1L)).thenReturn(Optional.of(order));

        // Act
        WorkOrderDetail cancelled = service.cancel(1L, "Reporte duplicado", chief);

        // Assert
        assertThat(cancelled.summary().status()).isEqualTo(WorkOrderStatus.CANCELADA);
        assertThat(cancelled.resolutionNotes()).isEqualTo("Cancelada: Reporte duplicado");
    }

    @Test
    void should_rejectCancel_when_orderAlreadyStarted() {
        // Arrange
        when(workOrderRepository.findWithDetailsById(1L))
                .thenReturn(Optional.of(TestData.inProgressOrder(1, technician)));

        // Act + Assert
        assertThatThrownBy(() -> service.cancel(1L, "motivo", chief))
                .isInstanceOf(InvalidStateTransitionException.class);
    }

    @Test
    void should_markOverdueOrders_when_listing() {
        // Arrange
        WorkOrder late = TestData.workOrder(1, TestData.asset(1));
        late.setDueDate(TODAY.minusDays(2));
        when(workOrderRepository.findAll(org.mockito.ArgumentMatchers.<Specification<WorkOrder>>any(),
                any(PageRequest.class))).thenReturn(new PageImpl<>(List.of(late)));

        // Act
        PageResponse<WorkOrderSummary> page = service.list(null, null, null, true, chief, PageRequest.of(0, 20));

        // Assert
        assertThat(page.content()).singleElement().satisfies(order -> assertThat(order.overdue()).isTrue());
    }
}
