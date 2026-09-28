package com.sigma.cmms.repositories;

import com.sigma.cmms.model.WorkOrder;
import com.sigma.cmms.model.WorkOrderStatus;
import com.sigma.cmms.model.WorkOrderType;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface WorkOrderRepository extends JpaRepository<WorkOrder, Long>, JpaSpecificationExecutor<WorkOrder> {

    /** Carga activo y tecnico en la misma consulta para evitar N+1 en los listados. */
    @Override
    @EntityGraph(attributePaths = {"asset", "assignedTo"})
    Page<WorkOrder> findAll(Specification<WorkOrder> spec, Pageable pageable);

    @EntityGraph(attributePaths = {"asset", "assignedTo", "createdBy", "preventivePlan", "parts", "parts.sparePart"})
    Optional<WorkOrder> findWithDetailsById(Long id);

    @EntityGraph(attributePaths = {"asset", "assignedTo"})
    Page<WorkOrder> findByAssetIdOrderByCreatedAtDesc(Long assetId, Pageable pageable);

    @Query("""
            select new com.sigma.cmms.repositories.RepairInterval(
                a.id, a.code, a.name, coalesce(w.failureAt, w.createdAt), w.closedAt)
            from WorkOrder w join w.asset a
            where w.type = :type and w.status = :status
              and coalesce(w.failureAt, w.createdAt) >= :from
            """)
    List<RepairInterval> findRepairsSince(@Param("type") WorkOrderType type,
            @Param("status") WorkOrderStatus status, @Param("from") LocalDateTime from);

    @Query("""
            select new com.sigma.cmms.repositories.PreventiveDue(w.dueDate, w.status, w.closedAt)
            from WorkOrder w
            where w.type = :type and w.status <> :excluded and w.dueDate between :from and :to
            """)
    List<PreventiveDue> findPreventiveDue(@Param("type") WorkOrderType type,
            @Param("excluded") WorkOrderStatus excluded, @Param("from") LocalDate from, @Param("to") LocalDate to);

    @Query("select new com.sigma.cmms.repositories.StatusCount(w.status, count(w)) from WorkOrder w group by w.status")
    List<StatusCount> countByStatus();

    long countByStatusInAndDueDateBefore(Collection<WorkOrderStatus> statuses, LocalDate date);

    boolean existsByPreventivePlanIdAndDueDate(Long planId, LocalDate dueDate);

    boolean existsByAssetIdAndStatusIn(Long assetId, Collection<WorkOrderStatus> statuses);
}
