package com.sigma.cmms.controller;

import com.sigma.cmms.dto.ApiResult;
import com.sigma.cmms.dto.AssetDtos.AssetRequest;
import com.sigma.cmms.dto.AssetDtos.AssetResponse;
import com.sigma.cmms.dto.PageResponse;
import com.sigma.cmms.dto.WorkOrderDtos.WorkOrderSummary;
import com.sigma.cmms.model.AssetStatus;
import com.sigma.cmms.security.Authz;
import com.sigma.cmms.services.AssetService;
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
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

/** Catalogo de activos (RF-01). */
@Tag(name = "Activos")
@RestController
@RequestMapping("/api/v1/assets")
@RequiredArgsConstructor
public class AssetController {

    private static final String OK = "200";

    private final AssetService assetService;

    @Operation(summary = "Busca activos por codigo o nombre, area y estatus")
    @ApiResponse(responseCode = OK, description = "Pagina de activos")
    @GetMapping
    public ResponseEntity<ApiResult<PageResponse<AssetResponse>>> search(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String area,
            @RequestParam(required = false) AssetStatus status,
            @PageableDefault(size = 20, sort = "code", direction = Sort.Direction.ASC) Pageable pageable) {
        return ResponseEntity.ok(ApiResult.ok(assetService.search(search, area, status, pageable)));
    }

    @Operation(summary = "Ficha de un activo")
    @ApiResponse(responseCode = OK, description = "Activo encontrado")
    @ApiResponse(responseCode = "404", description = "El activo no existe")
    @GetMapping("/{assetId}")
    public ResponseEntity<ApiResult<AssetResponse>> get(@PathVariable Long assetId) {
        return ResponseEntity.ok(ApiResult.ok(assetService.get(assetId)));
    }

    @Operation(summary = "Historial de ordenes del activo")
    @ApiResponse(responseCode = OK, description = "Pagina del historial")
    @GetMapping("/{assetId}/work-orders")
    public ResponseEntity<ApiResult<PageResponse<WorkOrderSummary>>> history(@PathVariable Long assetId,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(ApiResult.ok(assetService.history(assetId, pageable)));
    }

    @Operation(summary = "Registra un activo")
    @ApiResponse(responseCode = "201", description = "Activo creado")
    @ApiResponse(responseCode = "409", description = "Codigo duplicado")
    @PreAuthorize(Authz.CAN_PLAN)
    @PostMapping
    public ResponseEntity<ApiResult<AssetResponse>> create(@Valid @RequestBody AssetRequest request) {
        AssetResponse created = assetService.create(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}")
                .buildAndExpand(created.id()).toUri();
        return ResponseEntity.created(location).body(ApiResult.ok(created, "Activo registrado"));
    }

    @Operation(summary = "Actualiza un activo")
    @ApiResponse(responseCode = OK, description = "Activo actualizado")
    @PreAuthorize(Authz.CAN_PLAN)
    @PutMapping("/{assetId}")
    public ResponseEntity<ApiResult<AssetResponse>> update(@PathVariable Long assetId,
            @Valid @RequestBody AssetRequest request) {
        return ResponseEntity.ok(ApiResult.ok(assetService.update(assetId, request)));
    }

    @Operation(summary = "Baja logica de un activo")
    @ApiResponse(responseCode = "204", description = "Activo dado de baja")
    @ApiResponse(responseCode = "422", description = "El activo tiene ordenes pendientes")
    @PreAuthorize(Authz.SUPERVISOR)
    @DeleteMapping("/{assetId}")
    public ResponseEntity<Void> delete(@PathVariable Long assetId) {
        assetService.delete(assetId);
        return ResponseEntity.noContent().build();
    }
}
