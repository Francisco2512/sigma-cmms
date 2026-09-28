package com.sigma.cmms.dto;

import com.sigma.cmms.dto.WorkOrderDtos.WorkOrderSummary;
import com.sigma.cmms.model.PlanStatus;
import com.sigma.cmms.model.PreventivePlan;
import com.sigma.cmms.model.Priority;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.List;

/** DTOs de planes de mantenimiento preventivo. */
public final class PlanDtos {

    private PlanDtos() {
    }

    public record PlanRequest(
            @NotBlank @Size(max = 120) String name,
            @NotNull Long assetId,
            @NotNull @Min(1) @Max(730) Integer frequencyDays,
            @NotNull LocalDate nextDueDate,
            @NotBlank @Size(max = 1000) String taskDescription,
            @NotNull Priority priority) {
    }

    public record PlanStatusRequest(@NotNull PlanStatus status) {
    }

    public record PlanResponse(Long id, String name, Long assetId, String assetCode, String assetName,
            int frequencyDays, LocalDate nextDueDate, String taskDescription, Priority priority,
            PlanStatus status, boolean due) {

        public static PlanResponse from(PreventivePlan plan, LocalDate today) {
            return new PlanResponse(plan.getId(), plan.getName(), plan.getAsset().getId(),
                    plan.getAsset().getCode(), plan.getAsset().getName(), plan.getFrequencyDays(),
                    plan.getNextDueDate(), plan.getTaskDescription(), plan.getPriority(), plan.getStatus(),
                    plan.isDue(today));
        }
    }

    public record GenerationResult(int generated, int skippedCycles, List<WorkOrderSummary> orders) {
    }
}
