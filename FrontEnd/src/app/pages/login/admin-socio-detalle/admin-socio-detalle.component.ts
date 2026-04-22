import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { HttpClient } from '@angular/common/http';
import { finalize } from 'rxjs/operators';

import { SociosService } from '../../../service/socio.service';
import { AdminStatsService } from '../../../core/auth/stats/admin-stats.service';
import { PagosService } from '../../../service/pagos.service';
import {
  PagoManualRequest,
  ArancelDisciplinaDto,
  PagoDto,
} from '../../../features/pagos/pago-manual.models';
import { SocioResumenDto, DeudaResponseDto } from '../../../features/socios/socio-resumen.models';
import { environment } from '../../../../environments/environment';

interface SocioDisciplinaResumenVm {
  socioDisciplinaId?: number | null;
  disciplinaId: number;
  disciplinaNombre: string;
  arancelDisciplinaId?: number | null;
  categoriaArancel?: string | null;
  vigenciaHasta?: string | null;
  inscripcionPagada?: boolean | null;
  deuda: DeudaResponseDto | null;
}

interface SocioResumenMultiDto extends SocioResumenDto {
  disciplinas?: SocioDisciplinaResumenVm[];
}

interface DisciplinaDto {
  id: number;
  nombre: string;
  activa?: boolean;
}

interface AgregarDisciplinaForm {
  disciplinaId: number | null;
  arancelDisciplinaId: number | null;
  inscripcionPagada: boolean;
}

interface BecaForm {
  tieneBeca: boolean;
  porcentajeBecaSocial: number | null;
  porcentajeBecaDeportiva: number | null;
  porcentajeBecaPreparacionFisica: number | null;
  observacionBeca: string;
}

@Component({
  standalone: true,
  selector: 'app-admin-socio-detalle',
  imports: [CommonModule, RouterLink, FormsModule],
  templateUrl: './admin-socio-detalle.component.html',
  styleUrls: ['./admin-socio-detalle.component.css'],
})
export class AdminSocioDetalleComponent implements OnInit {
  loading = false;
  error: string | null = null;
  data: SocioResumenMultiDto | null = null;
  ultimoPagoRegistradoId: number | null = null;
  anulandoPagoId: number | null = null;
  pagoError: string | null = null;
  pagoOk: string | null = null;

  pagoErrorPorDisciplina: Record<number, string | null> = {};
  pagoOkPorDisciplina: Record<number, string | null> = {};
  arancelesPorDisciplina: Record<number, ArancelDisciplinaDto[]> = {};
  arancelSeleccionadoPorDisciplina: Record<number, ArancelDisciplinaDto | null> = {};
  pagoForms: Record<number, PagoManualRequest> = {};

  // NUEVO: agregar disciplina
  mostrarAgregarDisciplina = false;
  agregarDisciplinaLoading = false;
  agregarDisciplinaError: string | null = null;
  agregarDisciplinaOk: string | null = null;

  disciplinasDisponibles: DisciplinaDto[] = [];
  arancelesNuevaDisciplina: ArancelDisciplinaDto[] = [];

  agregarDisciplinaForm: AgregarDisciplinaForm = {
    disciplinaId: null,
    arancelDisciplinaId: null,
    inscripcionPagada: false,
  };

  editandoBeca = false;
  guardandoBeca = false;
  becaError: string | null = null;
  becaOk: string | null = null;

  becaForm: BecaForm = {
    tieneBeca: false,
    porcentajeBecaSocial: 0,
    porcentajeBecaDeportiva: 0,
    porcentajeBecaPreparacionFisica: 0,
    observacionBeca: '',
  };
  constructor(
    private route: ActivatedRoute,
    private api: SociosService,
    private pagosApi: PagosService,
    private cdr: ChangeDetectorRef,
    private statsSvc: AdminStatsService,
    private http: HttpClient,
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

  esDisciplinaPrincipal(d: SocioDisciplinaResumenVm): boolean {
    if (!this.data?.disciplinas?.length) return true;
    return this.data.disciplinas[0]?.disciplinaId === d.disciplinaId;
  }

  cargar(id: number): void {
    this.loading = true;
    this.error = null;
    this.data = null;
    this.pagoError = null;
    this.pagoOk = null;
    this.pagoErrorPorDisciplina = {};
    this.pagoOkPorDisciplina = {};
    this.pagoForms = {};
    this.arancelesPorDisciplina = {};
    this.arancelSeleccionadoPorDisciplina = {};
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
          const incoming = (res?.data ?? null) as SocioResumenMultiDto | null;

          if (!incoming) {
            this.data = null;
            this.error = 'No se encontraron datos del socio';
            this.cdr.detectChanges();
            return;
          }

          if (!incoming.disciplinas || incoming.disciplinas.length === 0) {
            incoming.disciplinas =
              incoming.disciplinaId != null
                ? [
                    {
                      socioDisciplinaId: null,
                      disciplinaId: incoming.disciplinaId,
                      disciplinaNombre: incoming.disciplinaNombre ?? '-',
                      arancelDisciplinaId: incoming.arancelDisciplinaId ?? null,
                      categoriaArancel: incoming.categoriaArancel ?? null,
                      vigenciaHasta: incoming.vigenciaHasta ?? null,
                      inscripcionPagada: null,
                      deuda: incoming.deuda ?? null,
                    },
                  ]
                : [];
          }

          this.data = incoming;

          setTimeout(() => {
            const disciplinas = this.data?.disciplinas ?? [];
            for (const d of disciplinas) {
              const arancel = this.arancelSeleccionadoPorDisciplina[d.disciplinaId];
              if (arancel) {
                this.cargarMontosConBeca(d, arancel);
              }
            }
            this.cdr.detectChanges();
          }, 0);

          this.becaForm = {
            tieneBeca: incoming.tieneBeca ?? false,
            porcentajeBecaSocial: incoming.porcentajeBecaSocial ?? 0,
            porcentajeBecaDeportiva: incoming.porcentajeBecaDeportiva ?? 0,
            porcentajeBecaPreparacionFisica: incoming.porcentajeBecaPreparacionFisica ?? 0,
            observacionBeca: incoming.observacionBeca ?? '',
          };

          const disciplinas = this.data.disciplinas ?? [];
          for (const d of disciplinas) {
            this.inicializarPagoForm(d);
            this.cargarArancelesPorDisciplina(d);
          }

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

  private inicializarPagoForm(d: SocioDisciplinaResumenVm): void {
    if (!this.data) return;

    this.pagoForms[d.disciplinaId] = {
      socioId: this.data.socioId,
      concepto: 'CUOTA_MENSUAL',
      periodo: '',
      disciplinaId: d.disciplinaId,
      arancelDisciplinaId: d.arancelDisciplinaId ?? null,
      categoria: d.categoriaArancel ?? null,
      montoTotal: 0,
      montoSocial: 0,
      montoDisciplina: 0,
      montoPreparacionFisica: 0,
      medio: 'EFECTIVO',
      observacion: '',
    };

    this.pagoErrorPorDisciplina[d.disciplinaId] = null;
    this.pagoOkPorDisciplina[d.disciplinaId] = null;
    this.arancelSeleccionadoPorDisciplina[d.disciplinaId] = null;
  }

  private cargarArancelesPorDisciplina(d: SocioDisciplinaResumenVm): void {
    this.pagosApi.getArancelesPorDisciplina(d.disciplinaId).subscribe({
      next: (res) => {
        const aranceles: ArancelDisciplinaDto[] = res?.data ?? [];
        this.arancelesPorDisciplina[d.disciplinaId] = aranceles;

        const form = this.pagoForms[d.disciplinaId];
        if (!form) return;

        const seleccionado =
          aranceles.find((a: ArancelDisciplinaDto) => a.id === d.arancelDisciplinaId) ??
          aranceles[0] ??
          null;

        if (seleccionado) {
          form.arancelDisciplinaId = seleccionado.id;
          this.arancelSeleccionadoPorDisciplina[d.disciplinaId] = seleccionado;
          this.onArancelChangeDisciplina(d);
        } else {
          this.arancelSeleccionadoPorDisciplina[d.disciplinaId] = null;
        }

        this.autocompletarPagoDisciplina(d);
        this.cdr.detectChanges();
      },
      error: () => {
        this.arancelesPorDisciplina[d.disciplinaId] = [];
        this.arancelSeleccionadoPorDisciplina[d.disciplinaId] = null;
        this.cdr.detectChanges();
      },
    });
  }

  anularPago(pago: PagoDto): void {
    if (!pago?.id) return;

    const motivo = window.prompt('Motivo de anulación (opcional):') ?? '';

    this.anulandoPagoId = pago.id;
    this.cdr.detectChanges();

    this.pagosApi
      .anularPago(pago.id, motivo)
      .pipe(
        finalize(() => {
          this.anulandoPagoId = null;
          this.cdr.detectChanges();
        }),
      )
      .subscribe({
        next: (res: any) => {
          if (res?.status !== 200) {
            this.error = res?.mensaje || 'No se pudo anular el pago';
            this.cdr.detectChanges();
            return;
          }

          this.pagoOk = 'Pago anulado correctamente';
          if (this.data?.socioId) {
            this.cargar(this.data.socioId);
          }
        },
        error: (err) => {
          this.error = err?.error?.mensaje || 'Error anulando el pago';
          this.cdr.detectChanges();
        },
      });
  }

  puedeAnularPago(p: any): boolean {
    return !p?.anulado;
  }

  // NUEVO: abrir/cerrar agregar disciplina
  toggleAgregarDisciplina(): void {
    this.mostrarAgregarDisciplina = !this.mostrarAgregarDisciplina;

    if (this.mostrarAgregarDisciplina) {
      this.agregarDisciplinaError = null;
      this.agregarDisciplinaOk = null;
      this.resetAgregarDisciplinaForm();
      this.cargarDisciplinasDisponibles();
    }
  }

  toggleEditarBeca(): void {
    this.editandoBeca = !this.editandoBeca;
    this.becaError = null;
    this.becaOk = null;

    if (this.editandoBeca && this.data) {
      this.becaForm = {
        tieneBeca: this.data.tieneBeca ?? false,
        porcentajeBecaSocial: this.data.porcentajeBecaSocial ?? 0,
        porcentajeBecaDeportiva: this.data.porcentajeBecaDeportiva ?? 0,
        porcentajeBecaPreparacionFisica: this.data.porcentajeBecaPreparacionFisica ?? 0,
        observacionBeca: this.data.observacionBeca ?? '',
      };
    }

    this.cdr.detectChanges();
  }

  guardarBeca(): void {
    if (!this.data?.socioId) {
      this.becaError = 'Socio inválido';
      this.cdr.detectChanges();
      return;
    }

    const social = Number(this.becaForm.porcentajeBecaSocial ?? 0);
    const deportiva = Number(this.becaForm.porcentajeBecaDeportiva ?? 0);
    const prep = Number(this.becaForm.porcentajeBecaPreparacionFisica ?? 0);

    if (social < 0 || social > 100 || deportiva < 0 || deportiva > 100 || prep < 0 || prep > 100) {
      this.becaError = 'Los porcentajes de beca deben estar entre 0 y 100';
      this.cdr.detectChanges();
      return;
    }

    this.guardandoBeca = true;
    this.becaError = null;
    this.becaOk = null;
    this.cdr.detectChanges();

    this.api
      .actualizarBeca(this.data.socioId, {
        tieneBeca: !!this.becaForm.tieneBeca,
        porcentajeBecaSocial: this.becaForm.tieneBeca ? social : 0,
        porcentajeBecaDeportiva: this.becaForm.tieneBeca ? deportiva : 0,
        porcentajeBecaPreparacionFisica: this.becaForm.tieneBeca ? prep : 0,
        observacionBeca: this.becaForm.tieneBeca ? this.becaForm.observacionBeca?.trim() || '' : '',
      })
      .pipe(
        finalize(() => {
          this.guardandoBeca = false;
          this.cdr.detectChanges();
        }),
      )
      .subscribe({
        next: (res: any) => {
          if (res?.status !== 200 && res?.status !== 201) {
            this.becaError = res?.mensaje || 'No se pudo actualizar la beca';
            this.cdr.detectChanges();
            return;
          }

          this.becaOk = 'Beca actualizada correctamente';
          this.editandoBeca = false;
          this.cargar(this.data!.socioId);
        },
        error: (err) => {
          this.becaError = err?.error?.mensaje || 'Error actualizando la beca';
          this.cdr.detectChanges();
        },
      });
  }

  resetAgregarDisciplinaForm(): void {
    this.agregarDisciplinaForm = {
      disciplinaId: null,
      arancelDisciplinaId: null,
      inscripcionPagada: false,
    };
    this.arancelesNuevaDisciplina = [];
  }

  cargarDisciplinasDisponibles(): void {
    this.http.get<any>(`${environment.apiUrl}/api/admin/disciplinas`).subscribe({
      next: (res) => {
        const todas: DisciplinaDto[] = res?.data ?? [];
        const idsActuales = new Set((this.data?.disciplinas ?? []).map((d) => d.disciplinaId));

        this.disciplinasDisponibles = todas.filter(
          (d) => d?.id != null && !idsActuales.has(d.id) && d.activa !== false,
        );

        this.cdr.detectChanges();
      },
      error: () => {
        this.disciplinasDisponibles = [];
        this.cdr.detectChanges();
      },
    });
  }

  onNuevaDisciplinaChange(): void {
    const disciplinaId = this.agregarDisciplinaForm.disciplinaId;

    this.agregarDisciplinaForm.arancelDisciplinaId = null;
    this.arancelesNuevaDisciplina = [];

    if (!disciplinaId) {
      this.cdr.detectChanges();
      return;
    }

    this.pagosApi.getArancelesPorDisciplina(disciplinaId).subscribe({
      next: (res) => {
        this.arancelesNuevaDisciplina = res?.data ?? [];
        this.cdr.detectChanges();
      },
      error: () => {
        this.arancelesNuevaDisciplina = [];
        this.cdr.detectChanges();
      },
    });
  }

  guardarNuevaDisciplina(): void {
    if (!this.data?.socioId) {
      this.agregarDisciplinaError = 'Socio inválido';
      this.cdr.detectChanges();
      return;
    }

    if (!this.agregarDisciplinaForm.disciplinaId) {
      this.agregarDisciplinaError = 'Debés seleccionar una disciplina';
      this.cdr.detectChanges();
      return;
    }

    if (!this.agregarDisciplinaForm.arancelDisciplinaId) {
      this.agregarDisciplinaError = 'Debés seleccionar una categoría/arancel';
      this.cdr.detectChanges();
      return;
    }

    this.agregarDisciplinaLoading = true;
    this.agregarDisciplinaError = null;
    this.agregarDisciplinaOk = null;
    this.cdr.detectChanges();

    this.api
      .agregarDisciplina(this.data.socioId, {
        disciplinaId: this.agregarDisciplinaForm.disciplinaId,
        arancelDisciplinaId: this.agregarDisciplinaForm.arancelDisciplinaId,
        inscripcionPagada: this.agregarDisciplinaForm.inscripcionPagada,
      })
      .pipe(
        finalize(() => {
          this.agregarDisciplinaLoading = false;
          this.cdr.detectChanges();
        }),
      )
      .subscribe({
        next: (res: any) => {
          if (res?.status !== 200 && res?.status !== 201) {
            this.agregarDisciplinaError = res?.mensaje || 'No se pudo agregar la disciplina';
            this.cdr.detectChanges();
            return;
          }

          const socioId = this.data!.socioId;

          this.agregarDisciplinaOk = 'Disciplina agregada correctamente';
          this.resetAgregarDisciplinaForm();
          this.mostrarAgregarDisciplina = false;
          this.cargar(socioId);
          this.cdr.detectChanges();
        },
        error: (err) => {
          this.agregarDisciplinaError = err?.error?.mensaje || 'Error al agregar la disciplina';
          this.cdr.detectChanges();
        },
      });
  }

  onConceptoChangeDisciplina(d: SocioDisciplinaResumenVm): void {
    const form = this.pagoForms[d.disciplinaId];
    if (!form) return;

    if (form.concepto === 'INSCRIPCION') {
      this.arancelSeleccionadoPorDisciplina[d.disciplinaId] = null;
      form.periodo = null as any;
      form.arancelDisciplinaId = null as any;
      form.categoria = null as any;
      form.montoSocial = 0;
      form.montoDisciplina = 0;
      form.montoPreparacionFisica = 0;
      form.montoTotal = 0;
      this.cdr.detectChanges();
      return;
    }

    const aranceles = this.arancelesPorDisciplina[d.disciplinaId] ?? [];
    const seleccionado =
      aranceles.find((a: ArancelDisciplinaDto) => a.id === d.arancelDisciplinaId) ??
      aranceles[0] ??
      null;

    if (seleccionado) {
      form.arancelDisciplinaId = seleccionado.id;
      this.arancelSeleccionadoPorDisciplina[d.disciplinaId] = seleccionado;

      this.cargarMontosConBeca(d, seleccionado);
    }

    this.autocompletarPagoDisciplina(d);
    this.cdr.detectChanges();
  }

  onArancelChangeDisciplina(d: SocioDisciplinaResumenVm): void {
    const form = this.pagoForms[d.disciplinaId];
    if (!form) return;

    if (form.concepto === 'INSCRIPCION') {
      this.arancelSeleccionadoPorDisciplina[d.disciplinaId] = null;
      form.arancelDisciplinaId = null as any;
      form.categoria = null as any;
      form.montoSocial = 0;
      form.montoDisciplina = 0;
      form.montoPreparacionFisica = 0;
      form.montoTotal = 0;
      return;
    }

    const arancelFijoId = d.arancelDisciplinaId ?? null;
    if (arancelFijoId != null) {
      form.arancelDisciplinaId = arancelFijoId;
    }

    const id = Number(form.arancelDisciplinaId);
    const aranceles = this.arancelesPorDisciplina[d.disciplinaId] ?? [];
    const seleccionado = aranceles.find((a: ArancelDisciplinaDto) => a.id === id) ?? null;

    this.arancelSeleccionadoPorDisciplina[d.disciplinaId] = seleccionado;

    if (!seleccionado) {
      form.categoria = null as any;
      form.montoSocial = 0;
      form.montoDisciplina = 0;
      form.montoPreparacionFisica = 0;
      form.montoTotal = 0;
      return;
    }

    this.cargarMontosConBeca(d, seleccionado);

    if (
      form.concepto === 'CUOTA_MENSUAL' &&
      !d.deuda?.items?.some((i) => !i.pagado && i.periodo !== 'INSCRIPCION')
    ) {
      this.autocompletarPagoDisciplina(d);
    }
  }

  recalcularMontoTotalDisciplina(d: SocioDisciplinaResumenVm): void {
    const form = this.pagoForms[d.disciplinaId];
    if (!form) return;

    const total =
      Number(form.montoSocial ?? 0) +
      Number(form.montoDisciplina ?? 0) +
      Number(form.montoPreparacionFisica ?? 0);

    form.montoTotal = total;
  }

  private aplicarBeca(montoBase: number, porcentajeBeca: number | null | undefined): number {
    const base = Number(montoBase ?? 0);
    const beca = Number(porcentajeBeca ?? 0);

    if (beca <= 0) return base;
    if (beca >= 100) return 0;

    const descuento = (base * beca) / 100;
    return Number((base - descuento).toFixed(2));
  }

  private cargarMontosConBeca(d: SocioDisciplinaResumenVm, arancel: ArancelDisciplinaDto): void {
    const form = this.pagoForms[d.disciplinaId];
    if (!form || !this.data) return;

    const esPrincipal = this.esDisciplinaPrincipal(d);

    form.categoria = arancel.categoria;
    form.montoSocial = esPrincipal
      ? this.aplicarBeca(Number(arancel.montoSocial ?? 0), this.data.porcentajeBecaSocial)
      : 0;

    form.montoDisciplina = this.aplicarBeca(
      Number(arancel.montoDeportivo ?? 0),
      this.data.porcentajeBecaDeportiva,
    );

    form.montoPreparacionFisica = this.aplicarBeca(
      Number(arancel.montoPreparacionFisica ?? 0),
      this.data.porcentajeBecaPreparacionFisica,
    );

    this.recalcularMontoTotalDisciplina(d);
  }

  autocompletarPagoDisciplina(d: SocioDisciplinaResumenVm): void {
    const form = this.pagoForms[d.disciplinaId];
    if (!form) return;

    const deuda = d.deuda;

    if (form.concepto === 'CUOTA_MENSUAL') {
      const primerAdeudado = deuda?.items?.find((i) => !i.pagado && i.periodo !== 'INSCRIPCION');

      if (primerAdeudado) {
        form.periodo = primerAdeudado.periodo as any;
        form.montoTotal = Number(primerAdeudado.monto ?? 0);
      } else {
        const actual = this.periodoActual();
        const arancel = this.arancelSeleccionadoPorDisciplina[d.disciplinaId];

        if (arancel?.vigenteDesde) {
          const periodoArancel = this.periodoDesdeFecha(arancel.vigenteDesde as any);
          form.periodo = this.maxPeriodo(actual, periodoArancel) as any;
        } else {
          form.periodo = actual as any;
        }
      }

      const arancel = this.arancelSeleccionadoPorDisciplina[d.disciplinaId];
      if (arancel) {
        this.cargarMontosConBeca(d, arancel);
      }

      if (primerAdeudado) {
        form.montoTotal = Number(primerAdeudado.monto ?? 0);
      }

      return;
    }

    if (form.concepto === 'INSCRIPCION') {
      form.periodo = null as any;
      form.arancelDisciplinaId = null as any;
      form.categoria = null as any;
      form.montoSocial = 0;
      form.montoDisciplina = 0;
      form.montoPreparacionFisica = 0;
    }
  }

  resetPagoFormDisciplina(d: SocioDisciplinaResumenVm): void {
    if (!this.data) return;

    this.pagoForms[d.disciplinaId] = {
      socioId: this.data.socioId,
      concepto: 'CUOTA_MENSUAL',
      periodo: '',
      disciplinaId: d.disciplinaId,
      arancelDisciplinaId: d.arancelDisciplinaId ?? null,
      categoria: d.categoriaArancel ?? null,
      montoTotal: 0,
      montoSocial: 0,
      montoDisciplina: 0,
      montoPreparacionFisica: 0,
      medio: 'EFECTIVO',
      observacion: '',
    };

    const aranceles = this.arancelesPorDisciplina[d.disciplinaId] ?? [];
    const seleccionado =
      aranceles.find((a: ArancelDisciplinaDto) => a.id === d.arancelDisciplinaId) ??
      aranceles[0] ??
      null;

    this.arancelSeleccionadoPorDisciplina[d.disciplinaId] = seleccionado;

    if (seleccionado) {
      this.pagoForms[d.disciplinaId].arancelDisciplinaId = seleccionado.id;
      this.onArancelChangeDisciplina(d);
    }

    this.autocompletarPagoDisciplina(d);
  }

  registrarPagoDisciplina(d: SocioDisciplinaResumenVm): void {
    const form = this.pagoForms[d.disciplinaId];
    if (!form) return;

    this.pagoErrorPorDisciplina[d.disciplinaId] = null;
    this.pagoOkPorDisciplina[d.disciplinaId] = null;
    this.cdr.detectChanges();

    if (!form.socioId) {
      this.pagoErrorPorDisciplina[d.disciplinaId] = 'Socio inválido';
      this.cdr.detectChanges();
      return;
    }

    if (!form.disciplinaId) {
      this.pagoErrorPorDisciplina[d.disciplinaId] = 'Disciplina inválida';
      this.cdr.detectChanges();
      return;
    }

    if (form.concepto === 'CUOTA_MENSUAL') {
      const per = (form.periodo ?? '').trim();

      if (!/^\d{4}-\d{2}$/.test(per)) {
        this.pagoErrorPorDisciplina[d.disciplinaId] =
          'Periodo inválido. Formato: YYYY-MM (ej: 2026-04)';
        this.cdr.detectChanges();
        return;
      }

      form.periodo = per as any;

      if (!form.arancelDisciplinaId) {
        this.pagoErrorPorDisciplina[d.disciplinaId] = 'Debés seleccionar una categoría/arancel';
        this.cdr.detectChanges();
        return;
      }

      if (this.periodoEsAnterior(per, '2026-04')) {
        this.pagoErrorPorDisciplina[d.disciplinaId] =
          'Solo se permiten cuotas mensuales desde 2026-04. Para períodos anteriores, registralo como ingreso manual.';
        this.cdr.detectChanges();
        return;
      }

      form.montoTotal = Number(form.montoTotal ?? 0);
    } else {
      form.periodo = null as any;
      form.arancelDisciplinaId = null as any;
      form.categoria = null as any;
      form.montoSocial = 0;
      form.montoDisciplina = 0;
      form.montoPreparacionFisica = 0;
      form.montoTotal = Number(form.montoTotal ?? 0);
    }

    if (!(form.medio as any)?.trim()) {
      this.pagoErrorPorDisciplina[d.disciplinaId] = 'Medio es obligatorio';
      this.cdr.detectChanges();
      return;
    }

    if (!form.montoTotal || form.montoTotal <= 0) {
      this.pagoErrorPorDisciplina[d.disciplinaId] = 'Monto total debe ser mayor a 0';
      this.cdr.detectChanges();
      return;
    }

    this.loading = true;
    this.cdr.detectChanges();

    this.pagosApi
      .registrarManual(form)
      .pipe(
        finalize(() => {
          this.loading = false;
          this.cdr.detectChanges();
        }),
      )
      .subscribe({
        next: (res: any) => {
          if (res.status !== 200 && res.status !== 201) {
            this.pagoErrorPorDisciplina[d.disciplinaId] =
              res.mensaje || 'No se pudo registrar el pago';
            this.cdr.detectChanges();
            return;
          }

          this.pagoOkPorDisciplina[d.disciplinaId] = 'Pago registrado correctamente';
          this.ultimoPagoRegistradoId = res?.data?.pagoId ?? null;

          const socioId = form.socioId;
          this.cargar(socioId);
          this.resetPagoFormDisciplina(d);
          this.statsSvc.refresh();
          this.cdr.detectChanges();
        },
        error: (err) => {
          this.pagoErrorPorDisciplina[d.disciplinaId] =
            err?.error?.mensaje || 'Error registrando pago';
          this.cdr.detectChanges();
        },
      });
  }

  getPagosPorDisciplina(d: SocioDisciplinaResumenVm): PagoDto[] {
    if (!this.data) return [];
    return (this.data.ultimosPagos ?? []).filter((p) => p.disciplinaId === d.disciplinaId);
  }

  getItemsAdeudadosPorDisciplina(d: SocioDisciplinaResumenVm): any[] {
    const items = d.deuda?.items ?? [];
    return items.filter((i) => !i.pagado);
  }

  getEstadoItemDeuda(item: any): string {
    if (!item) return '-';

    if (item.periodo === 'INSCRIPCION') {
      return item.pagado ? 'Pagada' : 'Pendiente';
    }

    return item.pagado ? 'Pagado' : 'Debe';
  }

  getPeriodosAdeudadosPorDisciplina(d: SocioDisciplinaResumenVm): string[] {
    const items = d.deuda?.items ?? [];
    return items.filter((i) => !i.pagado && !!i.periodo).map((i) => i.periodo);
  }

  tieneDeudaDisciplina(d: SocioDisciplinaResumenVm): boolean {
    const items = d.deuda?.items ?? [];
    return items.some((i) => !i.pagado);
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
    if (!iso) return '-';

    const fecha = iso.endsWith('Z') ? iso : iso + 'Z';
    const d = new Date(fecha);

    return d.toLocaleString('es-AR', {
      year: 'numeric',
      month: '2-digit',
      day: '2-digit',
      hour: '2-digit',
      minute: '2-digit',
    });
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

  descargarComprobantePorPago(pagoId: number): void {
    if (!pagoId) return;

    this.pagosApi.descargarComprobante(pagoId).subscribe({
      next: (blob: Blob) => {
        const url = window.URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = `comprobante_${pagoId}.pdf`;
        a.click();
        window.URL.revokeObjectURL(url);
      },
      error: () => {
        this.error = 'No se pudo descargar el comprobante';
        this.cdr.detectChanges();
      },
    });
  }
}
