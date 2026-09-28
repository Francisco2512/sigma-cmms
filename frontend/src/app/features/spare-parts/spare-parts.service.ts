import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable, map } from 'rxjs';
import { toParams } from '../../core/http/params';
import { ApiResult, PageResponse, SparePart } from '../../core/models';

const BASE = '/api/v1/spare-parts';

/** Inventario de refacciones. */
@Injectable({ providedIn: 'root' })
export class SparePartsService {
  private readonly http = inject(HttpClient);

  /**
   * @param belowReorder solo las que requieren reabasto
   * @returns página de refacciones ordenada por nombre
   */
  search(search: string, belowReorder: boolean, page: number, size = 20): Observable<PageResponse<SparePart>> {
    const params = toParams({ search, belowReorder, page, size, sort: 'name,asc' });
    return this.http.get<ApiResult<PageResponse<SparePart>>>(BASE, { params }).pipe(map((res) => res.data));
  }

  /** @returns la refacción con la existencia actualizada */
  restock(id: number, quantity: number): Observable<SparePart> {
    return this.http.patch<ApiResult<SparePart>>(`${BASE}/${id}/stock`, { quantity }).pipe(map((res) => res.data));
  }
}
