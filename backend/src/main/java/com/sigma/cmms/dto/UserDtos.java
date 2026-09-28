package com.sigma.cmms.dto;

import com.sigma.cmms.model.Role;
import com.sigma.cmms.model.User;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.Instant;

/** DTOs de autenticacion y usuarios. */
public final class UserDtos {

    private UserDtos() {
    }

    public record LoginRequest(
            @NotBlank @Size(max = 50) String username,
            @NotBlank @Size(max = 100) String password) {
    }

    public record UserSummary(Long id, String username, String fullName, Role role) {

        public static UserSummary from(User user) {
            return new UserSummary(user.getId(), user.getUsername(), user.getFullName(), user.getRole());
        }
    }

    public record LoginResponse(String token, Instant expiresAt, UserSummary user) {
    }
}
