package com.sigma.cmms.services;

import com.sigma.cmms.dto.UserDtos.LoginRequest;
import com.sigma.cmms.dto.UserDtos.LoginResponse;
import com.sigma.cmms.dto.UserDtos.UserSummary;
import com.sigma.cmms.model.User;
import com.sigma.cmms.repositories.UserRepository;
import com.sigma.cmms.security.JwtTokenService;
import com.sigma.cmms.security.JwtTokenService.IssuedToken;
import com.sigma.cmms.security.LoginAttemptService;
import com.sigma.cmms.util.LogSanitizer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Autenticacion de usuarios y emision de tokens. */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private static final String INVALID_CREDENTIALS = "Usuario o contrasena incorrectos";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenService jwtTokenService;
    private final LoginAttemptService loginAttemptService;

    /**
     * Valida credenciales y emite un token de acceso.
     *
     * @param request usuario y contrasena
     * @return token, expiracion y datos del usuario
     * @throws BadCredentialsException si las credenciales no son validas
     * @throws com.sigma.cmms.exception.TooManyAttemptsException si el usuario esta bloqueado
     */
    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest request) {
        String username = request.username().trim();
        loginAttemptService.checkNotLocked(username);
        User user = userRepository.findByUsername(username)
                .filter(candidate -> passwordEncoder.matches(request.password(), candidate.getPasswordHash()))
                .orElseThrow(() -> rejectLogin(username));
        loginAttemptService.registerSuccess(username);
        log.info("Inicio de sesion exitoso: {} ({})", user.getUsername(), user.getRole());
        IssuedToken token = jwtTokenService.issue(user);
        return new LoginResponse(token.value(), token.expiresAt(), UserSummary.from(user));
    }

    private BadCredentialsException rejectLogin(String username) {
        loginAttemptService.registerFailure(username);
        log.warn("Inicio de sesion fallido para {}", LogSanitizer.clean(username));
        return new BadCredentialsException(INVALID_CREDENTIALS);
    }
}
