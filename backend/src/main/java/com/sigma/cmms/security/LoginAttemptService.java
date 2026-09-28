package com.sigma.cmms.security;

import com.sigma.cmms.config.SigmaProperties;
import com.sigma.cmms.exception.TooManyAttemptsException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Bloquea temporalmente un usuario tras varios intentos fallidos, para frenar ataques
 * de fuerza bruta sobre el endpoint publico de login. Estado en memoria: suficiente
 * para una sola instancia; con varias instancias deberia moverse a Redis.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LoginAttemptService {

    private final SigmaProperties properties;
    private final Clock clock;
    private final Map<String, Attempts> attempts = new ConcurrentHashMap<>();

    private record Attempts(int failures, Instant lockedUntil) {
    }

    /**
     * Verifica que el usuario no este bloqueado.
     *
     * @param username usuario que intenta iniciar sesion
     * @throws TooManyAttemptsException si el usuario esta bloqueado
     */
    public void checkNotLocked(String username) {
        Attempts current = attempts.get(key(username));
        if (current != null && current.lockedUntil() != null && clock.instant().isBefore(current.lockedUntil())) {
            throw new TooManyAttemptsException(properties.security().lockoutMinutes());
        }
    }

    /** Registra un intento fallido y bloquea al alcanzar el maximo configurado. */
    public void registerFailure(String username) {
        attempts.compute(key(username), (k, prev) -> {
            int failures = (prev == null || isExpiredLock(prev) ? 0 : prev.failures()) + 1;
            if (failures >= properties.security().maxLoginAttempts()) {
                log.warn("Usuario {} bloqueado por {} intentos fallidos", username, failures);
                return new Attempts(failures, clock.instant()
                        .plus(Duration.ofMinutes(properties.security().lockoutMinutes())));
            }
            return new Attempts(failures, null);
        });
    }

    /**
     * Limpia el conteo de intentos tras un inicio de sesion correcto.
     *
     * @param username usuario que inicio sesion
     */
    public void registerSuccess(String username) {
        attempts.remove(key(username));
    }

    private boolean isExpiredLock(Attempts prev) {
        return prev.lockedUntil() != null && !clock.instant().isBefore(prev.lockedUntil());
    }

    private static String key(String username) {
        return username.trim().toLowerCase(Locale.ROOT);
    }
}
