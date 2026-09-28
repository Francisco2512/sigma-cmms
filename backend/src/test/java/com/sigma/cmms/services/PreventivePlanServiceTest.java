package com.sigma.cmms.services;

import static com.sigma.cmms.support.TestData.CLOCK;
import static com.sigma.cmms.support.TestData.TODAY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.sigma.cmms.dto.PlanDtos.GenerationResult;
import com.sigma.cmms.dto.PlanDtos.PlanRequest;
import com.sigma.cmms.dto.PlanDtos.PlanResponse;
import com.sigma.cmms.exception.NotFoundException;
import com.sigma.cmms.model.PlanStatus;
import com.sigma.cmms.model.PreventivePlan;
import com.sigma.cmms.model.Priority;
import com.sigma.cmms.model.Role;
import com.sigma.cmms.model.User;
import com.sigma.cmms.model.WorkOrder;
import com.sigma.cmms.model.WorkOrderType;
import com.sigma.cmms.repositories.PreventivePlanRepository;
import com.sigma.cmms.repositories.WorkOrderRepository;
import com.sigma.cmms.support.TestData;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PreventivePlanServiceTest {

    @Mock
    private PreventivePlanRepository planRepository;
    @Mock
    private WorkOrderRepository workOrderRepository;
    @Mock
    private AssetService assetService;
    @Mock
    private WorkOrderService workOrderService;
    @Mock
    private UserService userService;

    private PreventivePlanService service;
    private final User planner = TestData.user(3, Role.PLANIFICADOR);

    @BeforeEach
    void setUp() {
        service = new PreventivePlanService(planRepository, workOrderRepository, assetService, workOrderService,
                userService, CLOCK);
    }

    @Test
    void should_generatePreventiveOrder_when_activePlanIsDue() {
        // Arrange
        PreventivePlan plan = TestData.plan(1, TODAY, 30);
        when(planRepository.findByStatusAndNextDueDateLessThanEqual(PlanStatus.ACTIVO, TODAY))
                .thenReturn(List.of(plan));
        when(workOrderRepository.existsByPreventivePlanIdAndDueDate(1L, TODAY)).thenReturn(false);
        when(workOrderService.saveWithCode(any(WorkOrder.class))).thenAnswer(invocation -> {
            WorkOrder order = invocation.getArgument(0);
            order.setId(50L);
            order.setCode("OT-2026-00050");
            return order;
        });

        // Act
        GenerationResult result = service.generateDueOrders(planner);

        // Assert
        assertThat(result.generated()).isEqualTo(1);
        assertThat(result.orders()).singleElement().satisfies(order -> {
            assertThat(order.type()).isEqualTo(WorkOrderType.PREVENTIVA);
            assertThat(order.dueDate()).isEqualTo(TODAY);
        });
        assertThat(plan.getNextDueDate()).isEqualTo(TODAY.plusDays(30));
    }

    @Test
    void should_notDuplicateOrder_when_orderForDueDateAlreadyExists() {
        // Arrange
        PreventivePlan plan = TestData.plan(1, TODAY, 30);
        when(planRepository.findByStatusAndNextDueDateLessThanEqual(PlanStatus.ACTIVO, TODAY))
                .thenReturn(List.of(plan));
        when(workOrderRepository.existsByPreventivePlanIdAndDueDate(1L, TODAY)).thenReturn(true);

        // Act
        GenerationResult result = service.generateDueOrders(planner);

        // Assert
        assertThat(result.generated()).isZero();
        verify(workOrderService, never()).saveWithCode(any());
        assertThat(plan.getNextDueDate()).isEqualTo(TODAY.plusDays(30));
    }

    @Test
    void should_reportSkippedCycles_when_planIsSeveralCyclesLate() {
        // Arrange
        PreventivePlan plan = TestData.plan(1, TODAY.minusDays(25), 10);
        when(planRepository.findByStatusAndNextDueDateLessThanEqual(PlanStatus.ACTIVO, TODAY))
                .thenReturn(List.of(plan));
        when(workOrderRepository.existsByPreventivePlanIdAndDueDate(1L, TODAY.minusDays(25))).thenReturn(false);
        when(workOrderService.saveWithCode(any(WorkOrder.class))).thenAnswer(invocation -> {
            WorkOrder order = invocation.getArgument(0);
            order.setId(51L);
            return order;
        });

        // Act
        GenerationResult result = service.generateDueOrders(planner);

        // Assert
        assertThat(result.generated()).isEqualTo(1);
        assertThat(result.skippedCycles()).isEqualTo(2);
    }

    @Test
    void should_generateNothing_when_noPlanIsDue() {
        // Arrange
        when(planRepository.findByStatusAndNextDueDateLessThanEqual(PlanStatus.ACTIVO, TODAY))
                .thenReturn(List.of());

        // Act
        GenerationResult result = service.generateDueOrders(planner);

        // Assert
        assertThat(result.generated()).isZero();
        assertThat(result.orders()).isEmpty();
    }

    @Test
    void should_createActivePlan_when_requestIsValid() {
        // Arrange
        when(assetService.findEntity(1L)).thenReturn(TestData.asset(1));
        when(planRepository.save(any(PreventivePlan.class))).thenAnswer(invocation -> invocation.getArgument(0));
        PlanRequest request = new PlanRequest("Cambio de aceite", 1L, 30, TODAY.plusDays(3), "Cambiar aceite",
                Priority.ALTA);

        // Act
        PlanResponse created = service.create(request);

        // Assert
        assertThat(created.status()).isEqualTo(PlanStatus.ACTIVO);
        assertThat(created.due()).isFalse();
    }

    @Test
    void should_pausePlan_when_statusChanges() {
        // Arrange
        when(planRepository.findWithAssetById(1L)).thenReturn(Optional.of(TestData.plan(1, TODAY, 30)));

        // Act
        PlanResponse paused = service.changeStatus(1L, PlanStatus.PAUSADO);

        // Assert
        assertThat(paused.status()).isEqualTo(PlanStatus.PAUSADO);
        assertThat(paused.due()).isFalse();
    }

    @Test
    void should_throwNotFound_when_planDoesNotExist() {
        // Arrange
        when(planRepository.findWithAssetById(9L)).thenReturn(Optional.empty());

        // Act + Assert
        assertThatThrownBy(() -> service.changeStatus(9L, PlanStatus.ACTIVO)).isInstanceOf(NotFoundException.class);
    }

    @Test
    void should_flagDuePlans_when_listing() {
        // Arrange
        when(planRepository.findAllByOrderByNextDueDateAsc()).thenReturn(
                List.of(TestData.plan(1, TODAY.minusDays(1), 15), TestData.plan(2, TODAY.plusDays(4), 15)));

        // Act
        List<PlanResponse> plans = service.list();

        // Assert
        assertThat(plans).extracting(PlanResponse::due).containsExactly(true, false);
    }

    @Test
    void should_useRequesterAsCreator_when_generationIsManual() {
        // Arrange
        when(userService.findEntity(3L)).thenReturn(planner);
        when(planRepository.findByStatusAndNextDueDateLessThanEqual(PlanStatus.ACTIVO, TODAY)).thenReturn(List.of());

        // Act
        GenerationResult result = service.generateDueOrdersBy(3L);

        // Assert
        assertThat(result.generated()).isZero();
        verify(userService).findEntity(3L);
    }
}
