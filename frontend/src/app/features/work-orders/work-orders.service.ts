import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable, map, shareReplay } from 'rxjs';
import { toParams } from '../../core/http/params';
import {
  ApiResult,
  PageResponse,
  UserSummary,
  WorkOrderClose,
  WorkOrderCreate,
  WorkOrderDetail,
  WorkOrderStatus,
  WorkOrderSummary,
  WorkOrderType,
} from '../../core/models';

/** Filtros del listado de órdenes. */
export interface WorkOrderFilters {
  status?: WorkOrderStatus | '';
  type?: WorkOrderType | '';
  mine?: boolean;
  page?: number;
}

const BASE = '/api/v1/work-orders';

/** Órdenes de trabajo y sus transiciones. */
@Injectable({ providedIn: 'root' })
export class WorkOrdersService {
  private readonly http = inject(HttpClient);
  private technicians$?: Observable<UserSummary[]>;

  /** @returns página de órdenes filtrada, ordenada por fecha compromiso */
  list(filters: WorkOrderFilters): Observable<PageResponse<WorkOrderSummary>> {
    const params = toParams({ ...filters, size: 15, sort: 'dueDate,asc' });
    return this.http.get<ApiResult<PageResponse<WorkOrderSummary>>>(BASE, { params }).pipe(map((res) => res.data));
  }

  /** @returns detalle con refacciones consumidas */
  get(id: number): Observable<WorkOrderDetail> {
    return this.http.get<ApiResult<WorkOrderDetail>>(`${BASE}/${id}`).pipe(map((res) => res.data));
  }

  /** @returns la orden creada */
  create(body: WorkOrderCreate): Observable<WorkOrderDetail> {
    return this.http.post<ApiResult<WorkOrderDetail>>(BASE, body).pipe(map((res) => res.data));
  }

  /** @returns la orden asignada */
  assign(id: number, technicianId: number): Observable<WorkOrderDetail> {
    return this.patch(id, 'assign', { technicianId });
  }

  /** @returns la orden en proceso */
  start(id: number): Observable<WorkOrderDetail> {
    return this.patch(id, 'start', {});
  }

  /** @returns la orden cerrada; 422 si falta existencia de alguna refacción */
  close(id: number, body: WorkOrderClose): Observable<WorkOrderDetail> {
    return this.patch(id, 'close', body);
  }

  /** @returns la orden cancelada */
  cancel(id: number, reason: string): Observable<WorkOrderDetail> {
    return this.patch(id, 'cancel', { reason });
  }

  /** Técnicos disponibles; se consultan una vez y se reutilizan. */
  technicians(): Observable<UserSummary[]> {
    this.technicians$ ??= this.http
      .get<ApiResult<UserSummary[]>>('/api/v1/users', { params: { role: 'TECNICO' } })
      .pipe(
        map((res) => res.data),
        shareReplay({ bufferSize: 1, refCount: true }),
      );
    return this.technicians$;
  }

  private patch(id: number, action: string, body: object): Observable<WorkOrderDetail> {
    return this.http.patch<ApiResult<WorkOrderDetail>>(`${BASE}/${id}/${action}`, body).pipe(map((res) => res.data));
  }
}
