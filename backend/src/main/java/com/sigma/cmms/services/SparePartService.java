package com.sigma.cmms.services;

import com.sigma.cmms.dto.PageResponse;
import com.sigma.cmms.dto.SparePartDtos.SparePartRequest;
import com.sigma.cmms.dto.SparePartDtos.SparePartResponse;
import com.sigma.cmms.exception.ConflictException;
import com.sigma.cmms.exception.NotFoundException;
import com.sigma.cmms.model.SparePart;
import com.sigma.cmms.repositories.SparePartRepository;
import com.sigma.cmms.repositories.SparePartSpecifications;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Inventario de refacciones. */
@Slf4j
@Service
@RequiredArgsConstructor
public class SparePartService {

    private final SparePartRepository sparePartRepository;

    /**
     * Busca refacciones por SKU o nombre.
     *
     * @param onlyBelowReorder si es verdadero, solo las que requieren reabasto
     */
    @Transactional(readOnly = true)
    public PageResponse<SparePartResponse> search(String text, boolean onlyBelowReorder, Pageable pageable) {
        return PageResponse.from(
                sparePartRepository.findAll(SparePartSpecifications.search(text, onlyBelowReorder), pageable),
                SparePartResponse::from);
    }

    /**
     * Da de alta una refaccion.
     *
     * @throws ConflictException si el SKU ya existe
     */
    @Transactional
    public SparePartResponse create(SparePartRequest request) {
        if (sparePartRepository.existsBySku(request.sku())) {
            throw new ConflictException("Ya existe una refaccion con el SKU " + request.sku());
        }
        SparePart part = new SparePart();
        part.setSku(request.sku());
        part.setName(request.name());
        part.setUnit(request.unit());
        part.setStock(request.stock());
        part.setReorderPoint(request.reorderPoint());
        part.setUnitCost(request.unitCost());
        SparePart saved = sparePartRepository.save(part);
        log.info("Refaccion {} registrada con existencia {}", saved.getSku(), saved.getStock());
        return SparePartResponse.from(saved);
    }

    /**
     * Registra una entrada de almacen. Bloquea la fila para no perder consumos concurrentes.
     *
     * @throws NotFoundException si la refaccion no existe
     */
    @Transactional
    public SparePartResponse restock(Long id, int quantity) {
        SparePart part = sparePartRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new NotFoundException("Refaccion", id));
        part.restock(quantity);
        log.info("Entrada de {} unidades a {}; existencia {}", quantity, part.getSku(), part.getStock());
        return SparePartResponse.from(part);
    }
}
