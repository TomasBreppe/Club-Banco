import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';
import {
  BaseResponse,
  IngresoManualCreateRequest,
  IngresoManualDto,
} from '../models/ingreso-manual.model';

@Injectable({
  providedIn: 'root',
})
export class IngresosManualesService {
  private http = inject(HttpClient);
  private base = environment.apiUrl;

  crear(body: IngresoManualCreateRequest): Observable<BaseResponse<IngresoManualDto>> {
    return this.http.post<BaseResponse<IngresoManualDto>>(
      `${this.base}/api/admin/ingresos-manuales`,
      body
    );
  }
}