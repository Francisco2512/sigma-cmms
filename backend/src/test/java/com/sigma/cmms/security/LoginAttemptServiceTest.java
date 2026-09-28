package com.sigma.cmms.security;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.sigma.cmms.config.SigmaProperties;
import com.sigma.cmms.exception.TooManyAttemptsException;
import com.sigma.cmms.support.MutableClock;
import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class LoginAttemptServiceTest {

    private MutableClock clock;
    private LoginAttemptService service;

    @BeforeEach
    void setUp() {
        clock = new MutableClock(Instant.parse("2026-09-21T15:00:00Z"));
        SigmaProperties properties = new SigmaProperties(null, null, null, new SigmaProperties.Security(3, 5), null);
        service = new LoginAttemptService(properties, clock);
    }

    @Test
    void should_lockUser_when_maxFailedAttemptsIsReached() {
        // Arrange
        service.registerFailure("lhernandez");
        service.registerFailure("lhernandez");
        service.registerFailure("lhernandez");

        // Act + Assert
        assertThatThrownBy(() -> service.checkNotLocked("lhernandez"))
                .isInstanceOf(TooManyAttemptsException.class);
    }

    @Test
    void should_ignoreUsernameCase_when_countingFailures() {
        // Arrange
        service.registerFailure("LHernandez");
        service.registerFailure("lhernandez ");
        service.registerFailure("LHERNANDEZ");

        // Act + Assert
        assertThatThrownBy(() -> service.checkNotLocked("lhernandez"))
                .isInstanceOf(TooManyAttemptsException.class);
    }

    @Test
    void should_unlockUser_when_lockoutPeriodExpires() {
        // Arrange
        for (int i = 0; i < 3; i++) {
            service.registerFailure("lhernandez");
        }

        // Act
        clock.advance(Duration.ofMinutes(6));

        // Assert
        assertThatCode(() -> service.checkNotLocked("lhernandez")).doesNotThrowAnyException();
    }

    @Test
    void should_resetFailures_when_loginSucceeds() {
        // Arrange
        service.registerFailure("lhernandez");
        service.registerFailure("lhernandez");
        service.registerSuccess("lhernandez");

        // Act
        service.registerFailure("lhernandez");

        // Assert
        assertThatCode(() -> service.checkNotLocked("lhernandez")).doesNotThrowAnyException();
    }
}
