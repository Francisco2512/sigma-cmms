import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { IconComponent, IconName } from '../../shared';
import { AuthService, PLANNER_ROLES } from '../auth';
import { ROLE_LABELS, Role } from '../models';

interface NavItem {
  path: string;
  label: string;
  icon: IconName;
  roles?: readonly Role[];
}

const NAV: NavItem[] = [
  { path: '/dashboard', label: 'Tablero', icon: 'dashboard', roles: PLANNER_ROLES },
  { path: '/work-orders', label: 'Órdenes de trabajo', icon: 'orders' },
  { path: '/assets', label: 'Activos', icon: 'assets' },
  { path: '/spare-parts', label: 'Refacciones', icon: 'parts' },
  { path: '/preventive-plans', label: 'Planes preventivos', icon: 'plans', roles: PLANNER_ROLES },
];

/** Estructura de la aplicación autenticada: menú lateral por rol y área de contenido. */
@Component({
  selector: 'app-shell',
  imports: [RouterOutlet, RouterLink, RouterLinkActive, IconComponent],
  templateUrl: './shell.component.html',
  styleUrl: './shell.component.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ShellComponent {
  protected readonly auth = inject(AuthService);
  protected readonly menuOpen = signal(false);
  protected readonly items = computed(() => NAV.filter((item) => !item.roles || this.auth.hasAnyRole(item.roles)));
  protected readonly roleLabel = computed(() => {
    const role = this.auth.user()?.role;
    return role ? ROLE_LABELS[role] : '';
  });
  protected readonly initials = computed(() =>
    (this.auth.user()?.fullName ?? '')
      .split(' ')
      .slice(0, 2)
      .map((part) => part.charAt(0))
      .join(''),
  );

  protected toggleMenu(): void {
    this.menuOpen.update((open) => !open);
  }
}
