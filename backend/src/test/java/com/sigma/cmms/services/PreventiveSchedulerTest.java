package com.sigma.cmms.services;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.sigma.cmms.model.Role;
import com.sigma.cmms.model.User;
import com.sigma.cmms.repositories.UserRepository;
import com.sigma.cmms.support.TestData;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PreventiveSchedulerTest {

    @Mock
    private PreventivePlanService planService;
    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private PreventiveScheduler scheduler;

    @Test
    void should_generateAsAdministrator_when_adminExists() {
        // Arrange
        User admin = TestData.user(1, Role.ADMIN);
        when(userRepository.findFirstByRoleOrderByIdAsc(Role.ADMIN)).thenReturn(Optional.of(admin));

        // Act
        scheduler.generateDaily();

        // Assert
        verify(planService).generateDueOrders(admin);
    }

    @Test
    void should_skipGeneration_when_thereIsNoAdministrator() {
        // Arrange
        when(userRepository.findFirstByRoleOrderByIdAsc(Role.ADMIN)).thenReturn(Optional.empty());

        // Act
        scheduler.generateDaily();

        // Assert
        verify(planService, never()).generateDueOrders(any());
    }
}
