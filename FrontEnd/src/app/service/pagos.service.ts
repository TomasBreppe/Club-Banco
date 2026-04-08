import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { environment } from '../../environments/environment';
import { BaseResponse } from '../core/auth/auth.models';
import { Observable } from 'rxjs';
import { PagoManualRequest } from '../features/pagos/pago-manual.models';

@Injectable({ providedIn: 'root' })
export class PagosService {
  private base = environment.apiUrl;

  constructor(private http: HttpClient) {}

  registrarManual(body: PagoManualRequest): Observable<BaseResponse<any>> {
    return this.http.post<BaseResponse<any>>(`${this.base}/api/admin/pagos/manual`, body);
  }

  getArancelesActivos() {
    return this.http.get<any>(`${this.base}/api/admin/aranceles/activos`);
  }

  getArancelesPorDisciplina(disciplinaId: number) {
    return this.http.get<any>(`${this.base}/api/admin/disciplinas/${disciplinaId}/aranceles`);
  }
  descargarComprobante(pagoId: number) {
    return this.http.get(`${this.base}/api/admin/pagos/${pagoId}/comprobante`, {
      responseType: 'blob',
    });
  }
}
