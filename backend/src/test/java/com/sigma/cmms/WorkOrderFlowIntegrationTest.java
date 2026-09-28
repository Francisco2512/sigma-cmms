package com.sigma.cmms;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import com.sigma.cmms.model.Asset;
import com.sigma.cmms.model.AssetStatus;
import com.sigma.cmms.model.Criticality;
import com.sigma.cmms.model.PreventivePlan;
import com.sigma.cmms.model.Priority;
import com.sigma.cmms.model.Role;
import com.sigma.cmms.model.SparePart;
import com.sigma.cmms.model.User;
import com.sigma.cmms.repositories.AssetRepository;
import com.sigma.cmms.repositories.PreventivePlanRepository;
import com.sigma.cmms.repositories.SparePartRepository;
import com.sigma.cmms.repositories.UserRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Flujo completo contra MySQL real (base sigma_cmms_test). Se ejecuta con SIGMA_IT=true;
 * en un entorno con Docker se sustituiria la base local por Testcontainers.
 */
@SpringBootTest(properties = {
    "spring.datasource.url=jdbc:mysql://localhost:3306/sigma_cmms_test",
    "spring.flyway.clean-disabled=false",
    "sigma.seed.enabled=false"})
@AutoConfigureMockMvc
@EnabledIfEnvironmentVariable(named = "SIGMA_IT", matches = "true")
class WorkOrderFlowIntegrationTest {

    private static final String PASSWORD = "clave-de-prueba-123";

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private Flyway flyway;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private AssetRepository assetRepository;
    @Autowired
    private SparePartRepository sparePartRepository;
    @Autowired
    private PreventivePlanRepository planRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;

    private Asset asset;
    private SparePart bearing;
    private SparePart seals;
    private User technician;

    @BeforeEach
    void resetDatabase() {
        flyway.clean();
        flyway.migrate();
        String hash = passwordEncoder.encode(PASSWORD);
        userRepository.save(new User("jefe", hash, "Jefa de prueba", Role.JEFE_MANTENIMIENTO));
        technician = userRepository.save(new User("tecnico", hash, "Tecnico de prueba", Role.TECNICO));
        asset = new Asset();
        asset.setCode("CMP-100");
        asset.setName("Compresor de prueba");
        asset.setArea("Utilidades");
        asset.setCriticality(Criticality.ALTA);
        asset = assetRepository.save(asset);
        bearing = sparePartRepository.save(part("ROD-100", 10));
        seals = sparePartRepository.save(part("SEL-100", 1));
    }

    private static SparePart part(String sku, int stock) {
        SparePart part = new SparePart();
        part.setSku(sku);
        part.setName("Refaccion " + sku);
        part.setUnit("pza");
        part.setStock(stock);
        part.setReorderPoint(2);
        part.setUnitCost(new BigDecimal("120.00"));
        return part;
    }

    private String login(String username) throws Exception {
        String body = mockMvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"" + username + "\",\"password\":\"" + PASSWORD + "\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return "Bearer " + JsonPath.read(body, "$.data.token");
    }

    private long createAssignedOrder(String chiefToken) throws Exception {
        String body = mockMvc.perform(post("/api/v1/work-orders").header(HttpHeaders.AUTHORIZATION, chiefToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"assetId": %d, "type": "CORRECTIVA", "priority": "ALTA",
                                 "title": "Perdida de presion", "dueDate": "%s", "assignedToId": %d}
                                """.formatted(asset.getId(), LocalDate.now(), technician.getId())))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(body, "$.data.summary.id")).longValue();
    }

    @Test
    void should_completeOrderLifecycle_when_technicianExecutesAssignedOrder() throws Exception {
        // Arrange
        long orderId = createAssignedOrder(login("jefe"));
        String techToken = login("tecnico");

        // Act
        mockMvc.perform(patch("/api/v1/work-orders/" + orderId + "/start").header(HttpHeaders.AUTHORIZATION, techToken))
                .andExpect(status().isOk());
        mockMvc.perform(patch("/api/v1/work-orders/" + orderId + "/close").header(HttpHeaders.AUTHORIZATION, techToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"laborHours": 2.5, "resolutionNotes": "Cambio de rodamiento",
                                 "parts": [{"sparePartId": %d, "quantity": 3}]}
                                """.formatted(bearing.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.summary.status").value("CERRADA"))
                .andExpect(jsonPath("$.data.partsCost").value(360.0));

        // Assert
        assertThat(sparePartRepository.findById(bearing.getId()).orElseThrow().getStock()).isEqualTo(7);
        assertThat(assetRepository.findById(asset.getId()).orElseThrow().getStatus()).isEqualTo(AssetStatus.OPERATIVO);
    }

    @Test
    void should_rollbackAllConsumption_when_onePartIsInsufficient() throws Exception {
        // Arrange
        long orderId = createAssignedOrder(login("jefe"));
        String techToken = login("tecnico");
        mockMvc.perform(patch("/api/v1/work-orders/" + orderId + "/start").header(HttpHeaders.AUTHORIZATION, techToken))
                .andExpect(status().isOk());

        // Act: el rodamiento alcanza, los sellos no
        mockMvc.perform(patch("/api/v1/work-orders/" + orderId + "/close").header(HttpHeaders.AUTHORIZATION, techToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"laborHours": 1, "resolutionNotes": "Intento de cierre",
                                 "parts": [{"sparePartId": %d, "quantity": 2}, {"sparePartId": %d, "quantity": 5}]}
                                """.formatted(bearing.getId(), seals.getId())))
                .andExpect(status().is(422));

        // Assert: nada se desconto y la orden sigue en proceso
        assertThat(sparePartRepository.findById(bearing.getId()).orElseThrow().getStock()).isEqualTo(10);
        mockMvc.perform(get("/api/v1/work-orders/" + orderId).header(HttpHeaders.AUTHORIZATION, techToken))
                .andExpect(jsonPath("$.data.summary.status").value("EN_PROCESO"));
    }

    @Test
    void should_generatePreventiveOrderOnce_when_generationRunsTwice() throws Exception {
        // Arrange
        PreventivePlan plan = new PreventivePlan();
        plan.setName("Cambio de aceite");
        plan.setAsset(asset);
        plan.setFrequencyDays(30);
        plan.setNextDueDate(LocalDate.now());
        plan.setTaskDescription("Cambiar aceite y filtro");
        plan.setPriority(Priority.MEDIA);
        planRepository.save(plan);
        String chiefToken = login("jefe");

        // Act
        mockMvc.perform(post("/api/v1/preventive-plans/generate").header(HttpHeaders.AUTHORIZATION, chiefToken))
                .andExpect(jsonPath("$.data.generated").value(1));
        mockMvc.perform(post("/api/v1/preventive-plans/generate").header(HttpHeaders.AUTHORIZATION, chiefToken))
                .andExpect(jsonPath("$.data.generated").value(0));

        // Assert
        assertThat(planRepository.findById(plan.getId()).orElseThrow().getNextDueDate())
                .isEqualTo(LocalDate.now().plusDays(30));
    }

    @Test
    void should_return401_when_tokenSignatureIsTampered() throws Exception {
        // Arrange
        String token = login("jefe");
        String tampered = token.substring(0, token.length() - 4) + "AAAA";

        // Act + Assert
        mockMvc.perform(get("/api/v1/assets").header(HttpHeaders.AUTHORIZATION, tampered))
                .andExpect(status().isUnauthorized());
    }
}
