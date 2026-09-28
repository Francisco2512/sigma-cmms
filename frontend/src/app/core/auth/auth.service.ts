import { HttpClient } from '@angular/common/http';
import { Injectable, computed, inject, signal } from '@angular/core';
import { Router } from '@angular/router';
import { firstValueFrom } from 'rxjs';
import { ApiResult, LoginResponse, Role, UserSummary } from '../models';
import { PLANNER_ROLES } from './roles';

const STORAGE_KEY = 'sigma.session';

interface Session {
  token: string;
  expiresAt: string;
  user: UserSummary;
}

/** Sesión del usuario: inicio y cierre de sesión, token y rol actual. */
@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly http = inject(HttpClient);
  private readonly router = inject(Router);
  private readonly session = signal<Session | null>(this.restore());

  /** Usuario autenticado o `null`. */
  readonly user = computed(() => this.session()?.user ?? null);
  readonly isAuthenticated = computed(() => this.session() !== null);

  /** Token vigente o `null` si no hay sesión. */
  get token(): string | null {
    return this.session()?.token ?? null;
  }

  /**
   * Valida las credenciales y guarda la sesión en el almacenamiento de la pestaña.
   * @param username usuario
   * @param password contraseña
   * @returns el usuario autenticado
   * @throws HttpErrorResponse 401 si las credenciales no son válidas, 429 si está bloqueado
   */
  async login(username: string, password: string): Promise<UserSummary> {
    const response = await firstValueFrom(
      this.http.post<ApiResult<LoginResponse>>('/api/v1/auth/login', { username, password }),
    );
    const { token, expiresAt, user } = response.data;
    const session: Session = { token, expiresAt, user };
    sessionStorage.setItem(STORAGE_KEY, JSON.stringify(session));
    this.session.set(session);
    return user;
  }

  /** Cierra la sesión y regresa al inicio de sesión. */
  logout(): void {
    sessionStorage.removeItem(STORAGE_KEY);
    this.session.set(null);
    void this.router.navigate(['/login']);
  }

  /** @returns verdadero si el usuario tiene alguno de los roles indicados */
  hasAnyRole(roles: readonly Role[]): boolean {
    const role = this.user()?.role;
    return role !== undefined && roles.includes(role);
  }

  /** Pantalla inicial según el rol: tablero para quien planea, órdenes para el resto. */
  homeRoute(): string {
    return this.hasAnyRole(PLANNER_ROLES) ? '/dashboard' : '/work-orders';
  }

  private restore(): Session | null {
    const raw = sessionStorage.getItem(STORAGE_KEY);
    if (!raw) {
      return null;
    }
    try {
      const session = JSON.parse(raw) as Session;
      return new Date(session.expiresAt).getTime() > Date.now() ? session : null;
    } catch {
      return null;
    }
  }
}
