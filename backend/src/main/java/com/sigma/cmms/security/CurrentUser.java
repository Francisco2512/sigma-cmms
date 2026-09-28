package com.sigma.cmms.security;

import com.sigma.cmms.model.Role;
import org.springframework.security.oauth2.jwt.Jwt;

/** Usuario autenticado, reconstruido a partir de las claims del token. */
public record CurrentUser(Long id, String username, Role role) {

    public static CurrentUser from(Jwt jwt) {
        Number uid = jwt.getClaim(JwtTokenService.CLAIM_USER_ID);
        return new CurrentUser(uid.longValue(), jwt.getSubject(),
                Role.valueOf(jwt.getClaimAsString(JwtTokenService.CLAIM_ROLE)));
    }

    public boolean hasRole(Role expected) {
        return role == expected;
    }
}
