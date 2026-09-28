package com.sigma.cmms.support;

import com.sigma.cmms.model.Asset;
import com.sigma.cmms.model.Criticality;
import com.sigma.cmms.model.PreventivePlan;
import com.sigma.cmms.model.Priority;
import com.sigma.cmms.model.Role;
import com.sigma.cmms.model.SparePart;
import com.sigma.cmms.model.User;
import com.sigma.cmms.model.WorkOrder;
import com.sigma.cmms.model.WorkOrderType;
import com.sigma.cmms.security.CurrentUser;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;

/** Fabrica de objetos de prueba: cada prueba construye solo lo que necesita. */
public final class TestData {

    /** Reloj fijo: 21/09/2026 15:00 UTC. */
    public static final Clock CLOCK = Clock.fixed(Instant.parse("2026-09-21T15:00:00Z"), ZoneOffset.UTC);
    public static final LocalDate TODAY = LocalDate.now(CLOCK);

    private TestData() {
    }

    public static User user(long id, Role role) {
        User user = new User("user" + id, "{hash}", "Usuario " + id, role);
        user.setId(id);
        return user;
    }

    public static CurrentUser actor(long id, Role role) {
        return new CurrentUser(id, "user" + id, role);
    }

    public static Asset asset(long id) {
        Asset asset = new Asset();
        asset.setId(id);
        asset.setCode("ACT-" + id);
        asset.setName("Activo " + id);
        asset.setArea("Utilidades");
        asset.setCriticality(Criticality.ALTA);
        return asset;
    }

    public static SparePart sparePart(long id, int stock, int reorderPoint) {
        SparePart part = new SparePart();
        part.setId(id);
        part.setSku("SKU-" + id);
        part.setName("Refaccion " + id);
        part.setUnit("pza");
        part.setStock(stock);
        part.setReorderPoint(reorderPoint);
        part.setUnitCost(new BigDecimal("100.00"));
        return part;
    }

    public static WorkOrder workOrder(long id, Asset asset) {
        WorkOrder order = new WorkOrder();
        order.setId(id);
        order.setCode("OT-2026-" + id);
        order.setAsset(asset);
        order.setType(WorkOrderType.CORRECTIVA);
        order.setPriority(Priority.MEDIA);
        order.setTitle("Falla de prueba");
        order.setDueDate(TODAY);
        order.setCreatedBy(user(99, Role.JEFE_MANTENIMIENTO));
        return order;
    }

    /** Orden asignada al tecnico indicado. */
    public static WorkOrder assignedOrder(long id, User technician) {
        WorkOrder order = workOrder(id, asset(1));
        order.assignTo(technician);
        return order;
    }

    /** Orden en proceso por el tecnico indicado. */
    public static WorkOrder inProgressOrder(long id, User technician) {
        WorkOrder order = assignedOrder(id, technician);
        order.start(TODAY.atTime(9, 0));
        return order;
    }

    public static PreventivePlan plan(long id, LocalDate nextDue, int frequencyDays) {
        PreventivePlan plan = new PreventivePlan();
        plan.setId(id);
        plan.setName("Plan " + id);
        plan.setAsset(asset(id));
        plan.setFrequencyDays(frequencyDays);
        plan.setNextDueDate(nextDue);
        plan.setTaskDescription("Rutina");
        plan.setPriority(Priority.MEDIA);
        return plan;
    }
}
