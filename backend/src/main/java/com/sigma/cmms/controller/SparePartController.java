package com.sigma.cmms.controller;

import com.sigma.cmms.dto.ApiResult;
import com.sigma.cmms.dto.PageResponse;
import com.sigma.cmms.dto.SparePartDtos.SparePartRequest;
import com.sigma.cmms.dto.SparePartDtos.SparePartResponse;
import com.sigma.cmms.dto.SparePartDtos.StockEntryRequest;
import com.sigma.cmms.security.Authz;
import com.sigma.cmms.services.SparePartService;
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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

/** Inventario de refacciones (RF-06). */
@Tag(name = "Refacciones")
@RestController
@RequestMapping("/api/v1/spare-parts")
@RequiredArgsConstructor
public class SparePartController {

    private final SparePartService sparePartService;

    @Operation(summary = "Busca refacciones; belowReorder=true devuelve solo las que requieren reabasto")
    @ApiResponse(responseCode = "200", description = "Pagina de refacciones")
    @GetMapping
    public ResponseEntity<ApiResult<PageResponse<SparePartResponse>>> search(
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "false") boolean belowReorder,
            @PageableDefault(size = 20, sort = "name", direction = Sort.Direction.ASC) Pageable pageable) {
        return ResponseEntity.ok(ApiResult.ok(sparePartService.search(search, belowReorder, pageable)));
    }

    @Operation(summary = "Da de alta una refaccion")
    @ApiResponse(responseCode = "201", description = "Refaccion creada")
    @ApiResponse(responseCode = "409", description = "SKU duplicado")
    @PreAuthorize(Authz.CAN_STOCK)
    @PostMapping
    public ResponseEntity<ApiResult<SparePartResponse>> create(@Valid @RequestBody SparePartRequest request) {
        SparePartResponse created = sparePartService.create(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}")
                .buildAndExpand(created.id()).toUri();
        return ResponseEntity.created(location).body(ApiResult.ok(created, "Refaccion registrada"));
    }

    @Operation(summary = "Registra una entrada de almacen")
    @ApiResponse(responseCode = "200", description = "Existencia actualizada")
    @PreAuthorize(Authz.CAN_STOCK)
    @PatchMapping("/{sparePartId}/stock")
    public ResponseEntity<ApiResult<SparePartResponse>> restock(@PathVariable Long sparePartId,
            @Valid @RequestBody StockEntryRequest request) {
        return ResponseEntity.ok(ApiResult.ok(sparePartService.restock(sparePartId, request.quantity())));
    }
}
