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

  resumen: DashboardIngresosResumen | null = null;
  ingresos: IngresoDashboardItem[] = [];
  disciplinas: DisciplinaDto[] = [];
  mesActual = '';

  medios: string[] = ['EFECTIVO', 'TRANSFERENCIA', 'BANCO', 'MERCADO_PAGO', 'TARJETA', 'OTRO'];

  filtros = {
    medio: '',
    disciplinaId: '',
    categoriaManual: '',
    fechaDesde: '',
    fechaHasta: '',
    q: '',
  };

  categoriasIngresoManual: string[] = [
    'DIEF',
    'EVENTOS',
    'FUTBOL_ALQUILERES',
    'FUTBOL_TORNEOS',
    'MANTOVANI',
    'MUTUAL',
    'PLAYAS',
    'CUOTAS ATRASADAS'
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
        // medio: this.filtros.medio || null,
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
          this.ingresos = res.data.ingresos;
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
            fecha: new Date().toISOString().slice(0, 10),
            categoria: '',
            medioPago: '',
            monto: null,
            descripcion: '',
          };

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
}