package com.sigma.cmms.controller;

import com.sigma.cmms.dto.ApiResult;
import com.sigma.cmms.dto.UserDtos.LoginRequest;
import com.sigma.cmms.dto.UserDtos.LoginResponse;
import com.sigma.cmms.dto.UserDtos.UserSummary;
import com.sigma.cmms.security.CurrentUser;
import com.sigma.cmms.services.AuthService;
import com.sigma.cmms.services.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Inicio de sesion y datos del usuario autenticado. */
@Tag(name = "Autenticacion")
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final UserService userService;

    @Operation(summary = "Inicia sesion y devuelve un token JWT")
    @ApiResponse(responseCode = "200", description = "Credenciales validas")
    @ApiResponse(responseCode = "401", description = "Credenciales invalidas")
    @ApiResponse(responseCode = "429", description = "Usuario bloqueado por intentos fallidos")
    @PostMapping("/login")
    public ResponseEntity<ApiResult<LoginResponse>> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(ApiResult.ok(authService.login(request)));
    }

    @Operation(summary = "Datos del usuario autenticado")
    @ApiResponse(responseCode = "200", description = "Usuario actual")
    @GetMapping("/me")
    public ResponseEntity<ApiResult<UserSummary>> me(@AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(ApiResult.ok(userService.get(CurrentUser.from(jwt).id())));
    }
}
