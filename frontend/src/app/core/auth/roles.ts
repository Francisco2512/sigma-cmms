import { Role } from '../models';

/** Grupos de roles; reflejan las reglas @PreAuthorize del backend. */
export const PLANNER_ROLES: Role[] = ['ADMIN', 'JEFE_MANTENIMIENTO', 'PLANIFICADOR'];
export const SUPERVISOR_ROLES: Role[] = ['ADMIN', 'JEFE_MANTENIMIENTO'];
export const EXECUTOR_ROLES: Role[] = ['ADMIN', 'JEFE_MANTENIMIENTO', 'TECNICO'];
export const REPORTER_ROLES: Role[] = ['ADMIN', 'JEFE_MANTENIMIENTO', 'PLANIFICADOR', 'TECNICO'];
export const STOCK_ROLES: Role[] = ['ADMIN', 'ALMACENISTA'];
