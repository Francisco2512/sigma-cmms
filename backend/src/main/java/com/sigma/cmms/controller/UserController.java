package com.sigma.cmms.controller;

import com.sigma.cmms.dto.ApiResult;
import com.sigma.cmms.dto.UserDtos.UserSummary;
import com.sigma.cmms.model.Role;
import com.sigma.cmms.security.Authz;
import com.sigma.cmms.services.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Consulta de usuarios para asignacion de ordenes. */
@Tag(name = "Usuarios")
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @Operation(summary = "Lista usuarios por rol")
    @ApiResponse(responseCode = "200", description = "Usuarios del rol")
    @PreAuthorize(Authz.CAN_PLAN)
    @GetMapping
    public ResponseEntity<ApiResult<List<UserSummary>>> list(@RequestParam(defaultValue = "TECNICO") Role role) {
        return ResponseEntity.ok(ApiResult.ok(userService.listByRole(role)));
    }
}
