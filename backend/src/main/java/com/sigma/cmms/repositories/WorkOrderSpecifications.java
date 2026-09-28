package com.sigma.cmms.repositories;

import com.sigma.cmms.model.WorkOrder;
import com.sigma.cmms.model.WorkOrderStatus;
import com.sigma.cmms.model.WorkOrderType;
import org.springframework.data.jpa.domain.Specification;

/** Filtros combinables para el listado de ordenes. Un filtro nulo no restringe. */
public final class WorkOrderSpecifications {

    private WorkOrderSpecifications() {
    }

    public static Specification<WorkOrder> withFilters(WorkOrderStatus status, WorkOrderType type, Long assetId,
            Long assignedToId) {
        return Specification.allOf(
                hasStatus(status), hasType(type), forAsset(assetId), assignedTo(assignedToId));
    }

    private static Specification<WorkOrder> hasStatus(WorkOrderStatus status) {
        return (root, query, cb) -> status == null ? null : cb.equal(root.get("status"), status);
    }

    private static Specification<WorkOrder> hasType(WorkOrderType type) {
        return (root, query, cb) -> type == null ? null : cb.equal(root.get("type"), type);
    }

    private static Specification<WorkOrder> forAsset(Long assetId) {
        return (root, query, cb) -> assetId == null ? null : cb.equal(root.get("asset").get("id"), assetId);
    }

    private static Specification<WorkOrder> assignedTo(Long userId) {
        return (root, query, cb) -> userId == null ? null : cb.equal(root.get("assignedTo").get("id"), userId);
    }
}
