import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { environment } from '../../environments/environment';
import { Observable } from 'rxjs';
import { BaseResponse } from '../core/auth/auth.models';
import {
  DisciplinaDto,
  CuotaDto,
  CuotaCreateRequest,
  ArancelDisciplinaDto,
  ArancelCreateRequest,
} from '../features/disciplinas.models';

@Injectable({ providedIn: 'root' })
export class DisciplinasService {
  private base = environment.apiUrl;

  constructor(private http: HttpClient) {}

  listar(): Observable<BaseResponse<DisciplinaDto[]>> {
    return this.http.get<BaseResponse<DisciplinaDto[]>>(`${this.base}/api/admin/disciplinas`);
  }

  crear(nombre: string): Observable<BaseResponse<DisciplinaDto>> {
    return this.http.post<BaseResponse<DisciplinaDto>>(`${this.base}/api/admin/disciplinas`, {
      nombre,
    });
  }

  cambiarEstado(id: number, activa: boolean): Observable<BaseResponse<DisciplinaDto>> {
    return this.http.patch<BaseResponse<DisciplinaDto>>(
      `${this.base}/api/admin/disciplinas/${id}/estado?activa=${activa}`,
      {},
    );
  }

  getCuotaActiva(disciplinaId: number): Observable<BaseResponse<CuotaDto>> {
    return this.http.get<BaseResponse<CuotaDto>>(
      `${this.base}/api/admin/disciplinas/${disciplinaId}/cuotas/activa`,
    );
  }

  crearNuevaCuota(
    disciplinaId: number,
    body: CuotaCreateRequest,
  ): Observable<BaseResponse<CuotaDto>> {
    return this.http.post<BaseResponse<CuotaDto>>(
      `${this.base}/api/admin/disciplinas/${disciplinaId}/cuotas`,
      body,
    );
  }

  getArancelesPorDisciplina(
    disciplinaId: number,
  ): Observable<BaseResponse<ArancelDisciplinaDto[]>> {
    return this.http.get<BaseResponse<ArancelDisciplinaDto[]>>(
      `${this.base}/api/admin/disciplinas/${disciplinaId}/aranceles`,
    );
  }

  crearArancel(body: ArancelCreateRequest): Observable<BaseResponse<ArancelDisciplinaDto>> {
    return this.http.post<BaseResponse<ArancelDisciplinaDto>>(
      `${this.base}/api/admin/aranceles`,
      body,
    );
  }

  cambiarEstadoArancel(
    arancelId: number,
    activa: boolean,
  ): Observable<BaseResponse<ArancelDisciplinaDto>> {
    return this.http.patch<BaseResponse<ArancelDisciplinaDto>>(
      `${this.base}/api/admin/aranceles/${arancelId}/estado?activa=${activa}`,
      {},
    );
  }
}
