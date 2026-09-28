package com.sigma.cmms.security;

import com.sigma.cmms.config.SigmaProperties;
import com.sigma.cmms.model.User;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

/** Emite los tokens de acceso firmados. */
@Service
@RequiredArgsConstructor
public class JwtTokenService {

    public static final String CLAIM_USER_ID = "uid";
    public static final String CLAIM_ROLE = "role";
    public static final String CLAIM_NAME = "name";
    private static final String ISSUER = "sigma-cmms";

    private final JwtEncoder jwtEncoder;
    private final SigmaProperties properties;
    private final Clock clock;

    /** Token emitido junto con su fecha de expiracion. */
    public record IssuedToken(String value, Instant expiresAt) {
    }

    /**
     * Genera un token para el usuario autenticado.
     *
     * @param user usuario que inicio sesion
     * @return token firmado y su expiracion
     */
    public IssuedToken issue(User user) {
        Instant now = clock.instant();
        Instant expiresAt = now.plus(properties.jwt().expirationMinutes(), ChronoUnit.MINUTES);
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(ISSUER)
                .subject(user.getUsername())
                .issuedAt(now)
                .expiresAt(expiresAt)
                .claim(CLAIM_USER_ID, user.getId())
                .claim(CLAIM_ROLE, user.getRole().name())
                .claim(CLAIM_NAME, user.getFullName())
                .build();
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        String token = jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
        return new IssuedToken(token, expiresAt);
    }
}
