package com.sigma.cmms.repositories;

import com.sigma.cmms.model.PlanStatus;
import com.sigma.cmms.model.PreventivePlan;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PreventivePlanRepository extends JpaRepository<PreventivePlan, Long> {

    @EntityGraph(attributePaths = "asset")
    List<PreventivePlan> findByStatusAndNextDueDateLessThanEqual(PlanStatus status, LocalDate date);

    @EntityGraph(attributePaths = "asset")
    List<PreventivePlan> findAllByOrderByNextDueDateAsc();

    @EntityGraph(attributePaths = "asset")
    Optional<PreventivePlan> findWithAssetById(Long id);
}
