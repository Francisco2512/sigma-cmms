/** Roles del sistema; coinciden con el enum del backend. */
export type Role = 'ADMIN' | 'JEFE_MANTENIMIENTO' | 'PLANIFICADOR' | 'TECNICO' | 'ALMACENISTA';

/** Datos públicos de un usuario. */
export interface UserSummary {
  id: number;
  username: string;
  fullName: string;
  role: Role;
}

/** Respuesta del inicio de sesión. */
export interface LoginResponse {
  token: string;
  expiresAt: string;
  user: UserSummary;
}

/** Etiquetas legibles de cada rol. */
export const ROLE_LABELS: Record<Role, string> = {
  ADMIN: 'Administrador',
  JEFE_MANTENIMIENTO: 'Jefe de mantenimiento',
  PLANIFICADOR: 'Planificador',
  TECNICO: 'Técnico',
  ALMACENISTA: 'Almacenista',
};
