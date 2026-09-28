package com.sigma.cmms.controller;

import com.sigma.cmms.dto.ApiResult;
import com.sigma.cmms.dto.PageResponse;
import com.sigma.cmms.dto.WorkOrderDtos.AssignRequest;
import com.sigma.cmms.dto.WorkOrderDtos.CancelRequest;
import com.sigma.cmms.dto.WorkOrderDtos.CloseRequest;
import com.sigma.cmms.dto.WorkOrderDtos.WorkOrderCreateRequest;
import com.sigma.cmms.dto.WorkOrderDtos.WorkOrderDetail;
import com.sigma.cmms.dto.WorkOrderDtos.WorkOrderSummary;
import com.sigma.cmms.model.WorkOrderStatus;
import com.sigma.cmms.model.WorkOrderType;
import com.sigma.cmms.security.Authz;
import com.sigma.cmms.security.CurrentUser;
import com.sigma.cmms.services.WorkOrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.net.URI;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

/** Ordenes de trabajo y sus transiciones (RF-02, RF-04, RF-06). */
@Tag(name = "Ordenes de trabajo")
@RestController
@RequestMapping("/api/v1/work-orders")
@RequiredArgsConstructor
public class WorkOrderController {

    private static final String OK = "200";

    private final WorkOrderService workOrderService;

    /**
     * Lista ordenes con filtros opcionales de estatus, tipo y activo.
     *
     * @param mine si es verdadero, solo las asignadas al usuario autenticado
     * @return 200 con una pagina ordenada por fecha compromiso
     */
    @Operation(summary = "Lista ordenes con filtros opcionales")
    @ApiResponse(responseCode = OK, description = "Pagina de ordenes")
    @GetMapping
    public ResponseEntity<ApiResult<PageResponse<WorkOrderSummary>>> list(
            @RequestParam(required = false) WorkOrderStatus status,
            @RequestParam(required = false) WorkOrderType type,
            @RequestParam(required = false) Long assetId,
            @RequestParam(defaultValue = "false") boolean mine,
            @AuthenticationPrincipal Jwt jwt,
            @PageableDefault(size = 20, sort = "dueDate", direction = Sort.Direction.ASC) Pageable pageable) {
        return ResponseEntity.ok(ApiResult.ok(
                workOrderService.list(status, type, assetId, mine, CurrentUser.from(jwt), pageable)));
    }

    /**
     * Consulta el detalle de una orden con las refacciones consumidas.
     *
     * @param workOrderId identificador de la orden
     * @return 200 con el detalle, o 404 si la orden no existe
     */
    @Operation(summary = "Detalle de una orden con las refacciones consumidas")
    @ApiResponse(responseCode = OK, description = "Orden encontrada")
    @ApiResponse(responseCode = "404", description = "La orden no existe")
    @GetMapping("/{workOrderId}")
    public ResponseEntity<ApiResult<WorkOrderDetail>> get(@PathVariable Long workOrderId) {
        return ResponseEntity.ok(ApiResult.ok(workOrderService.get(workOrderId)));
    }

    /**
     * Crea una orden de trabajo. Un tecnico solo puede reportar fallas correctivas
     * y sin asignarlas.
     *
     * @param request datos de la orden
     * @param jwt token de quien la crea
     * @return 201 con la cabecera Location, o 400 si los datos no son validos
     */
    @Operation(summary = "Crea una orden; los tecnicos solo reportan fallas")
    @ApiResponse(responseCode = "201", description = "Orden creada")
    @ApiResponse(responseCode = "400", description = "Datos invalidos")
    @PreAuthorize(Authz.CAN_REPORT)
    @PostMapping
    public ResponseEntity<ApiResult<WorkOrderDetail>> create(@Valid @RequestBody WorkOrderCreateRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        WorkOrderDetail created = workOrderService.create(request, CurrentUser.from(jwt));
        URI location = ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}")
                .buildAndExpand(created.summary().id()).toUri();
        return ResponseEntity.created(location).body(ApiResult.ok(created, "Orden " + created.summary().code()
                + " creada"));
    }

    /**
     * Asigna o reasigna la orden a un tecnico. Requiere rol de planeacion.
     *
     * @param workOrderId identificador de la orden
     * @param request tecnico al que se asigna
     * @return 200 con la orden asignada, o 409 si ya inicio o termino
     */
    @Operation(summary = "Asigna la orden a un tecnico")
    @ApiResponse(responseCode = OK, description = "Orden asignada")
    @ApiResponse(responseCode = "409", description = "La orden ya inicio o termino")
    @PreAuthorize(Authz.CAN_PLAN)
    @PatchMapping("/{workOrderId}/assign")
    public ResponseEntity<ApiResult<WorkOrderDetail>> assign(@PathVariable Long workOrderId,
            @Valid @RequestBody AssignRequest request) {
        return ResponseEntity.ok(ApiResult.ok(workOrderService.assign(workOrderId, request.technicianId())));
    }

    /**
     * Inicia la ejecucion de la orden; el activo pasa a mantenimiento.
     *
     * @param workOrderId identificador de la orden
     * @param jwt token de quien la ejecuta
     * @return 200 con la orden en proceso, o 403 si la orden es de otro tecnico
     */
    @Operation(summary = "Inicia la ejecucion de la orden")
    @ApiResponse(responseCode = OK, description = "Orden en proceso")
    @PreAuthorize(Authz.CAN_EXECUTE)
    @PatchMapping("/{workOrderId}/start")
    public ResponseEntity<ApiResult<WorkOrderDetail>> start(@PathVariable Long workOrderId,
            @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(ApiResult.ok(workOrderService.start(workOrderId, CurrentUser.from(jwt))));
    }

    /**
     * Cierra la orden y descuenta las refacciones consumidas. Si alguna no tiene existencia
     * suficiente no se descuenta ninguna.
     *
     * @param workOrderId identificador de la orden
     * @param request horas, solucion y refacciones
     * @param jwt token de quien la ejecuta
     * @return 200 con la orden cerrada, o 422 si falta existencia
     */
    @Operation(summary = "Cierra la orden y descuenta refacciones")
    @ApiResponse(responseCode = OK, description = "Orden cerrada")
    @ApiResponse(responseCode = "422", description = "Existencia insuficiente de alguna refaccion")
    @PreAuthorize(Authz.CAN_EXECUTE)
    @PatchMapping("/{workOrderId}/close")
    public ResponseEntity<ApiResult<WorkOrderDetail>> close(@PathVariable Long workOrderId,
            @Valid @RequestBody CloseRequest request, @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(ApiResult.ok(
                workOrderService.close(workOrderId, request, CurrentUser.from(jwt)), "Orden cerrada"));
    }

    /**
     * Cancela una orden que aun no inicia. Requiere rol de supervision.
     *
     * @param workOrderId identificador de la orden
     * @param request motivo de la cancelacion
     * @return 200 con la orden cancelada, o 409 si ya inicio
     */
    @Operation(summary = "Cancela una orden que no ha iniciado")
    @ApiResponse(responseCode = OK, description = "Orden cancelada")
    @PreAuthorize(Authz.SUPERVISOR)
    @PatchMapping("/{workOrderId}/cancel")
    public ResponseEntity<ApiResult<WorkOrderDetail>> cancel(@PathVariable Long workOrderId,
            @Valid @RequestBody CancelRequest request, @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(ApiResult.ok(
                workOrderService.cancel(workOrderId, request.reason(), CurrentUser.from(jwt))));
    }
}
