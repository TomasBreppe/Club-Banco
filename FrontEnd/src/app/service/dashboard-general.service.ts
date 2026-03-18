import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';
import { BaseResponse, DashboardGeneral } from '../models/dashboard-general.model';

@Injectable({
  providedIn: 'root',
})
export class DashboardGeneralService {
  private http = inject(HttpClient);
  private base = environment.apiUrl;

  obtenerDashboardGeneral(): Observable<BaseResponse<DashboardGeneral>> {
    return this.http.get<BaseResponse<DashboardGeneral>>(
      `${this.base}/api/admin/dashboard/general`
    );
  }
}