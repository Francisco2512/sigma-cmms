package com.sigma.cmms.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Refaccion consumida por una orden. Conserva el costo unitario del momento del consumo. */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "sigma_work_order_parts")
public class WorkOrderPart extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "work_order_id", nullable = false)
    private WorkOrder workOrder;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "spare_part_id", nullable = false)
    private SparePart sparePart;

    @Column(nullable = false)
    private Integer quantity;

    @Column(name = "unit_cost", nullable = false, precision = 12, scale = 2)
    private BigDecimal unitCost;

    public WorkOrderPart(WorkOrder workOrder, SparePart sparePart, int quantity, BigDecimal unitCost) {
        this.workOrder = workOrder;
        this.sparePart = sparePart;
        this.quantity = quantity;
        this.unitCost = unitCost;
    }

    /** @return costo de la linea: cantidad por costo unitario */
    public BigDecimal subtotal() {
        return unitCost.multiply(BigDecimal.valueOf(quantity));
    }
}
