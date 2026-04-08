import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { BaseResponse, DashboardSociosResponse } from '../models/dashboard.model';
import { environment } from '../../environments/environment';

@Injectable({
  providedIn: 'root',
})
export class DashboardService {
  private http = inject(HttpClient);
  private apiUrl = `${environment.apiUrl}/api/admin/dashboard`;

  obtenerDashboardSocios(filtros?: {
    disciplinaId?: number | null;
    activo?: boolean | null;
    categoria?: string | null;

    estadoPago?: string | null;
    q?: string | null;
  }): Observable<BaseResponse<DashboardSociosResponse>> {
    let params = new HttpParams();

    if (filtros?.disciplinaId != null) {
      params = params.set('disciplinaId', filtros.disciplinaId);
    }

    if (filtros?.activo != null) {
      params = params.set('activo', filtros.activo);
    }

    if (filtros?.categoria && filtros.categoria.trim() !== '') {
      params = params.set('categoria', filtros.categoria.trim());
    }

    if (filtros?.estadoPago) {
      params = params.set('estadoPago', filtros.estadoPago);
    }

    if (filtros?.q && filtros.q.trim() !== '') {
      params = params.set('q', filtros.q.trim());
    }

    return this.http.get<BaseResponse<DashboardSociosResponse>>(`${this.apiUrl}/socios`, {
      params,
    });
  }
}
