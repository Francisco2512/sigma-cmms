package com.sigma.cmms.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.SQLRestriction;
import org.hibernate.type.SqlTypes;

/** Equipo fisico sujeto a mantenimiento. */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "sigma_assets")
@SQLRestriction("deleted_at IS NULL")
public class Asset extends BaseEntity {

    @Column(nullable = false, length = 20)
    private String code;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(nullable = false, length = 60)
    private String area;

    @Column(length = 120)
    private String location;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 32)
    private Criticality criticality;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 32)
    private AssetStatus status = AssetStatus.OPERATIVO;

    @Column(length = 80)
    private String manufacturer;

    @Column(length = 80)
    private String model;

    @Column(name = "commissioned_at")
    private LocalDate commissionedAt;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;
}
