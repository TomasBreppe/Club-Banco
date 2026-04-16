import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { environment } from '../../environments/environment';
import { Observable } from 'rxjs';

import { BaseResponse } from '../core/auth/auth.models';
import { SocioCreateRequest, SocioDto } from '../features/socios/socio.models';
import { SocioResumenDto } from '../features/socios/socio-resumen.models';

@Injectable({ providedIn: 'root' })
export class SociosService {
  private base = environment.apiUrl;

  constructor(private http: HttpClient) {}

  listar(): Observable<BaseResponse<SocioDto[]>> {
    return this.http.get<BaseResponse<SocioDto[]>>(`${this.base}/api/admin/socios`);
  }

  crear(body: SocioCreateRequest): Observable<BaseResponse<SocioDto>> {
    return this.http.post<BaseResponse<SocioDto>>(`${this.base}/api/admin/socios`, body);
  }

  resumen(id: number): Observable<BaseResponse<SocioResumenDto>> {
    return this.http.get<BaseResponse<SocioResumenDto>>(
      `${this.base}/api/admin/socios/${id}/resumen`,
    );
  }

  agregarDisciplina(
    socioId: number,
    body: { disciplinaId: number; arancelDisciplinaId: number; inscripcionPagada?: boolean },
  ): Observable<BaseResponse<any>> {
    return this.http.post<BaseResponse<any>>(
      `${this.base}/api/admin/socios/${socioId}/disciplinas`,
      body,
    );
  }

  actualizarBeca(socioId: number, body: any) {
    return this.http.put<any>(`${this.base}/api/admin/socios/${socioId}/beca`, body);
  }

  cambiarActivo(id: number, valor: boolean) {
    return this.http.patch<BaseResponse<SocioDto>>(
      `${this.base}/api/admin/socios/${id}/activo?valor=${valor}`,
      {},
    );
  }
}
