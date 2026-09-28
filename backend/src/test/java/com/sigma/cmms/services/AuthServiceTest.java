package com.sigma.cmms.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.sigma.cmms.dto.UserDtos.LoginRequest;
import com.sigma.cmms.dto.UserDtos.LoginResponse;
import com.sigma.cmms.exception.TooManyAttemptsException;
import com.sigma.cmms.model.Role;
import com.sigma.cmms.model.User;
import com.sigma.cmms.repositories.UserRepository;
import com.sigma.cmms.security.JwtTokenService;
import com.sigma.cmms.security.JwtTokenService.IssuedToken;
import com.sigma.cmms.security.LoginAttemptService;
import com.sigma.cmms.support.TestData;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private JwtTokenService jwtTokenService;
    @Mock
    private LoginAttemptService loginAttemptService;

    @InjectMocks
    private AuthService authService;

    @Test
    void should_issueToken_when_credentialsAreValid() {
        // Arrange
        User user = TestData.user(5, Role.TECNICO);
        when(userRepository.findByUsername("user5")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("secreta", "{hash}")).thenReturn(true);
        when(jwtTokenService.issue(user)).thenReturn(new IssuedToken("jwt", Instant.EPOCH));

        // Act
        LoginResponse response = authService.login(new LoginRequest(" user5 ", "secreta"));

        // Assert
        assertThat(response.token()).isEqualTo("jwt");
        assertThat(response.user().role()).isEqualTo(Role.TECNICO);
        verify(loginAttemptService).registerSuccess("user5");
    }

    @Test
    void should_registerFailure_when_passwordIsWrong() {
        // Arrange
        when(userRepository.findByUsername("user5")).thenReturn(Optional.of(TestData.user(5, Role.TECNICO)));
        when(passwordEncoder.matches("mala", "{hash}")).thenReturn(false);
        LoginRequest request = new LoginRequest("user5", "mala");

        // Act + Assert
        assertThatThrownBy(() -> authService.login(request)).isInstanceOf(BadCredentialsException.class);
        verify(loginAttemptService).registerFailure("user5");
    }

    @Test
    void should_rejectWithSameMessage_when_userDoesNotExist() {
        // Arrange
        when(userRepository.findByUsername("nadie")).thenReturn(Optional.empty());
        LoginRequest request = new LoginRequest("nadie", "x");

        // Act + Assert: mismo mensaje que con contrasena incorrecta, para no revelar usuarios validos
        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessage("Usuario o contrasena incorrectos");
    }

    @Test
    void should_notQueryUser_when_userIsLocked() {
        // Arrange
        doThrow(new TooManyAttemptsException(5)).when(loginAttemptService).checkNotLocked("user5");
        LoginRequest request = new LoginRequest("user5", "secreta");

        // Act + Assert
        assertThatThrownBy(() -> authService.login(request)).isInstanceOf(TooManyAttemptsException.class);
        verify(userRepository, never()).findByUsername("user5");
    }
}
