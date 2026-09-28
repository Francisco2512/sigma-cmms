import { Routes } from '@angular/router';

export const ASSET_ROUTES: Routes = [
  {
    path: '',
    loadComponent: () => import('./asset-list.component').then((m) => m.AssetListComponent),
    title: 'Activos · SIGMA',
  },
  {
    path: ':id',
    loadComponent: () => import('./asset-detail.component').then((m) => m.AssetDetailComponent),
    title: 'Ficha de activo · SIGMA',
  },
];
