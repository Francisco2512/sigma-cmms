package com.sigma.cmms.services;

import com.sigma.cmms.model.Role;
import com.sigma.cmms.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Ejecucion diaria del generador de ordenes preventivas (RF-03). */
@Slf4j
@Component
@RequiredArgsConstructor
public class PreventiveScheduler {

    private final PreventivePlanService planService;
    private final UserRepository userRepository;

    @Scheduled(cron = "${sigma.preventive.cron}")
    public void generateDaily() {
        userRepository.findFirstByRoleOrderByIdAsc(Role.ADMIN).ifPresentOrElse(
                planService::generateDueOrders,
                () -> log.warn("No hay usuario administrador; se omite la generacion preventiva"));
    }
}
