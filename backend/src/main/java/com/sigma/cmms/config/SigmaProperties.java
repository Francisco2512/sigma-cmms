package com.sigma.cmms.config;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

/** Configuracion propia de SIGMA (prefijo {@code sigma.*}). */
@ConfigurationProperties(prefix = "sigma")
public record SigmaProperties(Jwt jwt, Cors cors, Seed seed, Security security, Preventive preventive) {

    public record Jwt(String secret, long expirationMinutes) {
    }

    public record Cors(List<String> allowedOrigins) {
    }

    public record Seed(boolean enabled, String demoPassword) {
    }

    public record Security(int maxLoginAttempts, long lockoutMinutes) {
    }

    public record Preventive(String cron) {
    }
}
