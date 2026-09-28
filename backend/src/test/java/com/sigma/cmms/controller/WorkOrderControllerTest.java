package com.sigma.cmms.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.sigma.cmms.config.SigmaProperties;
import com.sigma.cmms.dto.WorkOrderDtos.WorkOrderDetail;
import com.sigma.cmms.exception.InvalidStateTransitionException;
import com.sigma.cmms.exception.NotFoundException;
import com.sigma.cmms.model.Role;
import com.sigma.cmms.security.SecurityConfig;
import com.sigma.cmms.services.WorkOrderService;
import com.sigma.cmms.support.TestData;
import java.util.List;
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

@WebMvcTest(WorkOrderController.class)
@Import({SecurityConfig.class, WorkOrderControllerTest.TestProperties.class})
class WorkOrderControllerTest {

    private static final String VALID_BODY = """
            {"assetId": 1, "type": "CORRECTIVA", "priority": "ALTA", "title": "Fuga de aceite",
             "dueDate": "2026-09-22"}
            """;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private WorkOrderService workOrderService;

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

    private static WorkOrderDetail detail() {
        return WorkOrderDetail.from(TestData.workOrder(10, TestData.asset(1)), TestData.TODAY);
    }

    @Test
    void should_return401_when_requestHasNoToken() throws Exception {
        // Act + Assert
        mockMvc.perform(get("/api/v1/work-orders/1")).andExpect(status().isUnauthorized());
        verifyNoInteractions(workOrderService);
    }

    @Test
    void should_return201WithLocation_when_chiefCreatesOrder() throws Exception {
        // Arrange
        when(workOrderService.create(any(), any())).thenReturn(detail());

        // Act + Assert
        mockMvc.perform(post("/api/v1/work-orders").with(as(2, Role.JEFE_MANTENIMIENTO))
                        .contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "http://localhost/api/v1/work-orders/10"))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.summary.code").value("OT-2026-10"));
    }

    @Test
    void should_return400WithFieldErrors_when_bodyIsInvalid() throws Exception {
        // Act + Assert
        mockMvc.perform(post("/api/v1/work-orders").with(as(2, Role.JEFE_MANTENIMIENTO))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"title\": \"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Error de validacion"))
                .andExpect(jsonPath("$.errors.assetId").exists())
                .andExpect(jsonPath("$.errors.title").exists());
        verifyNoInteractions(workOrderService);
    }

    @Test
    void should_return403_when_storekeeperCreatesOrder() throws Exception {
        // Act + Assert
        mockMvc.perform(post("/api/v1/work-orders").with(as(6, Role.ALMACENISTA))
                        .contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.title").value("Acceso denegado"));
        verifyNoInteractions(workOrderService);
    }

    @Test
    void should_return404ProblemDetail_when_orderDoesNotExist() throws Exception {
        // Arrange
        when(workOrderService.get(99L)).thenThrow(new NotFoundException("Orden", 99L));

        // Act + Assert
        mockMvc.perform(get("/api/v1/work-orders/99").with(as(5, Role.TECNICO)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.instance").value("/api/v1/work-orders/99"))
                .andExpect(jsonPath("$.trace").doesNotExist());
    }

    @Test
    void should_return409_when_transitionIsInvalid() throws Exception {
        // Arrange
        when(workOrderService.start(eq(1L), any()))
                .thenThrow(new InvalidStateTransitionException("No se puede iniciar una orden en estado CERRADA"));

        // Act + Assert
        mockMvc.perform(patch("/api/v1/work-orders/1/start").with(as(5, Role.TECNICO)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("No se puede iniciar una orden en estado CERRADA"));
    }
}
