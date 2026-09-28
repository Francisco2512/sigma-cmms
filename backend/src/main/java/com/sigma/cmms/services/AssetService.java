package com.sigma.cmms.services;

import com.sigma.cmms.dto.AssetDtos.AssetRequest;
import com.sigma.cmms.dto.AssetDtos.AssetResponse;
import com.sigma.cmms.dto.PageResponse;
import com.sigma.cmms.dto.WorkOrderDtos.WorkOrderSummary;
import com.sigma.cmms.exception.BusinessRuleException;
import com.sigma.cmms.exception.ConflictException;
import com.sigma.cmms.exception.NotFoundException;
import com.sigma.cmms.model.Asset;
import com.sigma.cmms.model.AssetStatus;
import com.sigma.cmms.model.WorkOrderStatus;
import com.sigma.cmms.repositories.AssetRepository;
import com.sigma.cmms.repositories.AssetSpecifications;
import com.sigma.cmms.repositories.WorkOrderRepository;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Catalogo de activos y su historial de intervenciones. */
@Slf4j
@Service
@RequiredArgsConstructor
public class AssetService {

    private final AssetRepository assetRepository;
    private final WorkOrderRepository workOrderRepository;
    private final Clock clock;

    /**
     * Busca activos por texto (codigo o nombre), area y estatus.
     *
     * @return pagina de activos; los filtros nulos no restringen
     */
    @Transactional(readOnly = true)
    public PageResponse<AssetResponse> search(String text, String area, AssetStatus status, Pageable pageable) {
        return PageResponse.from(
                assetRepository.findAll(AssetSpecifications.search(text, area, status), pageable),
                AssetResponse::from);
    }

    /**
     * @throws NotFoundException si el activo no existe o fue dado de baja
     */
    @Transactional(readOnly = true)
    public AssetResponse get(Long id) {
        return AssetResponse.from(findEntity(id));
    }

    /**
     * Registra un activo nuevo.
     *
     * @throws ConflictException si el codigo ya existe
     */
    @Transactional
    public AssetResponse create(AssetRequest request) {
        if (assetRepository.existsByCode(request.code())) {
            throw new ConflictException("Ya existe un activo con el codigo " + request.code());
        }
        Asset asset = new Asset();
        apply(asset, request);
        Asset saved = assetRepository.save(asset);
        log.info("Activo {} registrado", saved.getCode());
        return AssetResponse.from(saved);
    }

    /**
     * Actualiza todos los datos del activo.
     *
     * @throws ConflictException si el nuevo codigo ya pertenece a otro activo
     */
    @Transactional
    public AssetResponse update(Long id, AssetRequest request) {
        Asset asset = findEntity(id);
        if (assetRepository.existsByCodeAndIdNot(request.code(), id)) {
            throw new ConflictException("Ya existe un activo con el codigo " + request.code());
        }
        apply(asset, request);
        return AssetResponse.from(asset);
    }

    /**
     * Baja logica. No se permite con ordenes pendientes para no perder trabajo en curso.
     *
     * @throws BusinessRuleException si el activo tiene ordenes pendientes
     */
    @Transactional
    public void delete(Long id) {
        Asset asset = findEntity(id);
        if (workOrderRepository.existsByAssetIdAndStatusIn(id, WorkOrderStatus.pending())) {
            throw new BusinessRuleException("El activo " + asset.getCode() + " tiene ordenes pendientes");
        }
        asset.setDeletedAt(LocalDateTime.now(clock));
        log.info("Activo {} dado de baja", asset.getCode());
    }

    /**
     * Historial de ordenes del activo, de la mas reciente a la mas antigua.
     */
    @Transactional(readOnly = true)
    public PageResponse<WorkOrderSummary> history(Long id, Pageable pageable) {
        findEntity(id);
        LocalDate today = LocalDate.now(clock);
        return PageResponse.from(workOrderRepository.findByAssetIdOrderByCreatedAtDesc(id, pageable),
                wo -> WorkOrderSummary.from(wo, today));
    }

    Asset findEntity(Long id) {
        return assetRepository.findById(id).orElseThrow(() -> new NotFoundException("Activo", id));
    }

    private static void apply(Asset asset, AssetRequest request) {
        asset.setCode(request.code());
        asset.setName(request.name());
        asset.setArea(request.area());
        asset.setLocation(request.location());
        asset.setCriticality(request.criticality());
        if (request.status() != null) {
            asset.setStatus(request.status());
        }
        asset.setManufacturer(request.manufacturer());
        asset.setModel(request.model());
        asset.setCommissionedAt(request.commissionedAt());
    }
}
