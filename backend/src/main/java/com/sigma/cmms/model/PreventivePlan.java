package com.sigma.cmms.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.SQLRestriction;
import org.hibernate.type.SqlTypes;

/** Plan de mantenimiento preventivo con frecuencia por calendario. */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "sigma_preventive_plans")
@SQLRestriction("deleted_at IS NULL")
public class PreventivePlan extends BaseEntity {

    @Column(nullable = false, length = 120)
    private String name;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "asset_id", nullable = false)
    private Asset asset;

    @Column(name = "frequency_days", nullable = false)
    private Integer frequencyDays;

    @Column(name = "next_due_date", nullable = false)
    private LocalDate nextDueDate;

    @Column(name = "task_description", nullable = false, length = 1000)
    private String taskDescription;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 32)
    private Priority priority;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 32)
    private PlanStatus status = PlanStatus.ACTIVO;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    /**
     * Mueve el vencimiento al primer ciclo posterior a {@code today}. Si hubo ciclos
     * vencidos sin atender no se generan ordenes atrasadas en cascada: se cubre el actual.
     *
     * @return numero de ciclos que se omitieron por estar vencidos
     */
    public int advancePast(LocalDate today) {
        int skipped = -1;
        while (!nextDueDate.isAfter(today)) {
            nextDueDate = nextDueDate.plusDays(frequencyDays);
            skipped++;
        }
        return Math.max(skipped, 0);
    }

    public boolean isDue(LocalDate today) {
        return status == PlanStatus.ACTIVO && !nextDueDate.isAfter(today);
    }
}
