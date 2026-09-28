package com.sigma.cmms.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.sigma.cmms.config.SigmaProperties;
import com.sigma.cmms.dto.AssetDtos.AssetResponse;
import com.sigma.cmms.dto.KpiDtos.KpiResponse;
import com.sigma.cmms.dto.PageResponse;
import com.sigma.cmms.dto.PlanDtos.GenerationResult;
import com.sigma.cmms.dto.SparePartDtos.SparePartResponse;
import com.sigma.cmms.dto.UserDtos.UserSummary;
import com.sigma.cmms.exception.BusinessRuleException;
import com.sigma.cmms.model.Role;
import com.sigma.cmms.security.SecurityConfig;
import com.sigma.cmms.services.AssetService;
import com.sigma.cmms.services.KpiService;
import com.sigma.cmms.services.PreventivePlanService;
import com.sigma.cmms.services.SparePartService;
import com.sigma.cmms.services.UserService;
import com.sigma.cmms.support.TestData;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/** Autorizacion por rol y codigos HTTP de los controladores de catalogos, planes e indicadores. */
@WebMvcTest({AssetController.class, SparePartController.class, PreventivePlanController.class,
    DashboardController.class, UserController.class})
@Import({SecurityConfig.class, CatalogControllersTest.TestProperties.class})
class CatalogControllersTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AssetService assetService;
    @MockitoBean
    private SparePartService sparePartService;
    @MockitoBean
    private PreventivePlanService planService;
    @MockitoBean
    private KpiService kpiService;
    @MockitoBean
    private UserService userService;
    @MockitoBean
    private JwtDecoder jwtDecoder;

    @TestConfiguration
    static class TestProperties {
        @Bean
        SigmaProperties sigmaProperties() {
            return new SigmaProperties(null, new SigmaProperties.Cors(List.of("http://localhost:4200")), null,
                    null, null);
        }
    }

    private static JwtRequestPostProcessor as(long id, Role role) {
        return jwt().jwt(token -> token.subject("user" + id).claim("uid", id).claim("role", role.name()))
                .authorities(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }

    private static AssetResponse asset() {
        return AssetResponse.from(TestData.asset(1));
    }

    @Test
    void should_listAssets_when_anyAuthenticatedRoleRequests() throws Exception {
        // Arrange
        when(assetService.search(any(), any(), any(), any()))
                .thenReturn(new PageResponse<>(List.of(asset()), 1, 1, 0, 20));

        // Act + Assert
        mockMvc.perform(get("/api/v1/assets").param("search", "cmp").with(as(6, Role.ALMACENISTA)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].code").value("ACT-1"))
                .andExpect(jsonPath("$.data.totalElements").value(1));
    }

    @Test
    void should_return201_when_plannerRegistersAsset() throws Exception {
        // Arrange
        when(assetService.create(any())).thenReturn(asset());

        // Act + Assert
        mockMvc.perform(post("/api/v1/assets").with(as(3, Role.PLANIFICADOR)).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"ACT-1\",\"name\":\"Compresor\",\"area\":\"Utilidades\",\"criticality\":\"ALTA\"}"))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "http://localhost/api/v1/assets/1"));
    }

    @Test
    void should_return400_when_assetCodeHasInvalidFormat() throws Exception {
        // Act + Assert
        mockMvc.perform(post("/api/v1/assets").with(as(3, Role.PLANIFICADOR)).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"cmp 1\",\"name\":\"Compresor\",\"area\":\"Utilidades\",\"criticality\":\"ALTA\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.code").exists());
        verifyNoInteractions(assetService);
    }

    @Test
    void should_return422_when_deletingAssetWithPendingOrders() throws Exception {
        // Arrange
        doThrow(new BusinessRuleException("El activo ACT-1 tiene ordenes pendientes"))
                .when(assetService).delete(1L);

        // Act + Assert
        mockMvc.perform(delete("/api/v1/assets/1").with(as(2, Role.JEFE_MANTENIMIENTO)))
                .andExpect(status().is(422));
    }

    @Test
    void should_return204_when_supervisorDeletesAsset() throws Exception {
        // Act + Assert
        mockMvc.perform(delete("/api/v1/assets/1").with(as(2, Role.JEFE_MANTENIMIENTO)))
                .andExpect(status().isNoContent());
        verify(assetService).delete(1L);
    }

    @Test
    void should_return403_when_technicianRestocksPart() throws Exception {
        // Act + Assert
        mockMvc.perform(patch("/api/v1/spare-parts/1/stock").with(as(5, Role.TECNICO))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"quantity\": 5}"))
                .andExpect(status().isForbidden());
        verifyNoInteractions(sparePartService);
    }

    @Test
    void should_restock_when_storekeeperRegistersEntry() throws Exception {
        // Arrange
        when(sparePartService.restock(1L, 5)).thenReturn(new SparePartResponse(1L, "ROD-1", "Rodamiento", "pza",
                9, 4, BigDecimal.TEN, false));

        // Act + Assert
        mockMvc.perform(patch("/api/v1/spare-parts/1/stock").with(as(6, Role.ALMACENISTA))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"quantity\": 5}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.stock").value(9));
    }

    @Test
    void should_filterPartsBelowReorder_when_flagIsSent() throws Exception {
        // Arrange
        when(sparePartService.search(any(), anyBoolean(), any())).thenReturn(new PageResponse<>(List.of(), 0, 0, 0, 20));

        // Act
        mockMvc.perform(get("/api/v1/spare-parts").param("belowReorder", "true").with(as(6, Role.ALMACENISTA)))
                .andExpect(status().isOk());

        // Assert
        verify(sparePartService).search(eq(null), eq(true), any());
    }

    @Test
    void should_generateOrders_when_plannerRequestsGeneration() throws Exception {
        // Arrange
        when(planService.generateDueOrdersBy(3L)).thenReturn(new GenerationResult(2, 0, List.of()));

        // Act + Assert
        mockMvc.perform(post("/api/v1/preventive-plans/generate").with(as(3, Role.PLANIFICADOR)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("2 ordenes generadas"));
    }

    @Test
    void should_return403_when_technicianOpensDashboard() throws Exception {
        // Act + Assert
        mockMvc.perform(get("/api/v1/dashboard/kpis").with(as(5, Role.TECNICO)))
                .andExpect(status().isForbidden());
    }

    @Test
    void should_returnKpis_when_chiefOpensDashboard() throws Exception {
        // Arrange
        when(kpiService.compute(30)).thenReturn(new KpiResponse(30, TestData.TODAY.minusDays(30), TestData.TODAY,
                350.0, 4.5, 98.7, 90.0, 6, 3, 1, 2, Map.of("ABIERTA", 1L), List.of()));

        // Act + Assert
        mockMvc.perform(get("/api/v1/dashboard/kpis").param("days", "30").with(as(2, Role.JEFE_MANTENIMIENTO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.mttrHours").value(4.5));
    }

    @Test
    void should_listTechnicians_when_plannerAssignsWork() throws Exception {
        // Arrange
        when(userService.listByRole(Role.TECNICO))
                .thenReturn(List.of(new UserSummary(4L, "lhernandez", "Luis Hernandez", Role.TECNICO)));

        // Act + Assert
        mockMvc.perform(get("/api/v1/users").with(as(3, Role.PLANIFICADOR)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].username").value("lhernandez"));
    }
}
