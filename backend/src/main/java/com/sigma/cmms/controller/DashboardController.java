package com.sigma.cmms.controller;

import com.sigma.cmms.dto.ApiResult;
import com.sigma.cmms.dto.KpiDtos.KpiResponse;
import com.sigma.cmms.security.Authz;
import com.sigma.cmms.services.KpiService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Tablero de indicadores. */
@Tag(name = "Indicadores")
@Validated
@RestController
@RequestMapping("/api/v1/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final KpiService kpiService;

    /**
     * Calcula los indicadores del periodo: MTBF, MTTR, disponibilidad y cumplimiento preventivo.
     *
     * @param days ventana de observacion, entre 7 y 365 dias
     * @return 200 con los indicadores y los activos con mas fallas
     */
    @Operation(summary = "MTBF, MTTR, disponibilidad y cumplimiento preventivo del periodo")
    @ApiResponse(responseCode = "200", description = "Indicadores calculados")
    @PreAuthorize(Authz.CAN_PLAN)
    @GetMapping("/kpis")
    public ResponseEntity<ApiResult<KpiResponse>> kpis(
            @RequestParam(defaultValue = "90") @Min(7) @Max(365) int days) {
        return ResponseEntity.ok(ApiResult.ok(kpiService.compute(days)));
    }
}
