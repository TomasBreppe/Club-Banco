import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';
import {
  BaseResponse,
  DashboardIngresosResponse,
} from '../models/ingresos.model';

@Injectable({
  providedIn: 'root',
})
export class DashboardIngresosService {
  private http = inject(HttpClient);
  private base = environment.apiUrl;

  obtenerDashboard(filtros?: {
    disciplinaId?: number | null;
    categoriaManual?: string | null;
    fechaDesde?: string | null;
    fechaHasta?: string | null;
    q?: string | null;
  }): Observable<BaseResponse<DashboardIngresosResponse>> {
    let params = new HttpParams();

    if (filtros?.disciplinaId != null) {
      params = params.set('disciplinaId', filtros.disciplinaId);
    }

    if (filtros?.categoriaManual) {
      params = params.set('categoriaManual', filtros.categoriaManual);
    }

    if (filtros?.fechaDesde) {
      params = params.set('fechaDesde', filtros.fechaDesde);
    }

    if (filtros?.fechaHasta) {
      params = params.set('fechaHasta', filtros.fechaHasta);
    }

    if (filtros?.q?.trim()) {
      params = params.set('q', filtros.q.trim());
    }

    return this.http.get<BaseResponse<DashboardIngresosResponse>>(
      `${this.base}/api/admin/dashboard/ingresos`,
      { params }
    );
  }
}