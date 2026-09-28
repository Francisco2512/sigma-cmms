import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable, map } from 'rxjs';
import { ApiResult, Kpis } from '../../core/models';

/** Indicadores del tablero. */
@Injectable({ providedIn: 'root' })
export class DashboardService {
  private readonly http = inject(HttpClient);

  /**
   * @param days ventana de observación en días (7 a 365)
   * @returns indicadores del periodo
   */
  getKpis(days: number): Observable<Kpis> {
    return this.http
      .get<ApiResult<Kpis>>('/api/v1/dashboard/kpis', { params: { days } })
      .pipe(map((res) => res.data));
  }
}
