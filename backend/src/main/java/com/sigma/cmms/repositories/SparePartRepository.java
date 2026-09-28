package com.sigma.cmms.repositories;

import com.sigma.cmms.model.SparePart;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SparePartRepository extends JpaRepository<SparePart, Long>, JpaSpecificationExecutor<SparePart> {

    boolean existsBySku(String sku);

    /** SELECT ... FOR UPDATE: serializa consumos concurrentes de la misma refaccion. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from SparePart p where p.id = :id")
    Optional<SparePart> findByIdForUpdate(@Param("id") Long id);

    @Query("select count(p) from SparePart p where p.stock <= p.reorderPoint")
    long countBelowReorderPoint();
}
