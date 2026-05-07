import {
  AfterViewChecked,
  ChangeDetectorRef,
  Component,
  ElementRef,
  OnInit,
  ViewChild,
  inject,
} from '@angular/core';
import { CommonModule, DatePipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterModule } from '@angular/router';
import { finalize } from 'rxjs/operators';
import { Chart, DoughnutController, ArcElement, Tooltip, Legend } from 'chart.js';

import { DashboardIngresosService } from '../../../service/dashboard-ingresos.service';
import { DashboardIngresosResumen, IngresoDashboardItem } from '../../../models/ingresos.model';
import { IngresosManualesService } from '../../../service/ingresos-manuales.service';
import { IngresoManualCreateRequest } from '../../../models/ingreso-manual.model';
import { ExcelExportService } from '../../../service/excel-export.service';
import { DisciplinasService } from '../../../service/disciplinas.service';
import { DisciplinaDto } from '../../../features/disciplinas.models';

Chart.register(DoughnutController, ArcElement, Tooltip, Legend);

@Component({
  selector: 'app-admin-dashboard-ingresos',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterModule],
  templateUrl: './admin-dashboard-ingresos.component.html',
  styleUrls: ['./admin-dashboard-ingresos.component.css'],
  providers: [DatePipe],
})
export class AdminDashboardIngresosComponent implements OnInit, AfterViewChecked {
  private dashboardIngresosService = inject(DashboardIngresosService);
  private ingresosManualesService = inject(IngresosManualesService);
  private excelExportService = inject(ExcelExportService);
  private disciplinasService = inject(DisciplinasService);
  private cdr = inject(ChangeDetectorRef);
  private datePipe = inject(DatePipe);

  @ViewChild('chartMedios') chartMediosRef?: ElementRef<HTMLCanvasElement>;

  private chartMedios?: Chart;
  private chartsPendientes = false;

  cargando = false;
  cargandoDisciplinas = false;
  guardandoManual = false;
  error: string | null = null;
  ok: string | null = null;
  guardandoEdicion = false;
  mostrarModalEditar = false;
  editandoId: number | null = null;

  resumen: DashboardIngresosResumen | null = null;
  ingresos: IngresoDashboardItem[] = [];
  ingresosFiltrados: IngresoDashboardItem[] = [];
  disciplinas: DisciplinaDto[] = [];
  mesActual = '';

  paginaActual = 1;
  tamanoPagina = 10;
  opcionesTamano = [5, 10, 20, 50];

  sortColumn:
    | 'fecha'
    | 'origen'
    | 'socio'
    | 'disciplina'
    | 'categoria'
    | 'concepto'
    | 'periodo'
    | 'medio'
    | 'monto' = 'fecha';
  sortDirection: 'asc' | 'desc' = 'desc';

  medios: string[] = ['EFECTIVO', 'TRANSFERENCIA', 'BANCO', 'MERCADO_PAGO', 'TARJETA', 'OTRO'];

  filtros = {
    medio: '',
    disciplinaId: '',
    categoriaManual: '',
    fechaDesde: '',
    fechaHasta: '',
    q: '',
  };

  editFormIngresoManual: IngresoManualCreateRequest = {
    fecha: '',
    categoria: '',
    medioPago: '',
    monto: null,
    descripcion: '',
    concepto: '',
  };

  categoriasIngresoManual: string[] = [
    'CUOTAS_ATRASADAS',
    'DIEF',
    'DEPOSITOS',
    'EVENTOS',
    'FUTBOL_ALQUILERES',
    'FUTBOL_TORNEOS',
    'INSCRIPCIONES',
    'KIOSCO_ALQUILERES',
    'MANTOVANI',
    'MUTUAL',
    'PLAYAS',
  ];

  mediosIngresoManual: string[] = ['BANCO', 'EFECTIVO'];

  formIngresoManual: IngresoManualCreateRequest = {
    fecha: new Date().toISOString().slice(0, 10),
    categoria: '',
    medioPago: '',
    monto: null,
    descripcion: '',
  };

  ngOnInit(): void {
    this.mesActual = this.datePipe.transform(new Date(), "MMMM 'de' yyyy", 'es-AR') ?? '';
    this.cargarDisciplinas();
    this.cargarDashboard();
  }

  ngAfterViewChecked(): void {
    if (this.chartsPendientes) {
      this.chartsPendientes = false;
      this.renderChartMedios();
    }
  }

  cargarDisciplinas(): void {
    this.cargandoDisciplinas = true;
    this.cdr.detectChanges();

    this.disciplinasService
      .listar()
      .pipe(
        finalize(() => {
          this.cargandoDisciplinas = false;
          this.cdr.detectChanges();
        }),
      )
      .subscribe({
        next: (res) => {
          if (res.status === 200 && res.data) {
            this.disciplinas = res.data;
          }
          this.cdr.detectChanges();
        },
        error: () => {
          this.cdr.detectChanges();
        },
      });
  }

  cargarDashboard(): void {
    this.error = null;
    this.cargando = true;
    this.cdr.detectChanges();

    const disciplinaId =
      this.filtros.disciplinaId === '' ? null : Number(this.filtros.disciplinaId);

    this.dashboardIngresosService
      .obtenerDashboard({
        disciplinaId,
        categoriaManual: this.filtros.categoriaManual || null,
        fechaDesde: this.filtros.fechaDesde || null,
        fechaHasta: this.filtros.fechaHasta || null,
        q: this.filtros.q || null,
      })
      .pipe(
        finalize(() => {
          this.cargando = false;
          this.cdr.detectChanges();
        }),
      )
      .subscribe({
        next: (res) => {
          if (res.status !== 200) {
            this.error = res.mensaje || 'No se pudo cargar el dashboard de ingresos';
            this.cdr.detectChanges();
            return;
          }

          this.resumen = res.data.resumen;
          this.ingresos = res.data.ingresos ?? [];
          this.ingresosFiltrados = [...this.ingresos];
          this.ordenarIngresos();
          this.paginaActual = 1;
          this.chartsPendientes = true;
          this.cdr.detectChanges();
        },
        error: () => {
          this.error = 'Error al cargar el dashboard de ingresos';
          this.cdr.detectChanges();
        },
      });
  }

  limpiarFiltros(): void {
    this.filtros = {
      medio: '',
      disciplinaId: '',
      categoriaManual: '',
      fechaDesde: '',
      fechaHasta: '',
      q: '',
    };
    this.paginaActual = 1;
    this.cargarDashboard();
  }

  guardarIngresoManual(): void {
    this.error = null;
    this.ok = null;

    if (!this.formIngresoManual.fecha) {
      this.error = 'La fecha es obligatoria';
      this.cdr.detectChanges();
      return;
    }

    if (!this.formIngresoManual.categoria) {
      this.error = 'La categoría es obligatoria';
      this.cdr.detectChanges();
      return;
    }

    if (!this.formIngresoManual.medioPago) {
      this.error = 'El medio de pago es obligatorio';
      this.cdr.detectChanges();
      return;
    }

    if (!this.formIngresoManual.monto || this.formIngresoManual.monto <= 0) {
      this.error = 'El monto debe ser mayor a 0';
      this.cdr.detectChanges();
      return;
    }

    this.guardandoManual = true;
    this.cdr.detectChanges();

    this.ingresosManualesService
      .crear({
        fecha: this.formIngresoManual.fecha,
        categoria: this.formIngresoManual.categoria,
        medioPago: this.formIngresoManual.medioPago,
        monto: this.formIngresoManual.monto,
        descripcion: this.formIngresoManual.descripcion?.trim() || '',
      })
      .pipe(
        finalize(() => {
          this.guardandoManual = false;
          this.cdr.detectChanges();
        }),
      )
      .subscribe({
        next: (res) => {
          if (res.status !== 200 && res.status !== 201) {
            this.error = res.mensaje || 'No se pudo registrar el ingreso manual';
            this.cdr.detectChanges();
            return;
          }

          this.ok = 'Ingreso manual registrado correctamente';
          this.formIngresoManual = {
            fecha: this.obtenerFechaArgentina(),
            categoria: '',
            medioPago: '',
            monto: null,
            descripcion: '',
          };

          this.paginaActual = 1;
          this.cargarDashboard();
          this.cdr.detectChanges();
        },
        error: () => {
          this.error = 'Error al registrar el ingreso manual';
          this.cdr.detectChanges();
        },
      });
  }

  exportarIngresos(): void {
    if (!this.ingresos || this.ingresos.length === 0) {
      return;
    }

    const disciplina = this.filtros.disciplinaId
      ? (this.disciplinas.find((d) => d.id === Number(this.filtros.disciplinaId))?.nombre ??
        'todas')
      : 'todas';

    const categoriaManual = this.filtros.categoriaManual || 'todas';
    const fechaDesde = this.filtros.fechaDesde || 'sin_desde';
    const fechaHasta = this.filtros.fechaHasta || 'sin_hasta';
    const busqueda = this.filtros.q?.trim()
      ? this.filtros.q.trim().replace(/\s+/g, '_')
      : 'sin_busqueda';

    const data = this.ingresos.map((i, index) => ({
      Nro: index + 1,
      Fecha: i.fecha,
      Origen: this.formatOrigen(i.origen),
      Socio: i.socioNombreCompleto || '-',
      Disciplina: i.disciplinaNombre || '-',
      Categoría: this.formatCategoria(i.categoria),
      Concepto: i.concepto || '-',
      Período: i.periodo || '-',
      Medio: this.formatMedio(i.medio),
      Monto: Number(i.monto ?? 0),
      Descripción: i.descripcion || '-',
    }));

    const fileName = `ingresos_${disciplina}_${categoriaManual}_${fechaDesde}_${fechaHasta}_${busqueda}`;

    this.excelExportService.exportToExcel(data, fileName, 'Ingresos');
  }

  ordenarPor(
    columna:
      | 'fecha'
      | 'origen'
      | 'socio'
      | 'disciplina'
      | 'categoria'
      | 'concepto'
      | 'periodo'
      | 'medio'
      | 'monto',
  ): void {
    if (this.sortColumn === columna) {
      this.sortDirection = this.sortDirection === 'asc' ? 'desc' : 'asc';
    } else {
      this.sortColumn = columna;
      this.sortDirection = columna === 'fecha' || columna === 'monto' ? 'desc' : 'asc';
    }

    this.ordenarIngresos();
    this.paginaActual = 1;
    this.cdr.detectChanges();
  }

  ordenarIngresos(): void {
    this.ingresosFiltrados = [...this.ingresosFiltrados].sort((a, b) => {
      let valorA: any;
      let valorB: any;

      switch (this.sortColumn) {
        case 'fecha':
          valorA = this.normalizar(a.fecha);
          valorB = this.normalizar(b.fecha);
          break;
        case 'origen':
          valorA = this.normalizar(this.formatOrigen(a.origen));
          valorB = this.normalizar(this.formatOrigen(b.origen));
          break;
        case 'socio':
          valorA = this.normalizar(a.socioNombreCompleto);
          valorB = this.normalizar(b.socioNombreCompleto);
          break;
        case 'disciplina':
          valorA = this.normalizar(a.disciplinaNombre);
          valorB = this.normalizar(b.disciplinaNombre);
          break;
        case 'categoria':
          valorA = this.normalizar(a.categoria);
          valorB = this.normalizar(b.categoria);
          break;
        case 'concepto':
          valorA = this.normalizar(a.concepto);
          valorB = this.normalizar(b.concepto);
          break;
        case 'periodo':
          valorA = this.normalizar(a.periodo);
          valorB = this.normalizar(b.periodo);
          break;
        case 'medio':
          valorA = this.normalizar(a.medio);
          valorB = this.normalizar(b.medio);
          break;
        case 'monto':
          valorA = Number(a.monto ?? 0);
          valorB = Number(b.monto ?? 0);
          break;
        default:
          valorA = this.normalizar(a.fecha);
          valorB = this.normalizar(b.fecha);
      }

      if (valorA < valorB) {
        return this.sortDirection === 'asc' ? -1 : 1;
      }

      if (valorA > valorB) {
        return this.sortDirection === 'asc' ? 1 : -1;
      }

      return 0;
    });
  }

  esColumnaActiva(
    columna:
      | 'fecha'
      | 'origen'
      | 'socio'
      | 'disciplina'
      | 'categoria'
      | 'concepto'
      | 'periodo'
      | 'medio'
      | 'monto',
  ): boolean {
    return this.sortColumn === columna;
  }

  iconoOrden(
    columna:
      | 'fecha'
      | 'origen'
      | 'socio'
      | 'disciplina'
      | 'categoria'
      | 'concepto'
      | 'periodo'
      | 'medio'
      | 'monto',
  ): string {
    if (this.sortColumn !== columna) {
      return 'bi-arrow-down-up';
    }

    return this.sortDirection === 'asc' ? 'bi-sort-down-alt' : 'bi-sort-down';
  }

  get totalPaginas(): number {
    return Math.max(1, Math.ceil(this.ingresosFiltrados.length / this.tamanoPagina));
  }

  get ingresosPaginados(): IngresoDashboardItem[] {
    const inicio = (this.paginaActual - 1) * this.tamanoPagina;
    const fin = inicio + this.tamanoPagina;
    return this.ingresosFiltrados.slice(inicio, fin);
  }

  get inicioRegistro(): number {
    if (this.ingresosFiltrados.length === 0) return 0;
    return (this.paginaActual - 1) * this.tamanoPagina + 1;
  }

  get finRegistro(): number {
    return Math.min(this.paginaActual * this.tamanoPagina, this.ingresosFiltrados.length);
  }

  irAPagina(pagina: number): void {
    if (pagina < 1 || pagina > this.totalPaginas) return;
    this.paginaActual = pagina;
    this.cdr.detectChanges();
  }

  paginaAnterior(): void {
    this.irAPagina(this.paginaActual - 1);
  }

  paginaSiguiente(): void {
    this.irAPagina(this.paginaActual + 1);
  }

  cambiarTamanoPagina(): void {
    this.paginaActual = 1;
    this.cdr.detectChanges();
  }

  get paginasVisibles(): number[] {
    const total = this.totalPaginas;
    const actual = this.paginaActual;

    let desde = Math.max(1, actual - 2);
    let hasta = Math.min(total, actual + 2);

    if (actual <= 3) {
      hasta = Math.min(total, 5);
    }

    if (actual >= total - 2) {
      desde = Math.max(1, total - 4);
    }

    const paginas: number[] = [];
    for (let i = desde; i <= hasta; i++) {
      paginas.push(i);
    }

    return paginas;
  }

  private renderChartMedios(): void {
    if (!this.chartMediosRef?.nativeElement) return;

    const acumulado = new Map<string, number>();

    for (const ingreso of this.ingresos) {
      const key = this.obtenerNombreGrupoDisciplina(ingreso);
      const actual = acumulado.get(key) ?? 0;
      acumulado.set(key, actual + Number(ingreso.monto ?? 0));
    }

    const entries = Array.from(acumulado.entries())
      .filter(([, monto]) => monto > 0)
      .sort((a, b) => b[1] - a[1]);

    const labels = entries.map(([disciplina]) => disciplina);
    const data = entries.map(([, monto]) => monto);

    if (this.chartMedios) {
      this.chartMedios.destroy();
    }

    this.chartMedios = new Chart(this.chartMediosRef.nativeElement, {
      type: 'doughnut',
      data: {
        labels,
        datasets: [
          {
            data,
            backgroundColor: [
              '#0d6efd',
              '#198754',
              '#6610f2',
              '#fd7e14',
              '#20c997',
              '#dc3545',
              '#6f42c1',
              '#ffc107',
              '#6c757d',
              '#0dcaf0',
            ],
            borderWidth: 2,
          },
        ],
      },
      options: {
        responsive: true,
        maintainAspectRatio: false,
        plugins: {
          legend: { position: 'bottom' },
          tooltip: {
            callbacks: {
              label: (context) => {
                const label = context.label || '';
                const value = Number(context.raw || 0);
                const dataset = context.dataset.data as number[];
                const total = dataset.reduce((acc, item) => acc + Number(item), 0);
                const porcentaje = total > 0 ? ((value / total) * 100).toFixed(1) : '0.0';

                return `${label}: ${value.toLocaleString('es-AR', {
                  style: 'currency',
                  currency: 'ARS',
                  minimumFractionDigits: 2,
                })} (${porcentaje}%)`;
              },
            },
          },
        },
      },
    });
  }

  abrirEditarIngresoManual(i: IngresoDashboardItem): void {
    this.error = null;
    this.ok = null;
    this.editandoId = (i as any).id ?? null;
    this.mostrarModalEditar = true;

    this.editFormIngresoManual = {
      fecha: i.fecha ? String(i.fecha).slice(0, 10) : '',
      categoria: i.categoria || '',
      medioPago: i.medio || '',
      monto: i.monto ?? null,
      descripcion: i.descripcion || '',
      concepto: i.concepto || '',
    };

    this.cdr.detectChanges();
  }

  private obtenerFechaArgentina(): string {
    const ahora = new Date();

    const offset = ahora.getTimezoneOffset();
    const local = new Date(ahora.getTime() - offset * 60000);

    return local.toISOString().split('T')[0];
  }

  cerrarEditarIngresoManual(): void {
    this.mostrarModalEditar = false;
    this.editandoId = null;
    this.editFormIngresoManual = {
      fecha: '',
      categoria: '',
      medioPago: '',
      monto: null,
      descripcion: '',
      concepto: '',
    };
    this.cdr.detectChanges();
  }

  guardarEdicionIngresoManual(): void {
    this.error = null;
    this.ok = null;

    if (!this.editandoId) {
      this.error = 'No se encontró el ingreso a editar';
      this.cdr.detectChanges();
      return;
    }

    if (!this.editFormIngresoManual.fecha) {
      this.error = 'La fecha es obligatoria';
      this.cdr.detectChanges();
      return;
    }

    if (!this.editFormIngresoManual.categoria) {
      this.error = 'La categoría es obligatoria';
      this.cdr.detectChanges();
      return;
    }

    if (!this.editFormIngresoManual.medioPago) {
      this.error = 'El medio de pago es obligatorio';
      this.cdr.detectChanges();
      return;
    }

    if (!this.editFormIngresoManual.monto || this.editFormIngresoManual.monto <= 0) {
      this.error = 'El monto debe ser mayor a 0';
      this.cdr.detectChanges();
      return;
    }

    this.guardandoEdicion = true;
    this.cdr.detectChanges();

    this.ingresosManualesService
      .actualizar(this.editandoId, {
        fecha: this.editFormIngresoManual.fecha,
        categoria: this.editFormIngresoManual.categoria,
        medioPago: this.editFormIngresoManual.medioPago,
        monto: this.editFormIngresoManual.monto,
        descripcion: this.editFormIngresoManual.descripcion?.trim() || '',
        concepto: this.editFormIngresoManual.concepto?.trim() || '',
      })
      .pipe(
        finalize(() => {
          this.guardandoEdicion = false;
          this.cdr.detectChanges();
        }),
      )
      .subscribe({
        next: (res) => {
          if (res.status !== 200 && res.status !== 201) {
            this.error = res.mensaje || 'No se pudo actualizar el ingreso manual';
            this.cdr.detectChanges();
            return;
          }

          this.ok = 'Ingreso manual actualizado correctamente';
          this.cerrarEditarIngresoManual();
          this.paginaActual = 1;
          this.cargarDashboard();
          this.cdr.detectChanges();
        },
        error: () => {
          this.error = 'Error al actualizar el ingreso manual';
          this.cdr.detectChanges();
        },
      });
  }

  private obtenerNombreGrupoDisciplina(ingreso: IngresoDashboardItem): string {
    const disciplina = ingreso.disciplinaNombre?.trim();

    if (disciplina) {
      return disciplina;
    }

    if (ingreso.origen === 'MANUAL') {
      return 'Ingresos manuales';
    }

    return 'Sin disciplina';
  }

  formatMedio(medio: string | null): string {
    if (!medio) return '-';
    return medio
      .replaceAll('_', ' ')
      .toLowerCase()
      .replace(/\b\w/g, (l) => l.toUpperCase());
  }

  formatOrigen(origen: string | null): string {
    if (!origen) return '-';
    return origen === 'CUOTA' ? 'Cuota' : 'Manual';
  }

  formatCategoria(categoria: string | null): string {
    if (!categoria) return '-';
    return categoria
      .replaceAll('_', ' ')
      .toLowerCase()
      .replace(/\b\w/g, (l) => l.toUpperCase());
  }

  private normalizar(valor: any): string {
    return (valor ?? '')
      .toString()
      .toLowerCase()
      .normalize('NFD')
      .replace(/[\u0300-\u036f]/g, '');
  }
}
