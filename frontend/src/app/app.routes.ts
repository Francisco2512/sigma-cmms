import { inject } from '@angular/core';
import { Routes } from '@angular/router';
import { AuthService, PLANNER_ROLES, authGuard, roleGuard } from './core/auth';
import { ShellComponent } from './core/layout/shell.component';

export const routes: Routes = [
  {
    path: 'login',
    loadComponent: () => import('./features/login').then((m) => m.LoginComponent),
    title: 'Iniciar sesión · SIGMA',
  },
  {
    path: '',
    component: ShellComponent,
    canActivate: [authGuard],
    children: [
      { path: '', pathMatch: 'full', redirectTo: () => inject(AuthService).homeRoute() },
      {
        path: 'dashboard',
        canActivate: [roleGuard(PLANNER_ROLES)],
        loadComponent: () => import('./features/dashboard').then((m) => m.DashboardComponent),
        title: 'Tablero · SIGMA',
      },
      { path: 'work-orders', loadChildren: () => import('./features/work-orders').then((m) => m.WORK_ORDER_ROUTES) },
      { path: 'assets', loadChildren: () => import('./features/assets').then((m) => m.ASSET_ROUTES) },
      {
        path: 'spare-parts',
        loadComponent: () => import('./features/spare-parts').then((m) => m.SparePartListComponent),
        title: 'Refacciones · SIGMA',
      },
      {
        path: 'preventive-plans',
        canActivate: [roleGuard(PLANNER_ROLES)],
        loadComponent: () => import('./features/preventive-plans').then((m) => m.PlanListComponent),
        title: 'Planes preventivos · SIGMA',
      },
    ],
  },
  { path: '**', redirectTo: '' },
];
