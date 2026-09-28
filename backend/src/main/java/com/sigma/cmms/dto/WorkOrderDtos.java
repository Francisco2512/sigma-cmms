package com.sigma.cmms.dto;

import com.sigma.cmms.model.Priority;
import com.sigma.cmms.model.User;
import com.sigma.cmms.model.WorkOrder;
import com.sigma.cmms.model.WorkOrderPart;
import com.sigma.cmms.model.WorkOrderStatus;
import com.sigma.cmms.model.WorkOrderType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/** DTOs de ordenes de trabajo. */
public final class WorkOrderDtos {

    private WorkOrderDtos() {
    }

    public record WorkOrderCreateRequest(
            @NotNull Long assetId,
            @NotNull WorkOrderType type,
            @NotNull Priority priority,
            @NotBlank @Size(max = 150) String title,
            @Size(max = 1000) String description,
            @NotNull LocalDate dueDate,
            Long assignedToId,
            @PastOrPresent LocalDateTime failureAt) {
    }

    public record AssignRequest(@NotNull Long technicianId) {
    }

    public record PartUsage(
            @NotNull Long sparePartId,
            @NotNull @Min(1) @Max(1000) Integer quantity) {
    }

    public record CloseRequest(
            @NotNull @DecimalMin("0.25") @DecimalMax("999.99") @Digits(integer = 4, fraction = 2)
            BigDecimal laborHours,
            @NotBlank @Size(max = 1000) String resolutionNotes,
            @Valid @Size(max = 20) List<PartUsage> parts) {

        public List<PartUsage> partsOrEmpty() {
            return parts == null ? List.of() : parts;
        }
    }

    public record CancelRequest(@NotBlank @Size(max = 300) String reason) {
    }

    public record WorkOrderSummary(Long id, String code, String title, WorkOrderType type, Priority priority,
            WorkOrderStatus status, Long assetId, String assetCode, String assetName, Long assignedToId,
            String assignedToName, LocalDate dueDate, boolean overdue, LocalDateTime createdAt,
            LocalDateTime closedAt) {

        public static WorkOrderSummary from(WorkOrder wo, LocalDate today) {
            User tech = wo.getAssignedTo();
            return new WorkOrderSummary(wo.getId(), wo.getCode(), wo.getTitle(), wo.getType(), wo.getPriority(),
                    wo.getStatus(), wo.getAsset().getId(), wo.getAsset().getCode(), wo.getAsset().getName(),
                    tech == null ? null : tech.getId(), tech == null ? null : tech.getFullName(),
                    wo.getDueDate(), wo.isOverdue(today), wo.getCreatedAt(), wo.getClosedAt());
        }
    }

    public record PartLine(Long sparePartId, String sku, String name, int quantity, BigDecimal unitCost,
            BigDecimal subtotal) {

        public static PartLine from(WorkOrderPart line) {
            return new PartLine(line.getSparePart().getId(), line.getSparePart().getSku(),
                    line.getSparePart().getName(), line.getQuantity(), line.getUnitCost(), line.subtotal());
        }
    }

    public record WorkOrderDetail(WorkOrderSummary summary, String description, String createdByName,
            Long preventivePlanId, LocalDateTime failureAt, LocalDateTime startedAt, BigDecimal laborHours,
            String resolutionNotes, List<PartLine> parts, BigDecimal partsCost) {

        public static WorkOrderDetail from(WorkOrder wo, LocalDate today) {
            return new WorkOrderDetail(WorkOrderSummary.from(wo, today), wo.getDescription(),
                    wo.getCreatedBy().getFullName(),
                    wo.getPreventivePlan() == null ? null : wo.getPreventivePlan().getId(),
                    wo.getFailureAt(), wo.getStartedAt(), wo.getLaborHours(), wo.getResolutionNotes(),
                    wo.getParts().stream().map(PartLine::from).toList(), wo.partsCost());
        }
    }
}
