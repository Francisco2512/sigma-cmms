package com.sigma.cmms.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.sigma.cmms.config.SigmaProperties;
import com.sigma.cmms.dto.UserDtos.LoginResponse;
import com.sigma.cmms.dto.UserDtos.UserSummary;
import com.sigma.cmms.exception.TooManyAttemptsException;
import com.sigma.cmms.model.Role;
import com.sigma.cmms.security.SecurityConfig;
import com.sigma.cmms.services.AuthService;
import com.sigma.cmms.services.UserService;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(AuthController.class)
@Import({SecurityConfig.class, AuthControllerTest.TestProperties.class})
class AuthControllerTest {

    private static final String BODY = "{\"username\": \"lhernandez\", \"password\": \"secreta\"}";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AuthService authService;

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

    @Test
    void should_returnToken_when_credentialsAreValid() throws Exception {
        // Arrange
        when(authService.login(any())).thenReturn(new LoginResponse("jwt-token", Instant.parse("2026-09-21T17:00:00Z"),
                new UserSummary(4L, "lhernandez", "Luis Hernandez", Role.TECNICO)));

        // Act + Assert
        mockMvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.token").value("jwt-token"))
                .andExpect(jsonPath("$.data.user.role").value("TECNICO"));
    }

    @Test
    void should_return401_when_credentialsAreInvalid() throws Exception {
        // Arrange
        when(authService.login(any())).thenThrow(new BadCredentialsException("Usuario o contrasena incorrectos"));

        // Act + Assert
        mockMvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.detail").value("Usuario o contrasena incorrectos"));
    }

    @Test
    void should_return429_when_userIsLocked() throws Exception {
        // Arrange
        when(authService.login(any())).thenThrow(new TooManyAttemptsException(5));

        // Act + Assert
        mockMvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isTooManyRequests());
    }

    @Test
    void should_return400_when_passwordIsMissing() throws Exception {
        // Act + Assert
        mockMvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\": \"lhernandez\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.password").exists());
    }
}
