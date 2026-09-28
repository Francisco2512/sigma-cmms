package com.sigma.cmms.services;

import com.sigma.cmms.dto.PageResponse;
import com.sigma.cmms.dto.WorkOrderDtos.CloseRequest;
import com.sigma.cmms.dto.WorkOrderDtos.PartUsage;
import com.sigma.cmms.dto.WorkOrderDtos.WorkOrderCreateRequest;
import com.sigma.cmms.dto.WorkOrderDtos.WorkOrderDetail;
import com.sigma.cmms.dto.WorkOrderDtos.WorkOrderSummary;
import com.sigma.cmms.exception.ForbiddenOperationException;
import com.sigma.cmms.exception.NotFoundException;
import com.sigma.cmms.model.Role;
import com.sigma.cmms.model.SparePart;
import com.sigma.cmms.model.WorkOrder;
import com.sigma.cmms.model.WorkOrderStatus;
import com.sigma.cmms.model.WorkOrderType;
import com.sigma.cmms.repositories.SparePartRepository;
import com.sigma.cmms.repositories.UserRepository;
import com.sigma.cmms.repositories.WorkOrderRepository;
import com.sigma.cmms.repositories.WorkOrderSpecifications;
import com.sigma.cmms.security.CurrentUser;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.TreeMap;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Ciclo de vida de las ordenes de trabajo y consumo de refacciones. */
@Slf4j
@Service
@RequiredArgsConstructor
public class WorkOrderService {

    private final WorkOrderRepository workOrderRepository;
    private final SparePartRepository sparePartRepository;
    private final UserRepository userRepository;
    private final AssetService assetService;
    private final UserService userService;
    private final Clock clock;

    /**
     * Lista ordenes con filtros opcionales.
     *
     * @param onlyMine si es verdadero, solo las asignadas al usuario autenticado
     */
    @Transactional(readOnly = true)
    public PageResponse<WorkOrderSummary> list(WorkOrderStatus status, WorkOrderType type, Long assetId,
            boolean onlyMine, CurrentUser actor, Pageable pageable) {
        Long assignedToId = onlyMine ? actor.id() : null;
        LocalDate today = LocalDate.now(clock);
        return PageResponse.from(workOrderRepository.findAll(
                WorkOrderSpecifications.withFilters(status, type, assetId, assignedToId), pageable),
                wo -> WorkOrderSummary.from(wo, today));
    }

    /**
     * @throws NotFoundException si la orden no existe
     */
    @Transactional(readOnly = true)
    public WorkOrderDetail get(Long id) {
        return WorkOrderDetail.from(findDetailed(id), LocalDate.now(clock));
    }

    /**
     * Crea una orden. Un tecnico solo puede reportar fallas (correctivas) y sin asignarlas.
     *
     * @throws ForbiddenOperationException si un tecnico intenta crear un preventivo o asignar
     */
    @Transactional
    public WorkOrderDetail create(WorkOrderCreateRequest request, CurrentUser actor) {
        if (actor.hasRole(Role.TECNICO)
                && (request.type() != WorkOrderType.CORRECTIVA || request.assignedToId() != null)) {
            throw new ForbiddenOperationException("Un tecnico solo puede reportar fallas sin asignarlas");
        }
        WorkOrder order = new WorkOrder();
        order.setAsset(assetService.findEntity(request.assetId()));
        order.setType(request.type());
        order.setPriority(request.priority());
        order.setTitle(request.title());
        order.setDescription(request.description());
        order.setDueDate(request.dueDate());
        order.setCreatedBy(userRepository.getReferenceById(actor.id()));
        if (request.type() == WorkOrderType.CORRECTIVA) {
            order.setFailureAt(request.failureAt() != null ? request.failureAt() : LocalDateTime.now(clock));
        }
        if (request.assignedToId() != null) {
            order.assignTo(userService.findTechnician(request.assignedToId()));
        }
        WorkOrder saved = saveWithCode(order);
        log.info("Orden {} creada por {} para el activo {}", saved.getCode(), actor.username(),
                saved.getAsset().getCode());
        return WorkOrderDetail.from(saved, LocalDate.now(clock));
    }

    /**
     * Asigna (o reasigna) la orden a un tecnico.
     */
    @Transactional
    public WorkOrderDetail assign(Long id, Long technicianId) {
        WorkOrder order = findDetailed(id);
        order.assignTo(userService.findTechnician(technicianId));
        log.info("Orden {} asignada a {}", order.getCode(), order.getAssignedTo().getUsername());
        return WorkOrderDetail.from(order, LocalDate.now(clock));
    }

    /**
     * Inicia la ejecucion; el activo pasa a mantenimiento.
     *
     * @throws ForbiddenOperationException si un tecnico intenta iniciar una orden ajena
     */
    @Transactional
    public WorkOrderDetail start(Long id, CurrentUser actor) {
        WorkOrder order = findDetailed(id);
        requireExecutor(order, actor);
        order.start(LocalDateTime.now(clock));
        log.info("Orden {} iniciada por {}", order.getCode(), actor.username());
        return WorkOrderDetail.from(order, LocalDate.now(clock));
    }

    /**
     * Cierra la orden y descuenta las refacciones consumidas. Si alguna no tiene
     * existencia suficiente, no se descuenta ninguna (la transaccion se revierte).
     *
     * @throws com.sigma.cmms.exception.BusinessRuleException si falta existencia
     */
    @Transactional
    public WorkOrderDetail close(Long id, CloseRequest request, CurrentUser actor) {
        WorkOrder order = findDetailed(id);
        requireExecutor(order, actor);
        order.close(LocalDateTime.now(clock), request.laborHours(), request.resolutionNotes());
        consumeParts(order, request);
        log.info("Orden {} cerrada por {}; costo de refacciones {}", order.getCode(), actor.username(),
                order.partsCost());
        return WorkOrderDetail.from(order, LocalDate.now(clock));
    }

    /**
     * Cancela una orden que aun no inicia.
     */
    @Transactional
    public WorkOrderDetail cancel(Long id, String reason, CurrentUser actor) {
        WorkOrder order = findDetailed(id);
        order.cancel(reason);
        log.info("Orden {} cancelada por {}", order.getCode(), actor.username());
        return WorkOrderDetail.from(order, LocalDate.now(clock));
    }

    /** Guarda la orden y le asigna su folio legible a partir del id generado. */
    WorkOrder saveWithCode(WorkOrder order) {
        WorkOrder saved = workOrderRepository.save(order);
        saved.setCode(String.format("OT-%d-%05d", LocalDate.now(clock).getYear(), saved.getId()));
        return saved;
    }

    private void consumeParts(WorkOrder order, CloseRequest request) {
        // Se agrupan por refaccion y se bloquean en orden de id para evitar interbloqueos
        Map<Long, Integer> quantities = new TreeMap<>();
        for (PartUsage usage : request.partsOrEmpty()) {
            quantities.merge(usage.sparePartId(), usage.quantity(), Integer::sum);
        }
        quantities.forEach((partId, quantity) -> {
            SparePart part = sparePartRepository.findByIdForUpdate(partId)
                    .orElseThrow(() -> new NotFoundException("Refaccion", partId));
            part.consume(quantity);
            order.addPart(part, quantity);
            if (part.isBelowReorderPoint()) {
                log.warn("Alerta de reorden: {} quedo con {} (punto de reorden {})", part.getSku(),
                        part.getStock(), part.getReorderPoint());
            }
        });
    }

    private static void requireExecutor(WorkOrder order, CurrentUser actor) {
        if (actor.hasRole(Role.TECNICO) && !order.isAssignedTo(actor.id())) {
            throw new ForbiddenOperationException("La orden " + order.getCode() + " no esta asignada a usted");
        }
    }

    private WorkOrder findDetailed(Long id) {
        return workOrderRepository.findWithDetailsById(id).orElseThrow(() -> new NotFoundException("Orden", id));
    }
}
