package com.sigma.cmms.model;

import com.sigma.cmms.exception.BusinessRuleException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.SQLRestriction;

/** Refaccion de almacen consumida por las ordenes de trabajo. */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "sigma_spare_parts")
@SQLRestriction("deleted_at IS NULL")
public class SparePart extends BaseEntity {

    @Column(nullable = false, length = 30)
    private String sku;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(nullable = false, length = 16)
    private String unit;

    @Column(nullable = false)
    private Integer stock = 0;

    @Column(name = "reorder_point", nullable = false)
    private Integer reorderPoint = 0;

    @Column(name = "unit_cost", nullable = false, precision = 12, scale = 2)
    private BigDecimal unitCost = BigDecimal.ZERO;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    /** Verdadero cuando la existencia ya no cubre el punto de reorden. */
    public boolean isBelowReorderPoint() {
        return stock <= reorderPoint;
    }

    public void consume(int quantity) {
        if (quantity > stock) {
            throw new BusinessRuleException("Existencia insuficiente de " + sku + ": disponible "
                    + stock + ", solicitado " + quantity);
        }
        stock -= quantity;
    }

    public void restock(int quantity) {
        stock += quantity;
    }
}
