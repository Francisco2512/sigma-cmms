import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable, map } from 'rxjs';
import { toParams } from '../../core/http/params';
import { ApiResult, Asset, AssetStatus, PageResponse, WorkOrderSummary } from '../../core/models';

const BASE = '/api/v1/assets';

/** Catálogo de activos. */
@Injectable({ providedIn: 'root' })
export class AssetsService {
  private readonly http = inject(HttpClient);

  /**
   * @param search texto a buscar en código o nombre
   * @returns página de activos ordenada por código
   */
  search(search: string, status: AssetStatus | '', page: number, size = 15): Observable<PageResponse<Asset>> {
    const params = toParams({ search, status, page, size, sort: 'code,asc' });
    return this.http.get<ApiResult<PageResponse<Asset>>>(BASE, { params }).pipe(map((res) => res.data));
  }

  /** @returns ficha del activo */
  get(id: number): Observable<Asset> {
    return this.http.get<ApiResult<Asset>>(`${BASE}/${id}`).pipe(map((res) => res.data));
  }

  /** @returns historial de órdenes, de la más reciente a la más antigua */
  history(id: number, page: number): Observable<PageResponse<WorkOrderSummary>> {
    return this.http
      .get<ApiResult<PageResponse<WorkOrderSummary>>>(`${BASE}/${id}/work-orders`, { params: { page, size: 10 } })
      .pipe(map((res) => res.data));
  }
}
