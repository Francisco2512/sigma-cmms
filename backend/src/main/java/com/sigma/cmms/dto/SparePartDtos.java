package com.sigma.cmms.dto;

import com.sigma.cmms.model.SparePart;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

/** DTOs del inventario de refacciones. */
public final class SparePartDtos {

    private SparePartDtos() {
    }

    public record SparePartRequest(
            @NotBlank @Pattern(regexp = "[A-Z0-9-]{3,30}", message = "Use de 3 a 30 mayusculas, digitos o guiones")
            String sku,
            @NotBlank @Size(max = 120) String name,
            @NotBlank @Size(max = 16) String unit,
            @NotNull @Min(0) @Max(1_000_000) Integer stock,
            @NotNull @Min(0) @Max(1_000_000) Integer reorderPoint,
            @NotNull @DecimalMin("0.00") @Digits(integer = 10, fraction = 2) BigDecimal unitCost) {
    }

    public record StockEntryRequest(@NotNull @Min(1) @Max(100_000) Integer quantity) {
    }

    public record SparePartResponse(Long id, String sku, String name, String unit, int stock,
            int reorderPoint, BigDecimal unitCost, boolean belowReorderPoint) {

        public static SparePartResponse from(SparePart part) {
            return new SparePartResponse(part.getId(), part.getSku(), part.getName(), part.getUnit(),
                    part.getStock(), part.getReorderPoint(), part.getUnitCost(), part.isBelowReorderPoint());
        }
    }
}
