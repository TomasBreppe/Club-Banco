import { Injectable } from '@angular/core';
import { BehaviorSubject, forkJoin, Observable } from 'rxjs';
import { catchError, finalize, map } from 'rxjs/operators';

import { SociosService } from '../../../service/socio.service';
import { DisciplinasService } from '../../../service/disciplinas.service';

export type AdminStats = {
  socios: number;
  disciplinasActivas: number;
  deudores: number;
};

@Injectable({ providedIn: 'root' })
export class AdminStatsService {
  private readonly _stats$ = new BehaviorSubject<AdminStats | null>(null);
  stats$ = this._stats$.asObservable();

  private loading = false;

  constructor(
    private sociosApi: SociosService,
    private disciplinasApi: DisciplinasService
  ) {}

  /** Carga una sola vez (si ya hay datos, no vuelve a pegar) */
  loadOnce(): void {
    if (this.loading) return;
    if (this._stats$.value) return;
    this.refresh();
  }

  /** Fuerza recarga */
  refresh(): void {
    if (this.loading) return;
    this.loading = true;

    forkJoin({
      socios: this.sociosApi.listar(),
      disciplinas: this.disciplinasApi.listar(),
    })
      .pipe(
        map(({ socios, disciplinas }) => {
          const sociosArr = (socios as any)?.data ?? [];
          const discArr = (disciplinas as any)?.data ?? [];

          const sociosCount = Array.isArray(sociosArr) ? sociosArr.length : 0;
          const discActivas = Array.isArray(discArr)
            ? discArr.filter((d: any) => (d.activa ?? d.activo) === true).length
            : 0;

          const deudores = Array.isArray(sociosArr)
            ? sociosArr.filter((s: any) => String(s.estadoPago ?? '').toUpperCase() === 'DEBE').length
            : 0;

          return { socios: sociosCount, disciplinasActivas: discActivas, deudores };
        }),
        catchError(() => {
          // si falla, NO pisamos con 0 (dejamos lo último o null)
          return [];
        }),
        finalize(() => (this.loading = false))
      )
      .subscribe((stats: any) => {
        if (stats) this._stats$.next(stats);
      });
  }
}
