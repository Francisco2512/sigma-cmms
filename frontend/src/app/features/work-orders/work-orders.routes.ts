import { Routes } from '@angular/router';
import { REPORTER_ROLES, roleGuard } from '../../core/auth';

export const WORK_ORDER_ROUTES: Routes = [
  {
    path: '',
    loadComponent: () => import('./work-order-list.component').then((m) => m.WorkOrderListComponent),
    title: 'Órdenes de trabajo · SIGMA',
  },
  {
    path: 'new',
    canActivate: [roleGuard(REPORTER_ROLES)],
    loadComponent: () => import('./work-order-form.component').then((m) => m.WorkOrderFormComponent),
    title: 'Nueva orden · SIGMA',
  },
  {
    path: ':id',
    loadComponent: () => import('./work-order-detail.component').then((m) => m.WorkOrderDetailComponent),
    title: 'Detalle de orden · SIGMA',
  },
];
