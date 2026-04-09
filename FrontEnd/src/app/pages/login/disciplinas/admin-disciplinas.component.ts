import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { finalize, forkJoin, of } from 'rxjs';
import { catchError } from 'rxjs/operators';
import { ActivatedRoute } from '@angular/router';

import { DisciplinasService } from '../../../service/disciplinas.service';
import {
  DisciplinaDto,
  ArancelDisciplinaDto,
  ArancelCreateRequest,
} from '../../../features/disciplinas.models';
import { AdminStatsService } from '../../../core/auth/stats/admin-stats.service';

@Component({
  standalone: true,
  selector: 'app-admin-disciplinas',
  imports: [CommonModule, FormsModule],
  templateUrl: './admin-disciplinas.component.html',
  styleUrls: ['./admin-disciplinas.component.css'],
})
export class AdminDisciplinasComponent implements OnInit {
  items: DisciplinaDto[] = [];
  nombre = '';
  loading = false;
  initialLoading = true;
  cargandoAranceles = false;
  error: string | null = null;

  arancelesPorDisciplina: Record<number, ArancelDisciplinaDto[]> = {};
  mostrarFormulario: Record<number, boolean> = {};
  guardandoArancel: Record<number, boolean> = {};

  errorArancel: string | null = null;
  okArancel: string | null = null;

  nuevoArancelPorDisciplina: Record<number, ArancelCreateRequest> = {};

  constructor(
    private api: DisciplinasService,
    private route: ActivatedRoute,
    private statsSvc: AdminStatsService,
    private cdr: ChangeDetectorRef,
  ) {}

  ngOnInit(): void {
    const prefetched = this.route.snapshot.data['prefetched'];
    this.items = prefetched?.data ?? [];
    this.inicializarFormularios();
    this.cdr.detectChanges();

    if (this.items.length > 0) {
      this.cargarAranceles();
    } else {
      this.initialLoading = false;
      this.cargandoAranceles = false;
      this.cdr.detectChanges();
    }
  }

  inicializarFormularios(): void {
    this.items.forEach((d) => {
      this.nuevoArancelPorDisciplina[d.id] = {
        disciplinaId: d.id,
        categoria: '',
        montoSocial: 0,
        montoDeportivo: 0,
        montoPreparacionFisica: 0,
        vigenteDesde: this.primerDiaMesSiguiente(),
      };
    });
  }

  cargar(): void {
    this.loading = true;
    this.error = null;
    this.cdr.detectChanges();

    this.api
      .listar()
      .pipe(
        finalize(() => {
          this.loading = false;
          this.cdr.detectChanges();
        }),
      )
      .subscribe({
        next: (res) => {
          this.items = res.data ?? [];
          this.arancelesPorDisciplina = {};
          this.inicializarFormularios();
          this.cdr.detectChanges();
          this.cargarAranceles();
        },
        error: (err) => {
          this.error = err?.error?.mensaje || 'Error cargando disciplinas';
          this.cdr.detectChanges();
        },
      });
  }

  crear(): void {
    const n = this.nombre.trim();
    if (!n) return;

    this.loading = true;
    this.error = null;
    this.cdr.detectChanges();

    this.api
      .crear(n)
      .pipe(
        finalize(() => {
          this.loading = false;
          this.cdr.detectChanges();
        }),
      )
      .subscribe({
        next: (res) => {
          if (res.status !== 200 && res.status !== 201) {
            this.error = res.mensaje || 'No se pudo crear';
            this.cdr.detectChanges();
            return;
          }

          this.nombre = '';
          this.cargar();
          this.statsSvc.refresh();
        },
        error: (err) => {
          this.error = err?.error?.mensaje || 'Error creando disciplina';
          this.cdr.detectChanges();
        },
      });
  }

  toggleDisciplina(it: DisciplinaDto): void {
    this.loading = true;
    this.error = null;
    this.cdr.detectChanges();

    this.api
      .cambiarEstado(it.id, !it.activa)
      .pipe(
        finalize(() => {
          this.loading = false;
          this.cdr.detectChanges();
        }),
      )
      .subscribe({
        next: () => {
          this.cargar();
        },
        error: (err) => {
          this.error = err?.error?.mensaje || 'Error actualizando estado';
          this.cdr.detectChanges();
        },
      });
  }

  cargarAranceles(): void {
    if (!this.items.length) {
      this.initialLoading = false;
      this.cargandoAranceles = false;
      this.cdr.detectChanges();
      return;
    }

    this.cargandoAranceles = true;
    this.errorArancel = null;
    this.cdr.detectChanges();

    const requests = this.items.map((d) =>
      this.api.getArancelesPorDisciplina(d.id).pipe(catchError(() => of({ data: [] } as any))),
    );

    forkJoin(requests)
      .pipe(
        finalize(() => {
          this.initialLoading = false;
          this.cargandoAranceles = false;
          this.cdr.detectChanges();
        }),
      )
      .subscribe({
        next: (responses) => {
          responses.forEach((res, index) => {
            const disciplina = this.items[index];
            this.arancelesPorDisciplina[disciplina.id] = res?.data ?? [];
          });
          this.cdr.detectChanges();
        },
        error: () => {
          this.cdr.detectChanges();
        },
      });
  }

  toggleFormulario(disciplinaId: number): void {
    this.mostrarFormulario[disciplinaId] = !this.mostrarFormulario[disciplinaId];
    this.cdr.detectChanges();
  }

  guardarArancel(disciplinaId: number): void {
    this.errorArancel = null;
    this.okArancel = null;

    const body = this.nuevoArancelPorDisciplina[disciplinaId];
    if (!body) return;

    const categoria = body.categoria?.trim();
    const social = Number(body.montoSocial ?? 0);
    const deportivo = Number(body.montoDeportivo ?? 0);
    const prep = Number(body.montoPreparacionFisica ?? 0);

    if (!categoria) {
      this.errorArancel = 'La categoría es obligatoria';
      this.cdr.detectChanges();
      return;
    }

    if (social < 0 || deportivo < 0 || prep < 0) {
      this.errorArancel = 'Los montos no pueden ser negativos';
      this.cdr.detectChanges();
      return;
    }

    if (social + deportivo + prep <= 0) {
      this.errorArancel = 'La suma de los montos debe ser mayor a 0';
      this.cdr.detectChanges();
      return;
    }

    this.guardandoArancel[disciplinaId] = true;
    this.cdr.detectChanges();

    this.api
      .crearArancel({
        ...body,
        categoria,
        montoSocial: social,
        montoDeportivo: deportivo,
        montoPreparacionFisica: prep,
      })
      .pipe(
        finalize(() => {
          this.guardandoArancel[disciplinaId] = false;
          this.cdr.detectChanges();
        }),
      )
      .subscribe({
        next: (res) => {
          if (res.status !== 200 && res.status !== 201) {
            this.errorArancel = res.mensaje || 'No se pudo guardar el arancel';
            this.cdr.detectChanges();
            return;
          }

          this.okArancel = 'Arancel guardado correctamente';
          this.nuevoArancelPorDisciplina[disciplinaId] = {
            disciplinaId,
            categoria: '',
            montoSocial: 0,
            montoDeportivo: 0,
            montoPreparacionFisica: 0,
            vigenteDesde: this.primerDiaMesSiguiente(),
          };

          this.mostrarFormulario[disciplinaId] = false;
          this.cargarAranceles();
          this.statsSvc.refresh();
        },
        error: (err) => {
          this.errorArancel =
            err?.error?.mensaje ||
            err?.message ||
            `Error guardando arancel (${err?.status || 'sin status'})`;
          this.cdr.detectChanges();
        },
      });
  }

  toggleArancel(a: ArancelDisciplinaDto): void {
    this.errorArancel = null;
    this.okArancel = null;

    this.api.cambiarEstadoArancel(a.id, !a.activa).subscribe({
      next: (res) => {
        if (res.status !== 200 && res.status !== 201) {
          this.errorArancel = res.mensaje || 'No se pudo cambiar el estado del arancel';
          this.cdr.detectChanges();
          return;
        }

        this.okArancel = a.activa
          ? 'Arancel desactivado correctamente'
          : 'Arancel activado correctamente';

        this.cargarAranceles();
      },
      error: (err) => {
        this.errorArancel = err?.error?.mensaje || 'Error cambiando estado del arancel';
        this.cdr.detectChanges();
      },
    });
  }

  totalArancel(a: ArancelDisciplinaDto): number {
    return (
      Number(a.montoSocial || 0) +
      Number(a.montoDeportivo || 0) +
      Number(a.montoPreparacionFisica || 0)
    );
  }

  primerDiaMesSiguiente(): string {
    const hoy = new Date();
    const primerDiaMesSiguiente = new Date(hoy.getFullYear(), hoy.getMonth() + 1, 1);
    return primerDiaMesSiguiente.toISOString().slice(0, 10);
  }

  formatMoneda(valor: number | null | undefined): string {
    const n = Number(valor ?? 0);
    return new Intl.NumberFormat('es-AR', {
      style: 'currency',
      currency: 'ARS',
      maximumFractionDigits: 0,
    }).format(n);
  }
}
