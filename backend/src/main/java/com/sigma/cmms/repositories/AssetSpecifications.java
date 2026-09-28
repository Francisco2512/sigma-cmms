package com.sigma.cmms.repositories;

import com.sigma.cmms.model.Asset;
import com.sigma.cmms.model.AssetStatus;
import java.util.Locale;
import org.springframework.data.jpa.domain.Specification;

/** Busqueda de activos por texto libre, area y estatus. */
public final class AssetSpecifications {

    private AssetSpecifications() {
    }

    /**
     * @param text texto libre a buscar en codigo o nombre
     * @param area area a filtrar
     * @param status estatus a filtrar
     * @return filtro combinado; los criterios nulos no restringen
     */
    public static Specification<Asset> search(String text, String area, AssetStatus status) {
        return Specification.allOf(matchesText(text), inArea(area), hasStatus(status));
    }

    private static Specification<Asset> matchesText(String text) {
        return (root, query, cb) -> {
            if (text == null || text.isBlank()) {
                return null;
            }
            String like = "%" + text.trim().toLowerCase(Locale.ROOT) + "%";
            return cb.or(cb.like(cb.lower(root.get("code")), like), cb.like(cb.lower(root.get("name")), like));
        };
    }

    private static Specification<Asset> inArea(String area) {
        return (root, query, cb) -> area == null || area.isBlank() ? null : cb.equal(root.get("area"), area);
    }

    private static Specification<Asset> hasStatus(AssetStatus status) {
        return (root, query, cb) -> status == null ? null : cb.equal(root.get("status"), status);
    }
}
