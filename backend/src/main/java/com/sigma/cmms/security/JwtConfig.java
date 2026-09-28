package com.sigma.cmms.security;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import com.sigma.cmms.config.SigmaProperties;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

/** Claves y codificadores JWT (HMAC-SHA256). */
@Slf4j
@Configuration
public class JwtConfig {

    private static final int MIN_KEY_BYTES = 32;

    @Bean
    public SecretKey jwtSecretKey(SigmaProperties properties) {
        String secret = properties.jwt().secret();
        byte[] keyBytes;
        if (secret == null || secret.isBlank()) {
            keyBytes = new byte[MIN_KEY_BYTES];
            new SecureRandom().nextBytes(keyBytes);
            log.warn("SIGMA_JWT_SECRET no esta configurado: se genero una clave aleatoria. "
                    + "Los tokens emitidos se invalidan al reiniciar el servidor.");
        } else {
            keyBytes = secret.getBytes(StandardCharsets.UTF_8);
            if (keyBytes.length < MIN_KEY_BYTES) {
                throw new IllegalStateException("SIGMA_JWT_SECRET debe tener al menos 32 bytes");
            }
        }
        return new SecretKeySpec(keyBytes, "HmacSHA256");
    }

    @Bean
    public JwtEncoder jwtEncoder(SecretKey jwtSecretKey) {
        return new NimbusJwtEncoder(new ImmutableSecret<>(jwtSecretKey));
    }

    @Bean
    public JwtDecoder jwtDecoder(SecretKey jwtSecretKey) {
        return NimbusJwtDecoder.withSecretKey(jwtSecretKey).macAlgorithm(MacAlgorithm.HS256).build();
    }
}
