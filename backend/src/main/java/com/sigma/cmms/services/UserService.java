package com.sigma.cmms.services;

import com.sigma.cmms.dto.UserDtos.UserSummary;
import com.sigma.cmms.exception.BusinessRuleException;
import com.sigma.cmms.exception.NotFoundException;
import com.sigma.cmms.model.Role;
import com.sigma.cmms.model.User;
import com.sigma.cmms.repositories.UserRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Consulta de usuarios. */
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    /**
     * Lista los usuarios de un rol, ordenados por nombre.
     *
     * @param role rol a consultar
     * @return usuarios del rol
     */
    @Transactional(readOnly = true)
    public List<UserSummary> listByRole(Role role) {
        return userRepository.findByRoleOrderByFullNameAsc(role).stream().map(UserSummary::from).toList();
    }

    /**
     * @param id identificador del usuario
     * @return datos del usuario
     * @throws NotFoundException si no existe
     */
    @Transactional(readOnly = true)
    public UserSummary get(Long id) {
        return UserSummary.from(findEntity(id));
    }

    User findEntity(Long id) {
        return userRepository.findById(id).orElseThrow(() -> new NotFoundException("Usuario", id));
    }

    /** Devuelve el usuario solo si es tecnico; asignar ordenes a otros roles es un error de negocio. */
    User findTechnician(Long id) {
        User user = findEntity(id);
        if (user.getRole() != Role.TECNICO) {
            throw new BusinessRuleException("El usuario " + user.getUsername() + " no es tecnico");
        }
        return user;
    }
}
