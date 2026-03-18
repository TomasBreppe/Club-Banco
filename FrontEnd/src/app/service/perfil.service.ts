import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { environment } from '../../environments/environment';
import { BehaviorSubject, Observable, tap } from 'rxjs';

export interface MiPerfil {
  id: number;
  email: string;
  rol: string;
  activo: boolean;
  nombre: string;
  apellido: string;
  telefono: string;
  fotoUrl: string;
}

@Injectable({ providedIn: 'root' })
export class PerfilService {
  private base = environment.apiUrl;

  private perfilSubject = new BehaviorSubject<MiPerfil | null>(null);
  perfil$ = this.perfilSubject.asObservable();

  constructor(private http: HttpClient) {}

  obtenerPerfil(): Observable<any> {
    return this.http.get<any>(`${this.base}/api/me/perfil`).pipe(
      tap((res) => {
        if (res?.data) {
          this.perfilSubject.next(res.data);
        }
      }),
    );
  }

  actualizarPerfil(body: any): Observable<any> {
    return this.http.put<any>(`${this.base}/api/me/perfil`, body).pipe(
      tap(() => {
        this.obtenerPerfil().subscribe();
      }),
    );
  }

  actualizarFoto(body: { fotoUrl: string }): Observable<any> {
    return this.http.put<any>(`${this.base}/api/me/foto`, body).pipe(
      tap(() => {
        this.obtenerPerfil().subscribe();
      }),
    );
  }

  cambiarPassword(body: { currentPassword: string; newPassword: string }): Observable<any> {
    return this.http.post<any>(`${this.base}/api/auth/change-password`, body);
  }

  getPerfilActual(): MiPerfil | null {
    return this.perfilSubject.value;
  }

  setPerfil(perfil: MiPerfil): void {
    this.perfilSubject.next(perfil);
  }

  mergePerfil(parcial: Partial<MiPerfil>): void {
    const actual = this.perfilSubject.value;
    if (!actual) return;

    this.perfilSubject.next({
      ...actual,
      ...parcial,
    });
  }
}
