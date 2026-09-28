package com.sigma.cmms.security;

import static com.sigma.cmms.support.TestData.CLOCK;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.sigma.cmms.config.SigmaProperties;
import com.sigma.cmms.model.Role;
import com.sigma.cmms.security.JwtTokenService.IssuedToken;
import com.sigma.cmms.support.TestData;
import java.time.Duration;
import javax.crypto.SecretKey;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;

class JwtTokenServiceTest {

    private static final String SECRET = "clave-de-prueba-de-al-menos-32-bytes-para-hs256";

    private final JwtConfig config = new JwtConfig();
    private JwtTokenService service;
    private JwtDecoder decoder;

    private static SigmaProperties properties(String secret) {
        return new SigmaProperties(new SigmaProperties.Jwt(secret, 120), null, null, null, null);
    }

    @BeforeEach
    void setUp() {
        SecretKey key = config.jwtSecretKey(properties(SECRET));
        service = new JwtTokenService(config.jwtEncoder(key), properties(SECRET), CLOCK);
        // Decodificador sin validar expiracion: el reloj de la prueba esta fijo en 2026
        NimbusJwtDecoder nimbus = NimbusJwtDecoder.withSecretKey(key).build();
        nimbus.setJwtValidator(token -> OAuth2TokenValidatorResult.success());
        decoder = nimbus;
    }

    @Test
    void should_embedUserIdAndRole_when_issuingToken() {
        // Act
        IssuedToken token = service.issue(TestData.user(4, Role.TECNICO));
        Jwt jwt = decoder.decode(token.value());

        // Assert
        assertThat(jwt.getSubject()).isEqualTo("user4");
        assertThat(jwt.getClaimAsString(JwtTokenService.CLAIM_ROLE)).isEqualTo("TECNICO");
        assertThat(CurrentUser.from(jwt)).isEqualTo(new CurrentUser(4L, "user4", Role.TECNICO));
    }

    @Test
    void should_expireAfterConfiguredMinutes_when_issuingToken() {
        // Act
        IssuedToken token = service.issue(TestData.user(4, Role.TECNICO));

        // Assert
        assertThat(Duration.between(CLOCK.instant(), token.expiresAt())).isEqualTo(Duration.ofMinutes(120));
    }

    @Test
    void should_rejectToken_when_signedWithAnotherKey() {
        // Arrange
        SecretKey otherKey = config.jwtSecretKey(properties(""));
        JwtTokenService otherService = new JwtTokenService(config.jwtEncoder(otherKey), properties(SECRET), CLOCK);
        String foreign = otherService.issue(TestData.user(4, Role.ADMIN)).value();

        // Act + Assert
        assertThatThrownBy(() -> decoder.decode(foreign)).isInstanceOf(JwtException.class);
    }

    @Test
    void should_failFast_when_secretIsTooShort() {
        // Act + Assert
        assertThatThrownBy(() -> config.jwtSecretKey(properties("corta")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("32 bytes");
    }
}
