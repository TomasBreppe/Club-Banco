import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';
import { BaseResponse, DashboardBalanceResponse } from '../models/balance.model';

@Injectable({
  providedIn: 'root',
})
export class DashboardBalanceService {
  private http = inject(HttpClient);
  private base = environment.apiUrl;

  obtenerDashboard(
    fechaDesde?: string | null,
    fechaHasta?: string | null
  ): Observable<BaseResponse<DashboardBalanceResponse>> {
    let params = new HttpParams();

    if (fechaDesde) {
      params = params.set('fechaDesde', fechaDesde);
    }

    if (fechaHasta) {
      params = params.set('fechaHasta', fechaHasta);
    }

    return this.http.get<BaseResponse<DashboardBalanceResponse>>(
      `${this.base}/api/admin/dashboard/balance`,
      { params }
    );
  }
}