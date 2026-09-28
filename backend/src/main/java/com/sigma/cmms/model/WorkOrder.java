package com.sigma.cmms.model;

import com.sigma.cmms.exception.InvalidStateTransitionException;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * Orden de trabajo. Las transiciones de estado viven aqui para que ninguna capa
 * pueda dejar una orden en un estado invalido.
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "sigma_work_orders")
public class WorkOrder extends BaseEntity {

    private static final Set<WorkOrderStatus> ASSIGNABLE =
            EnumSet.of(WorkOrderStatus.ABIERTA, WorkOrderStatus.ASIGNADA);

    @Column(length = 20)
    private String code;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "asset_id", nullable = false)
    private Asset asset;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 32)
    private WorkOrderType type;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 32)
    private Priority priority;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 32)
    @Setter(AccessLevel.NONE)
    private WorkOrderStatus status = WorkOrderStatus.ABIERTA;

    @Column(nullable = false, length = 150)
    private String title;

    @Column(length = 1000)
    private String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_to_id")
    @Setter(AccessLevel.NONE)
    private User assignedTo;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "created_by_id", nullable = false)
    private User createdBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "preventive_plan_id")
    private PreventivePlan preventivePlan;

    @Column(name = "due_date", nullable = false)
    private LocalDate dueDate;

    @Column(name = "failure_at")
    private LocalDateTime failureAt;

    @Column(name = "started_at")
    private LocalDateTime startedAt;

    @Column(name = "closed_at")
    private LocalDateTime closedAt;

    @Column(name = "labor_hours", precision = 6, scale = 2)
    private BigDecimal laborHours;

    @Column(name = "resolution_notes", length = 1000)
    private String resolutionNotes;

    @OneToMany(mappedBy = "workOrder", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<WorkOrderPart> parts = new ArrayList<>();

    /**
     * Asigna la orden a un tecnico.
     *
     * @param technician tecnico responsable
     * @throws InvalidStateTransitionException si la orden ya inicio o termino
     */
    public void assignTo(User technician) {
        requireStatus(ASSIGNABLE, "asignar");
        this.assignedTo = technician;
        this.status = WorkOrderStatus.ASIGNADA;
    }

    /**
     * Inicia la ejecucion y marca el activo como en mantenimiento.
     *
     * @param now momento de inicio
     * @throws InvalidStateTransitionException si la orden no esta asignada
     */
    public void start(LocalDateTime now) {
        requireStatus(EnumSet.of(WorkOrderStatus.ASIGNADA), "iniciar");
        this.startedAt = now;
        this.status = WorkOrderStatus.EN_PROCESO;
        asset.setStatus(AssetStatus.EN_MANTENIMIENTO);
    }

    /**
     * Cierra la orden y devuelve el activo a operacion.
     *
     * @param now momento de cierre
     * @param hours horas de mano de obra
     * @param notes trabajo realizado
     * @throws InvalidStateTransitionException si la orden no esta en proceso
     */
    public void close(LocalDateTime now, BigDecimal hours, String notes) {
        requireStatus(EnumSet.of(WorkOrderStatus.EN_PROCESO), "cerrar");
        this.closedAt = now;
        this.laborHours = hours;
        this.resolutionNotes = notes;
        this.status = WorkOrderStatus.CERRADA;
        asset.setStatus(AssetStatus.OPERATIVO);
    }

    /**
     * Cancela la orden y conserva el motivo en las notas.
     *
     * @param reason motivo de la cancelacion
     * @throws InvalidStateTransitionException si la orden ya inicio
     */
    public void cancel(String reason) {
        requireStatus(ASSIGNABLE, "cancelar");
        this.resolutionNotes = "Cancelada: " + reason;
        this.status = WorkOrderStatus.CANCELADA;
    }

    /**
     * Agrega una refaccion consumida, con el costo unitario del momento.
     *
     * @param part refaccion consumida
     * @param quantity cantidad consumida
     */
    public void addPart(SparePart part, int quantity) {
        parts.add(new WorkOrderPart(this, part, quantity, part.getUnitCost()));
    }

    /** @return verdadero si la orden esta cerrada o cancelada */
    public boolean isFinished() {
        return status == WorkOrderStatus.CERRADA || status == WorkOrderStatus.CANCELADA;
    }

    /**
     * @param today fecha de referencia
     * @return verdadero si sigue pendiente y su fecha compromiso ya paso
     */
    public boolean isOverdue(LocalDate today) {
        return !isFinished() && dueDate.isBefore(today);
    }

    /**
     * @param userId identificador del usuario
     * @return verdadero si la orden esta asignada a ese usuario
     */
    public boolean isAssignedTo(Long userId) {
        return assignedTo != null && assignedTo.getId().equals(userId);
    }

    /** @return costo total de las refacciones consumidas */
    public BigDecimal partsCost() {
        return parts.stream().map(WorkOrderPart::subtotal).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private void requireStatus(Set<WorkOrderStatus> allowed, String action) {
        if (!allowed.contains(status)) {
            throw new InvalidStateTransitionException(
                    "No se puede " + action + " una orden en estado " + status);
        }
    }
}
