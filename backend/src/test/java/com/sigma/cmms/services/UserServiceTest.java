package com.sigma.cmms.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.sigma.cmms.exception.BusinessRuleException;
import com.sigma.cmms.exception.NotFoundException;
import com.sigma.cmms.model.Role;
import com.sigma.cmms.repositories.UserRepository;
import com.sigma.cmms.support.TestData;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserService service;

    @Test
    void should_rejectAssignment_when_userIsNotTechnician() {
        // Arrange
        when(userRepository.findById(6L)).thenReturn(Optional.of(TestData.user(6, Role.ALMACENISTA)));

        // Act + Assert
        assertThatThrownBy(() -> service.findTechnician(6L))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("no es tecnico");
    }

    @Test
    void should_throwNotFound_when_userDoesNotExist() {
        // Arrange
        when(userRepository.findById(9L)).thenReturn(Optional.empty());

        // Act + Assert
        assertThatThrownBy(() -> service.get(9L)).isInstanceOf(NotFoundException.class);
    }

    @Test
    void should_listUsersOfRole_when_requested() {
        // Arrange
        when(userRepository.findByRoleOrderByFullNameAsc(Role.TECNICO))
                .thenReturn(List.of(TestData.user(4, Role.TECNICO), TestData.user(5, Role.TECNICO)));

        // Act + Assert
        assertThat(service.listByRole(Role.TECNICO)).hasSize(2);
    }
}
