import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';
import {
  BaseResponse,
  DashboardGastosResponse,
  Gasto,
  GastoCreateRequest,
} from '../models/gasto.model';

@Injectable({
  providedIn: 'root',
})
export class GastosService {
  private http = inject(HttpClient);
  private base = environment.apiUrl;

  crear(body: GastoCreateRequest): Observable<BaseResponse<Gasto>> {
    return this.http.post<BaseResponse<Gasto>>(`${this.base}/api/admin/gastos`, body);
  }

  listar(filtros?: {
    categoria?: string | null;
    concepto?: string | null;
    fechaDesde?: string | null;
    fechaHasta?: string | null;
    q?: string | null;
  }): Observable<BaseResponse<Gasto[]>> {
    let params = new HttpParams();

    if (filtros?.categoria) {
      params = params.set('categoria', filtros.categoria);
    }

    if (filtros?.concepto?.trim()) {
      params = params.set('concepto', filtros.concepto.trim());
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

    return this.http.get<BaseResponse<Gasto[]>>(`${this.base}/api/admin/gastos`, { params });
  }

  dashboard(filtros?: {
    categoria?: string | null;
    concepto?: string | null;
    fechaDesde?: string | null;
    fechaHasta?: string | null;
    q?: string | null;
  }): Observable<BaseResponse<DashboardGastosResponse>> {
    let params = new HttpParams();

    if (filtros?.categoria) {
      params = params.set('categoria', filtros.categoria);
    }

    if (filtros?.concepto?.trim()) {
      params = params.set('concepto', filtros.concepto.trim());
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

    return this.http.get<BaseResponse<DashboardGastosResponse>>(
      `${this.base}/api/admin/gastos/dashboard`,
      { params }
    );
  }
}