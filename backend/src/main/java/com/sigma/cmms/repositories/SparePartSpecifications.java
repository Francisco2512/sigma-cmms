package com.sigma.cmms.repositories;

import com.sigma.cmms.model.SparePart;
import java.util.Locale;
import org.springframework.data.jpa.domain.Specification;

/** Busqueda de refacciones por texto y filtro de alerta de reorden. */
public final class SparePartSpecifications {

    private SparePartSpecifications() {
    }

    public static Specification<SparePart> search(String text, boolean onlyBelowReorder) {
        return Specification.allOf(matchesText(text), belowReorder(onlyBelowReorder));
    }

    private static Specification<SparePart> matchesText(String text) {
        return (root, query, cb) -> {
            if (text == null || text.isBlank()) {
                return null;
            }
            String like = "%" + text.trim().toLowerCase(Locale.ROOT) + "%";
            return cb.or(cb.like(cb.lower(root.get("sku")), like), cb.like(cb.lower(root.get("name")), like));
        };
    }

    private static Specification<SparePart> belowReorder(boolean only) {
        return (root, query, cb) -> only ? cb.le(root.get("stock"), root.get("reorderPoint")) : null;
    }
}
