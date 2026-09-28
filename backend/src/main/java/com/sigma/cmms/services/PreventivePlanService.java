package com.sigma.cmms.services;

import com.sigma.cmms.dto.PlanDtos.GenerationResult;
import com.sigma.cmms.dto.PlanDtos.PlanRequest;
import com.sigma.cmms.dto.PlanDtos.PlanResponse;
import com.sigma.cmms.dto.WorkOrderDtos.WorkOrderSummary;
import com.sigma.cmms.exception.NotFoundException;
import com.sigma.cmms.model.PlanStatus;
import com.sigma.cmms.model.PreventivePlan;
import com.sigma.cmms.model.User;
import com.sigma.cmms.model.WorkOrder;
import com.sigma.cmms.model.WorkOrderType;
import com.sigma.cmms.repositories.PreventivePlanRepository;
import com.sigma.cmms.repositories.WorkOrderRepository;
import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Planes preventivos y generacion automatica de sus ordenes. */
@Slf4j
@Service
@RequiredArgsConstructor
public class PreventivePlanService {

    private final PreventivePlanRepository planRepository;
    private final WorkOrderRepository workOrderRepository;
    private final AssetService assetService;
    private final WorkOrderService workOrderService;
    private final UserService userService;
    private final Clock clock;

    /**
     * @return todos los planes, del vencimiento mas proximo al mas lejano
     */
    @Transactional(readOnly = true)
    public List<PlanResponse> list() {
        LocalDate today = LocalDate.now(clock);
        return planRepository.findAllByOrderByNextDueDateAsc().stream()
                .map(plan -> PlanResponse.from(plan, today)).toList();
    }

    /**
     * Crea un plan activo.
     */
    @Transactional
    public PlanResponse create(PlanRequest request) {
        PreventivePlan plan = new PreventivePlan();
        plan.setName(request.name());
        plan.setAsset(assetService.findEntity(request.assetId()));
        plan.setFrequencyDays(request.frequencyDays());
        plan.setNextDueDate(request.nextDueDate());
        plan.setTaskDescription(request.taskDescription());
        plan.setPriority(request.priority());
        PreventivePlan saved = planRepository.save(plan);
        log.info("Plan preventivo '{}' creado cada {} dias", saved.getName(), saved.getFrequencyDays());
        return PlanResponse.from(saved, LocalDate.now(clock));
    }

    /**
     * Activa o pausa un plan. Un plan pausado deja de generar ordenes.
     */
    @Transactional
    public PlanResponse changeStatus(Long id, PlanStatus status) {
        PreventivePlan plan = planRepository.findWithAssetById(id)
                .orElseThrow(() -> new NotFoundException("Plan preventivo", id));
        plan.setStatus(status);
        return PlanResponse.from(plan, LocalDate.now(clock));
    }

    /**
     * Genera una orden por cada plan activo vencido y mueve su siguiente vencimiento.
     * Es idempotente: ejecutarla dos veces el mismo dia no duplica ordenes.
     *
     * @param requestedBy usuario que figura como creador de las ordenes
     * @return ordenes generadas y ciclos vencidos que se omitieron
     */
    @Transactional
    public GenerationResult generateDueOrders(User requestedBy) {
        LocalDate today = LocalDate.now(clock);
        List<WorkOrderSummary> created = new ArrayList<>();
        int skippedCycles = 0;
        for (PreventivePlan plan : planRepository.findByStatusAndNextDueDateLessThanEqual(PlanStatus.ACTIVO, today)) {
            if (!workOrderRepository.existsByPreventivePlanIdAndDueDate(plan.getId(), plan.getNextDueDate())) {
                WorkOrder order = workOrderService.saveWithCode(newPreventiveOrder(plan, requestedBy));
                created.add(WorkOrderSummary.from(order, today));
            }
            skippedCycles += plan.advancePast(today);
        }
        log.info("Generacion preventiva: {} ordenes creadas, {} ciclos vencidos omitidos",
                created.size(), skippedCycles);
        return new GenerationResult(created.size(), skippedCycles, created);
    }

    /**
     * Generacion solicitada manualmente desde la interfaz.
     *
     * @param userId usuario que la solicita
     */
    @Transactional
    public GenerationResult generateDueOrdersBy(Long userId) {
        return generateDueOrders(userService.findEntity(userId));
    }

    private static WorkOrder newPreventiveOrder(PreventivePlan plan, User requestedBy) {
        WorkOrder order = new WorkOrder();
        order.setAsset(plan.getAsset());
        order.setType(WorkOrderType.PREVENTIVA);
        order.setPriority(plan.getPriority());
        order.setTitle("Preventivo: " + plan.getName());
        order.setDescription(plan.getTaskDescription());
        order.setDueDate(plan.getNextDueDate());
        order.setPreventivePlan(plan);
        order.setCreatedBy(requestedBy);
        return order;
    }
}
