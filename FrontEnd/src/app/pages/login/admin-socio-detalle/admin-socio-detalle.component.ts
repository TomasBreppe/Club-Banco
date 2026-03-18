import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { finalize } from 'rxjs/operators';

import { SociosService } from '../../../service/socio.service';
import { SocioResumenDto } from '../../../features/socios/socio-resumen.models';
import { AdminStatsService } from '../../../core/auth/stats/admin-stats.service';
import { PagosService } from '../../../service/pagos.service';
import {
  PagoManualRequest,
  ArancelDisciplinaDto,
} from '../../../features/pagos/pago-manual.models';

@Component({
  standalone: true,
  selector: 'app-admin-socio-detalle',
  imports: [CommonModule, RouterLink, FormsModule],
  templateUrl: './admin-socio-detalle.component.html',
})
export class AdminSocioDetalleComponent implements OnInit {
  loading = false;
  error: string | null = null;
  data: SocioResumenDto | null = null;

  pagoError: string | null = null;
  pagoOk: string | null = null;

  aranceles: ArancelDisciplinaDto[] = [];
  arancelSeleccionado: ArancelDisciplinaDto | null = null;

  pago: PagoManualRequest = {
    socioId: 0,
    concepto: 'CUOTA_MENSUAL',
    periodo: '',
    disciplinaId: null,
    arancelDisciplinaId: null,
    categoria: null,
    montoTotal: 0,
    montoSocial: 0,
    montoDisciplina: 0,
    montoPreparacionFisica: 0,
    medio: 'EFECTIVO',
    observacion: '',
  };

  constructor(
    private route: ActivatedRoute,
    private api: SociosService,
    private pagosApi: PagosService,
    private cdr: ChangeDetectorRef,
    private statsSvc: AdminStatsService,
  ) {}

  ngOnInit(): void {
    this.route.paramMap.subscribe({
      next: (params) => {
        const id = Number(params.get('id'));

        if (!id || Number.isNaN(id)) {
          this.data = null;
          this.error = 'ID de socio inválido';
          this.loading = false;
          this.cdr.detectChanges();
          return;
        }

        this.cargar(id);
      },
    });
  }

  cargar(id: number): void {
    this.loading = true;
    this.error = null;
    this.data = null;
    this.cdr.detectChanges();

    this.api
      .resumen(id)
      .pipe(
        finalize(() => {
          this.loading = false;
          this.cdr.detectChanges();
        }),
      )
      .subscribe({
        next: (res) => {
          this.data = res?.data ?? null;

          if (!this.data) {
            this.error = 'No se encontraron datos del socio';
            this.cdr.detectChanges();
            return;
          }

          this.pago.socioId = this.data.socioId;
          this.pago.disciplinaId = this.data.disciplinaId ?? null;

          this.cargarAranceles();
          this.autocompletarPago();
          this.cdr.detectChanges();
        },
        error: (err) => {
          this.data = null;
          this.error =
            err?.error?.mensaje ||
            err?.message ||
            `Error cargando resumen (${err?.status || 'sin status'})`;
          this.cdr.detectChanges();
        },
      });
  }

  cargarAranceles(): void {
    const disciplinaId = this.data?.disciplinaId;
    if (!disciplinaId) {
      this.aranceles = [];
      this.arancelSeleccionado = null;
      return;
    }

    this.pagosApi.getArancelesPorDisciplina(disciplinaId).subscribe({
      next: (res) => {
        this.aranceles = res?.data ?? [];

        if (this.aranceles.length > 0) {
          const arancelSocio = this.aranceles.find((a) => a.id === this.data?.arancelDisciplinaId);

          const seleccionado = arancelSocio ?? this.aranceles[0];

          this.pago.arancelDisciplinaId = seleccionado.id;
          this.onArancelChange();
        } else {
          this.arancelSeleccionado = null;
        }

        this.cdr.detectChanges();
      },
      error: () => {
        this.aranceles = [];
        this.arancelSeleccionado = null;
        this.cdr.detectChanges();
      },
    });
  }

  onConceptoChange(): void {
    this.autocompletarPago();
    this.cdr.detectChanges();
  }

  onArancelChange(): void {
    const id = Number(this.pago.arancelDisciplinaId);
    this.arancelSeleccionado = this.aranceles.find((a) => a.id === id) ?? null;

    if (!this.arancelSeleccionado) {
      this.pago.categoria = null;
      this.pago.montoSocial = 0;
      this.pago.montoDisciplina = 0;
      this.pago.montoPreparacionFisica = 0;
      this.pago.montoTotal = 0;
      return;
    }

    if (this.pago.concepto === 'INSCRIPCION') {
      this.pago.periodo = null;
      this.pago.categoria = null;
      this.pago.montoSocial = 0;
      this.pago.montoDisciplina = 0;
      this.pago.montoPreparacionFisica = 0;
      this.pago.montoTotal = 0;
      return;
    }

    this.pago.categoria = this.arancelSeleccionado.categoria;
    this.pago.montoSocial = Number(this.arancelSeleccionado.montoSocial ?? 0);
    this.pago.montoDisciplina = Number(this.arancelSeleccionado.montoDeportivo ?? 0);
    this.pago.montoPreparacionFisica = Number(this.arancelSeleccionado.montoPreparacionFisica ?? 0);
    this.recalcularMontoTotal();

    if (
      this.pago.concepto === 'CUOTA_MENSUAL' &&
      !this.data?.deuda?.items?.some((i) => !i.pagado && i.periodo !== 'INSCRIPCION')
    ) {
      this.autocompletarPago();
    }
  }

  recalcularMontoTotal(): void {
    const total =
      Number(this.pago.montoSocial ?? 0) +
      Number(this.pago.montoDisciplina ?? 0) +
      Number(this.pago.montoPreparacionFisica ?? 0);

    this.pago.montoTotal = total;
  }

  autocompletarPago(): void {
    if (!this.data) return;

    const deuda = this.data.deuda;

    if (this.pago.concepto === 'CUOTA_MENSUAL') {
      const primerAdeudado = deuda?.items?.find((i) => !i.pagado && i.periodo !== 'INSCRIPCION');

      if (primerAdeudado) {
        this.pago.periodo = primerAdeudado.periodo;
      } else {
        const actual = this.periodoActual();

        if (this.arancelSeleccionado?.vigenteDesde) {
          const periodoArancel = this.periodoDesdeFecha(this.arancelSeleccionado.vigenteDesde);
          this.pago.periodo = this.maxPeriodo(actual, periodoArancel);
        } else {
          this.pago.periodo = actual;
        }
      }

      if (this.arancelSeleccionado) {
        this.pago.categoria = this.arancelSeleccionado.categoria;
        this.pago.montoSocial = Number(this.arancelSeleccionado.montoSocial ?? 0);
        this.pago.montoDisciplina = Number(this.arancelSeleccionado.montoDeportivo ?? 0);
        this.pago.montoPreparacionFisica = Number(
          this.arancelSeleccionado.montoPreparacionFisica ?? 0,
        );
        this.recalcularMontoTotal();
      }

      return;
    }

    if (this.pago.concepto === 'INSCRIPCION') {
      this.pago.periodo = null;
      this.pago.arancelDisciplinaId = null;
      this.pago.categoria = null;
      this.pago.montoSocial = 0;
      this.pago.montoDisciplina = 0;
      this.pago.montoPreparacionFisica = 0;
      this.pago.montoTotal = 0;
      return;
    }
  }

  resetPagoForm(): void {
    this.pagoError = null;
    this.pagoOk = null;

    this.pago = {
      socioId: this.data?.socioId ?? 0,
      concepto: 'CUOTA_MENSUAL',
      periodo: '',
      disciplinaId: this.data?.disciplinaId ?? null,
      arancelDisciplinaId: this.aranceles[0]?.id ?? null,
      categoria: this.aranceles[0]?.categoria ?? null,
      montoTotal: 0,
      montoSocial: 0,
      montoDisciplina: 0,
      montoPreparacionFisica: 0,
      medio: 'EFECTIVO',
      observacion: '',
    };

    this.autocompletarPago();
    this.onArancelChange();
  }

  registrarPago(): void {
    this.pagoError = null;
    this.pagoOk = null;
    this.cdr.detectChanges();

    if (!this.pago.socioId) {
      this.pagoError = 'Socio inválido';
      this.cdr.detectChanges();
      return;
    }

    if (this.pago.concepto === 'CUOTA_MENSUAL') {
      const per = (this.pago.periodo ?? '').trim();
      if (!/^\d{4}-\d{2}$/.test(per)) {
        this.pagoError = 'Periodo inválido. Formato: YYYY-MM (ej: 2026-03)';
        this.cdr.detectChanges();
        return;
      }
      this.pago.periodo = per;

      if (!this.pago.arancelDisciplinaId) {
        this.pagoError = 'Debés seleccionar una categoría/arancel';
        this.cdr.detectChanges();
        return;
      }

      if (this.arancelSeleccionado?.vigenteDesde) {
        const periodoMinimo = this.periodoDesdeFecha(this.arancelSeleccionado.vigenteDesde);

        if (this.periodoEsAnterior(per, periodoMinimo)) {
          this.pagoError =
            `No podés registrar el período ${per} porque el arancel ${this.arancelSeleccionado.categoria} ` +
            `rige desde ${periodoMinimo}.`;
          this.cdr.detectChanges();
          return;
        }
      }
    } else {
      this.pago.periodo = null;
    }

    if (!this.pago.medio?.trim()) {
      this.pagoError = 'Medio es obligatorio';
      this.cdr.detectChanges();
      return;
    }

    this.recalcularMontoTotal();

    if (!this.pago.montoTotal || this.pago.montoTotal <= 0) {
      this.pagoError = 'Monto total debe ser mayor a 0';
      this.cdr.detectChanges();
      return;
    }

    this.loading = true;
    this.cdr.detectChanges();

    this.pagosApi
      .registrarManual(this.pago)
      .pipe(
        finalize(() => {
          this.loading = false;
          this.cdr.detectChanges();
        }),
      )
      .subscribe({
        next: (res: any) => {
          if (res.status !== 200 && res.status !== 201) {
            this.pagoError = res.mensaje || 'No se pudo registrar el pago';
            this.cdr.detectChanges();
            return;
          }

          this.pagoOk = 'Pago registrado correctamente';

          const socioId = this.pago.socioId;
          this.cargar(socioId);
          this.resetPagoForm();
          this.statsSvc.refresh();
          this.cdr.detectChanges();
        },
        error: (err) => {
          this.pagoError = err?.error?.mensaje || 'Error registrando pago';
          this.cdr.detectChanges();
        },
      });
  }

  periodoActual(): string {
    const now = new Date();
    const yyyy = now.getFullYear();
    const mm = String(now.getMonth() + 1).padStart(2, '0');
    return `${yyyy}-${mm}`;
  }

  periodoDesdeFecha(fechaIso: string): string {
    const d = new Date(fechaIso);
    const yyyy = d.getFullYear();
    const mm = String(d.getMonth() + 1).padStart(2, '0');
    return `${yyyy}-${mm}`;
  }

  maxPeriodo(a: string, b: string): string {
    return a >= b ? a : b;
  }

  periodoEsAnterior(periodo: string, periodoMinimo: string): boolean {
    return periodo < periodoMinimo;
  }

  fmtFecha(iso: string): string {
    const d = new Date(iso);
    return d.toLocaleString();
  }

  getPeriodosAdeudados(): string[] {
    const items = this.data?.deuda?.items ?? [];

    return items.filter((i) => !i.pagado).map((i) => i.periodo);
  }

  formatearPeriodo(periodo: string): string {
    if (periodo === 'INSCRIPCION') return 'Inscripción';

    const [anio, mes] = periodo.split('-').map(Number);

    const nombresMeses = [
      'enero',
      'febrero',
      'marzo',
      'abril',
      'mayo',
      'junio',
      'julio',
      'agosto',
      'septiembre',
      'octubre',
      'noviembre',
      'diciembre',
    ];

    if (!anio || !mes || mes < 1 || mes > 12) return periodo;

    return `${nombresMeses[mes - 1]} ${anio}`;
  }
}
