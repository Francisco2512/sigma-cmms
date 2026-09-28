package com.sigma.cmms.dto;

import com.sigma.cmms.model.Asset;
import com.sigma.cmms.model.AssetStatus;
import com.sigma.cmms.model.Criticality;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

/** DTOs del catalogo de activos. */
public final class AssetDtos {

    private AssetDtos() {
    }

    public record AssetRequest(
            @NotBlank @Pattern(regexp = "[A-Z0-9-]{3,20}", message = "Use de 3 a 20 mayusculas, digitos o guiones")
            String code,
            @NotBlank @Size(max = 120) String name,
            @NotBlank @Size(max = 60) String area,
            @Size(max = 120) String location,
            @NotNull Criticality criticality,
            AssetStatus status,
            @Size(max = 80) String manufacturer,
            @Size(max = 80) String model,
            @PastOrPresent LocalDate commissionedAt) {
    }

    public record AssetResponse(Long id, String code, String name, String area, String location,
            Criticality criticality, AssetStatus status, String manufacturer, String model,
            LocalDate commissionedAt) {

        public static AssetResponse from(Asset asset) {
            return new AssetResponse(asset.getId(), asset.getCode(), asset.getName(), asset.getArea(),
                    asset.getLocation(), asset.getCriticality(), asset.getStatus(), asset.getManufacturer(),
                    asset.getModel(), asset.getCommissionedAt());
        }
    }
}
