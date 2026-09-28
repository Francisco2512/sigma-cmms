package com.sigma.cmms.controller;

import com.sigma.cmms.dto.ApiResult;
import com.sigma.cmms.dto.PlanDtos.GenerationResult;
import com.sigma.cmms.dto.PlanDtos.PlanRequest;
import com.sigma.cmms.dto.PlanDtos.PlanResponse;
import com.sigma.cmms.dto.PlanDtos.PlanStatusRequest;
import com.sigma.cmms.security.Authz;
import com.sigma.cmms.security.CurrentUser;
import com.sigma.cmms.services.PreventivePlanService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

/** Planes de mantenimiento preventivo (RF-03). */
@Tag(name = "Planes preventivos")
@RestController
@RequestMapping("/api/v1/preventive-plans")
@RequiredArgsConstructor
public class PreventivePlanController {

    private final PreventivePlanService planService;

    /**
     * Lista los planes preventivos ordenados por fecha de vencimiento.
     *
     * @return 200 con los planes y su indicador de vencimiento
     */
    @Operation(summary = "Lista los planes por fecha de vencimiento")
    @ApiResponse(responseCode = "200", description = "Planes")
    @GetMapping
    public ResponseEntity<ApiResult<List<PlanResponse>>> list() {
        return ResponseEntity.ok(ApiResult.ok(planService.list()));
    }

    /**
     * Crea un plan preventivo activo. Requiere rol de planeacion.
     *
     * @param request datos del plan
     * @return 201 con la cabecera Location
     */
    @Operation(summary = "Crea un plan preventivo")
    @ApiResponse(responseCode = "201", description = "Plan creado")
    @PreAuthorize(Authz.CAN_PLAN)
    @PostMapping
    public ResponseEntity<ApiResult<PlanResponse>> create(@Valid @RequestBody PlanRequest request) {
        PlanResponse created = planService.create(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}")
                .buildAndExpand(created.id()).toUri();
        return ResponseEntity.created(location).body(ApiResult.ok(created, "Plan creado"));
    }

    /**
     * Activa o pausa un plan; un plan pausado deja de generar ordenes.
     *
     * @param planId identificador del plan
     * @param request nuevo estatus
     * @return 200 con el plan actualizado
     */
    @Operation(summary = "Activa o pausa un plan")
    @ApiResponse(responseCode = "200", description = "Estatus actualizado")
    @PreAuthorize(Authz.CAN_PLAN)
    @PatchMapping("/{planId}/status")
    public ResponseEntity<ApiResult<PlanResponse>> changeStatus(@PathVariable Long planId,
            @Valid @RequestBody PlanStatusRequest request) {
        return ResponseEntity.ok(ApiResult.ok(planService.changeStatus(planId, request.status())));
    }

    /**
     * Genera de inmediato las ordenes de los planes vencidos. Es idempotente: ejecutarla
     * dos veces el mismo dia no duplica ordenes.
     *
     * @param jwt token de quien solicita la generacion
     * @return 200 con las ordenes generadas y los ciclos omitidos
     */
    @Operation(summary = "Genera ahora las ordenes de los planes vencidos (normalmente corre cada dia)")
    @ApiResponse(responseCode = "200", description = "Resultado de la generacion")
    @PreAuthorize(Authz.CAN_PLAN)
    @PostMapping("/generate")
    public ResponseEntity<ApiResult<GenerationResult>> generate(@AuthenticationPrincipal Jwt jwt) {
        GenerationResult result = planService.generateDueOrdersBy(CurrentUser.from(jwt).id());
        return ResponseEntity.ok(ApiResult.ok(result, result.generated() + " ordenes generadas"));
    }
}
