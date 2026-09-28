import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable, map } from 'rxjs';
import { ApiResult, GenerationResult, PlanCreate, PlanStatus, PreventivePlan } from '../../core/models';

const BASE = '/api/v1/preventive-plans';

/** Planes de mantenimiento preventivo. */
@Injectable({ providedIn: 'root' })
export class PlansService {
  private readonly http = inject(HttpClient);

  /** @returns planes ordenados por vencimiento */
  list(): Observable<PreventivePlan[]> {
    return this.http.get<ApiResult<PreventivePlan[]>>(BASE).pipe(map((res) => res.data));
  }

  /** @returns el plan creado */
  create(body: PlanCreate): Observable<PreventivePlan> {
    return this.http.post<ApiResult<PreventivePlan>>(BASE, body).pipe(map((res) => res.data));
  }

  /** @returns el plan con su nuevo estatus */
  changeStatus(id: number, status: PlanStatus): Observable<PreventivePlan> {
    return this.http.patch<ApiResult<PreventivePlan>>(`${BASE}/${id}/status`, { status }).pipe(map((res) => res.data));
  }

  /** Genera en este momento las órdenes de los planes vencidos. */
  generate(): Observable<GenerationResult> {
    return this.http.post<ApiResult<GenerationResult>>(`${BASE}/generate`, {}).pipe(map((res) => res.data));
  }
}
