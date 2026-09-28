package com.sigma.cmms.config;

import com.sigma.cmms.model.Asset;
import com.sigma.cmms.model.Criticality;
import com.sigma.cmms.model.PlanStatus;
import com.sigma.cmms.model.PreventivePlan;
import com.sigma.cmms.model.Priority;
import com.sigma.cmms.model.Role;
import com.sigma.cmms.model.SparePart;
import com.sigma.cmms.model.User;
import com.sigma.cmms.model.WorkOrder;
import com.sigma.cmms.model.WorkOrderType;
import com.sigma.cmms.repositories.AssetRepository;
import com.sigma.cmms.repositories.PreventivePlanRepository;
import com.sigma.cmms.repositories.SparePartRepository;
import com.sigma.cmms.repositories.UserRepository;
import com.sigma.cmms.repositories.WorkOrderRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Carga datos de demostracion en una base vacia: usuarios, activos, refacciones, planes y
 * 120 dias de historial. Usa una semilla fija para que los indicadores sean reproducibles.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "sigma.seed", name = "enabled", havingValue = "true")
public class DataSeeder implements ApplicationRunner {

    private static final int HISTORY_DAYS = 120;
    private static final long SEED = 42L;
    private static final String CHIEF = "mruiz";
    private static final String PLANNER = "jcastillo";
    private static final String TECH_1 = "lhernandez";
    private static final String TECH_2 = "atorres";
    private static final String UTILITIES = "Utilidades";
    private static final String ASSEMBLY = "Ensamble";
    private static final String PIECE = "pza";
    private static final String[] FAILURES = {
        "Paro por alta temperatura", "Fuga de aceite", "Ruido anormal en rodamiento",
        "Falla eléctrica en motor", "Pérdida de presión", "Sensor sin lectura",
        "Disparo de protección térmica", "Vibración excesiva", "Desalineación de transmisión"};

    private final UserRepository userRepository;
    private final AssetRepository assetRepository;
    private final SparePartRepository sparePartRepository;
    private final PreventivePlanRepository planRepository;
    private final WorkOrderRepository workOrderRepository;
    private final PasswordEncoder passwordEncoder;
    private final JdbcTemplate jdbcTemplate;
    private final SigmaProperties properties;
    private final Clock clock;

    private final Random random = new Random(SEED);

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (userRepository.count() > 0) {
            log.info("La base ya tiene datos: se omite la carga de demostracion");
            return;
        }
        String password = properties.seed().demoPassword();
        if (password == null || password.isBlank()) {
            throw new IllegalStateException(
                    "Defina SIGMA_DEMO_PASSWORD en backend/.env para crear los usuarios de demostracion");
        }
        Map<String, User> users = seedUsers(passwordEncoder.encode(password));
        Map<String, Asset> assets = seedAssets();
        Map<String, SparePart> parts = seedSpareParts();
        List<PreventivePlan> plans = seedPlans(assets);
        List<WorkOrder> history = new ArrayList<>();
        history.addAll(correctiveHistory(assets, parts, users));
        history.addAll(preventiveHistory(plans, users));
        persistChronologically(history);
        seedOpenOrders(assets, users);
        log.info("Datos de demostracion cargados: {} usuarios, {} activos, {} refacciones, {} planes, {} ordenes",
                users.size(), assets.size(), parts.size(), plans.size(), workOrderRepository.count());
    }

    private Map<String, User> seedUsers(String hash) {
        Map<String, User> users = new LinkedHashMap<>();
        users.put("admin", new User("admin", hash, "Administrador SIGMA", Role.ADMIN));
        users.put(CHIEF, new User(CHIEF, hash, "Martha Ruiz", Role.JEFE_MANTENIMIENTO));
        users.put(PLANNER, new User(PLANNER, hash, "Jorge Castillo", Role.PLANIFICADOR));
        users.put(TECH_1, new User(TECH_1, hash, "Luis Hernández", Role.TECNICO));
        users.put(TECH_2, new User(TECH_2, hash, "Ana Torres", Role.TECNICO));
        users.put("pgomez", new User("pgomez", hash, "Pedro Gómez", Role.ALMACENISTA));
        userRepository.saveAll(users.values());
        return users;
    }

    private Map<String, Asset> seedAssets() {
        Map<String, Asset> assets = new LinkedHashMap<>();
        addAsset(assets, "CMP-001", "Compresor de tornillo 75 HP", UTILITIES, Criticality.ALTA, "Atlas Copco");
        addAsset(assets, "CMP-002", "Compresor reciprocante 30 HP", UTILITIES, Criticality.MEDIA, "Ingersoll Rand");
        addAsset(assets, "CAL-001", "Caldera pirotubular 150 BHP", UTILITIES, Criticality.ALTA, "Cleaver-Brooks");
        addAsset(assets, "REF-001", "Chiller de agua helada 80 TR", UTILITIES, Criticality.ALTA, "Carrier");
        addAsset(assets, "BTR-001", "Banda transportadora línea 1", ASSEMBLY, Criticality.ALTA, "Hytrol");
        addAsset(assets, "BTR-002", "Banda transportadora línea 2", ASSEMBLY, Criticality.MEDIA, "Hytrol");
        addAsset(assets, "SOL-001", "Robot de soldadura por puntos", ASSEMBLY, Criticality.ALTA, "FANUC");
        addAsset(assets, "MON-001", "Montacargas eléctrico 2.5 t", ASSEMBLY, Criticality.BAJA, "Toyota");
        addAsset(assets, "HID-001", "Prensa hidráulica 200 t", "Estampado", Criticality.ALTA, "Schuler");
        addAsset(assets, "HID-002", "Unidad hidráulica de potencia", "Estampado", Criticality.MEDIA, "Parker");
        addAsset(assets, "TOR-001", "Torno CNC", "Maquinado", Criticality.MEDIA, "Haas");
        addAsset(assets, "REF-002", "Cámara de refrigeración de materiales", "Maquinado", Criticality.BAJA, "Bohn");
        assetRepository.saveAll(assets.values());
        return assets;
    }

    private void addAsset(Map<String, Asset> assets, String code, String name, String area, Criticality criticality,
            String manufacturer) {
        Asset asset = new Asset();
        asset.setCode(code);
        asset.setName(name);
        asset.setArea(area);
        asset.setLocation("Nave " + area);
        asset.setCriticality(criticality);
        asset.setManufacturer(manufacturer);
        asset.setCommissionedAt(LocalDate.now(clock).minusYears(2 + random.nextInt(6)));
        assets.put(code, asset);
    }

    private Map<String, SparePart> seedSpareParts() {
        Map<String, SparePart> parts = new LinkedHashMap<>();
        addPart(parts, "FLT-AIR-075", "Filtro de aire para compresor 75 HP", PIECE, 6, 4, "850.00");
        addPart(parts, "ACE-CMP-20L", "Aceite sintético para compresor 20 L", "cubeta", 3, 2, "4200.00");
        addPart(parts, "ROD-6205", "Rodamiento 6205-2RS", PIECE, 24, 10, "95.50");
        addPart(parts, "BND-A42", "Banda en V A-42", PIECE, 8, 6, "180.00");
        addPart(parts, "KIT-SELL-HID", "Kit de sellos para cilindro hidráulico", "kit", 2, 2, "2350.00");
        addPart(parts, "MNG-HID-12", "Manguera hidráulica 1/2 pulgada, 1 m", PIECE, 5, 4, "420.00");
        addPart(parts, "CNT-3P-32A", "Contactor tripolar 32 A", PIECE, 4, 3, "1150.00");
        addPart(parts, "SNS-IND-M18", "Sensor inductivo M18", PIECE, 3, 4, "690.00");
        addPart(parts, "GRS-EP2", "Grasa EP2 multiusos, cartucho", PIECE, 30, 12, "85.00");
        addPart(parts, "REF-R134A", "Refrigerante R-134a", "kg", 12, 10, "310.00");
        addPart(parts, "ELC-SOLD-CU", "Electrodo de cobre para soldadura por puntos", PIECE, 40, 25, "145.00");
        addPart(parts, "FUS-10A", "Fusible 10 A tipo cartucho", PIECE, 50, 20, "18.50");
        addPart(parts, "VAL-SOL-24V", "Válvula solenoide 24 VCD", PIECE, 1, 2, "1680.00");
        addPart(parts, "TRJ-PLC-ES", "Tarjeta de entradas y salidas para PLC", PIECE, 1, 1, "7400.00");
        sparePartRepository.saveAll(parts.values());
        return parts;
    }

    private static void addPart(Map<String, SparePart> parts, String sku, String name, String unit, int stock,
            int reorderPoint, String cost) {
        SparePart part = new SparePart();
        part.setSku(sku);
        part.setName(name);
        part.setUnit(unit);
        part.setStock(stock);
        part.setReorderPoint(reorderPoint);
        part.setUnitCost(new BigDecimal(cost));
        parts.put(sku, part);
    }

    private List<PreventivePlan> seedPlans(Map<String, Asset> assets) {
        LocalDate today = LocalDate.now(clock);
        List<PreventivePlan> plans = List.of(
                plan("Cambio de filtros y aceite", assets.get("CMP-001"), 30, today, Priority.ALTA,
                        "Cambiar filtro de aire y aceite; revisar separador y drenar condensados."),
                plan("Inspección y purga de caldera", assets.get("CAL-001"), 15, today.minusDays(2), Priority.ALTA,
                        "Purgar fondo, revisar tubos de fuego, probar válvula de seguridad."),
                plan("Lubricación de rodamientos", assets.get("BTR-001"), 14, today.plusDays(5), Priority.MEDIA,
                        "Engrasar chumaceras, verificar tensión y alineación de la banda."),
                plan("Revisión de sellos hidráulicos", assets.get("HID-001"), 60, today.plusDays(20), Priority.ALTA,
                        "Inspeccionar sellos del cilindro principal y nivel de aceite hidráulico."),
                plan("Limpieza de condensador", assets.get("REF-001"), 30, today.plusDays(1), Priority.MEDIA,
                        "Lavar serpentín del condensador y verificar presiones de refrigerante."),
                plan("Calibración de trayectorias", assets.get("SOL-001"), 90, today.plusDays(40), Priority.BAJA,
                        "Verificar puntos de referencia y desgaste de electrodos."));
        plans.get(5).setStatus(PlanStatus.PAUSADO);
        planRepository.saveAll(plans);
        return plans;
    }

    private static PreventivePlan plan(String name, Asset asset, int frequency, LocalDate next, Priority priority,
            String task) {
        PreventivePlan plan = new PreventivePlan();
        plan.setName(name);
        plan.setAsset(asset);
        plan.setFrequencyDays(frequency);
        plan.setNextDueDate(next);
        plan.setPriority(priority);
        plan.setTaskDescription(task);
        return plan;
    }

    private List<WorkOrder> correctiveHistory(Map<String, Asset> assets, Map<String, SparePart> parts,
            Map<String, User> users) {
        Map<String, Integer> failuresPerAsset = Map.ofEntries(
                Map.entry("CMP-001", 6), Map.entry("BTR-001", 5), Map.entry("HID-001", 5),
                Map.entry("SOL-001", 4), Map.entry("CAL-001", 3), Map.entry("REF-001", 3),
                Map.entry("BTR-002", 3), Map.entry("CMP-002", 2), Map.entry("HID-002", 2),
                Map.entry("TOR-001", 2), Map.entry("MON-001", 1), Map.entry("REF-002", 1));
        List<SparePart> catalog = new ArrayList<>(parts.values());
        List<User> technicians = List.of(users.get(TECH_1), users.get(TECH_2));
        LocalDateTime now = LocalDateTime.now(clock).truncatedTo(ChronoUnit.MINUTES);
        List<WorkOrder> orders = new ArrayList<>();
        failuresPerAsset.entrySet().stream().sorted(Map.Entry.comparingByKey()).forEach(entry -> {
            Asset asset = assets.get(entry.getKey());
            for (int i = 0; i < entry.getValue(); i++) {
                LocalDateTime failureAt = now.minusDays(3 + random.nextInt(HISTORY_DAYS - 5))
                        .minusMinutes(random.nextInt(24 * 60));
                double repairHours = repairHours(asset.getCriticality());
                WorkOrder order = baseOrder(asset, WorkOrderType.CORRECTIVA, users.get(CHIEF),
                        FAILURES[random.nextInt(FAILURES.length)], failureAt.toLocalDate().plusDays(1));
                order.setFailureAt(failureAt);
                order.assignTo(technicians.get(random.nextInt(technicians.size())));
                order.start(failureAt.plusMinutes(20 + random.nextInt(70)));
                LocalDateTime closedAt = failureAt.plusMinutes(Math.round(repairHours * 60));
                order.close(closedAt, hours(repairHours * 0.8), "Se corrigió la falla y se probó el equipo");
                if (random.nextInt(3) > 0) {
                    order.addPart(catalog.get(random.nextInt(catalog.size())), 1 + random.nextInt(2));
                }
                orders.add(order);
            }
        });
        return orders;
    }

    private List<WorkOrder> preventiveHistory(List<PreventivePlan> plans, Map<String, User> users) {
        LocalDate limit = LocalDate.now(clock).minusDays(HISTORY_DAYS);
        List<WorkOrder> orders = new ArrayList<>();
        for (PreventivePlan plan : plans) {
            for (LocalDate due = plan.getNextDueDate().minusDays(plan.getFrequencyDays()); !due.isBefore(limit);
                    due = due.minusDays(plan.getFrequencyDays())) {
                WorkOrder order = baseOrder(plan.getAsset(), WorkOrderType.PREVENTIVA, users.get(PLANNER),
                        "Preventivo: " + plan.getName(), due);
                order.setPriority(plan.getPriority());
                order.setDescription(plan.getTaskDescription());
                order.setPreventivePlan(plan);
                order.assignTo(users.get(random.nextBoolean() ? TECH_1 : TECH_2));
                int delayDays = random.nextInt(10) < 8 ? 0 : 1 + random.nextInt(3);
                LocalDateTime start = due.plusDays(delayDays).atTime(8, 0);
                order.start(start);
                double duration = 1.5 + random.nextDouble() * 2.5;
                order.close(start.plusMinutes(Math.round(duration * 60)), hours(duration),
                        "Rutina preventiva completada");
                orders.add(order);
            }
        }
        return orders;
    }

    /** Inserta en orden cronologico para que los folios sigan la fecha real y ajusta created_at. */
    private void persistChronologically(List<WorkOrder> orders) {
        orders.sort(Comparator.comparing(DataSeeder::createdMoment));
        List<Object[]> createdAt = new ArrayList<>();
        for (WorkOrder order : orders) {
            WorkOrder saved = workOrderRepository.save(order);
            saved.setCode(String.format("OT-%d-%05d", createdMoment(saved).getYear(), saved.getId()));
            createdAt.add(new Object[] {Timestamp.valueOf(createdMoment(saved)), saved.getId()});
        }
        workOrderRepository.flush();
        jdbcTemplate.batchUpdate("update sigma_work_orders set created_at = ? where id = ?", createdAt);
    }

    private static LocalDateTime createdMoment(WorkOrder order) {
        return order.getFailureAt() != null ? order.getFailureAt() : order.getDueDate().minusDays(1).atTime(6, 0);
    }

    /** Ordenes vigentes para la demostracion: una en cada estado del flujo, y una vencida. */
    private void seedOpenOrders(Map<String, Asset> assets, Map<String, User> users) {
        LocalDateTime now = LocalDateTime.now(clock).truncatedTo(ChronoUnit.MINUTES);
        LocalDate today = now.toLocalDate();
        WorkOrder open = baseOrder(assets.get("BTR-002"), WorkOrderType.CORRECTIVA, users.get(CHIEF),
                "Vibración excesiva en rodillo motriz", today);
        open.setPriority(Priority.ALTA);
        open.setFailureAt(now.minusHours(3));

        WorkOrder assigned = baseOrder(assets.get("HID-002"), WorkOrderType.CORRECTIVA, users.get(CHIEF),
                "Fuga en conexión de manguera de presión", today.plusDays(1));
        assigned.setFailureAt(now.minusHours(5));
        assigned.assignTo(users.get(TECH_1));

        WorkOrder inProgress = baseOrder(assets.get("CMP-002"), WorkOrderType.CORRECTIVA, users.get(CHIEF),
                "Compresor no alcanza presión de trabajo", today);
        inProgress.setPriority(Priority.ALTA);
        inProgress.setFailureAt(now.minusHours(2));
        inProgress.assignTo(users.get(TECH_1));
        inProgress.start(now.minusHours(1));

        WorkOrder overdue = baseOrder(assets.get("TOR-001"), WorkOrderType.CORRECTIVA, users.get(CHIEF),
                "Alarma de husillo por sobrecarga", today.minusDays(2));
        overdue.setFailureAt(now.minusDays(3));
        overdue.assignTo(users.get(TECH_2));

        for (WorkOrder order : List.of(open, assigned, inProgress, overdue)) {
            WorkOrder saved = workOrderRepository.save(order);
            saved.setCode(String.format("OT-%d-%05d", today.getYear(), saved.getId()));
        }
    }

    private static WorkOrder baseOrder(Asset asset, WorkOrderType type, User createdBy, String title,
            LocalDate dueDate) {
        WorkOrder order = new WorkOrder();
        order.setAsset(asset);
        order.setType(type);
        order.setPriority(Priority.MEDIA);
        order.setTitle(title);
        order.setCreatedBy(createdBy);
        order.setDueDate(dueDate);
        return order;
    }

    private double repairHours(Criticality criticality) {
        return switch (criticality) {
            case ALTA -> 2.0 + random.nextDouble() * 6.0;
            case MEDIA -> 1.5 + random.nextDouble() * 4.5;
            case BAJA -> 1.0 + random.nextDouble() * 3.0;
        };
    }

    private static BigDecimal hours(double value) {
        return BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP);
    }
}
