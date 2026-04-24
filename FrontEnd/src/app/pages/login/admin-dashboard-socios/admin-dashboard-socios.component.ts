import {
  AfterViewChecked,
  ChangeDetectorRef,
  Component,
  ElementRef,
  OnInit,
  ViewChild,
  inject,
  HostListener,
} from '@angular/core';
import { CommonModule, DatePipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterModule } from '@angular/router';
import { finalize } from 'rxjs/operators';
import { Chart, DoughnutController, ArcElement, Tooltip, Legend } from 'chart.js';

import { DashboardService } from '../../../service/dashboard.service';
import { DisciplinasService } from '../../../service/disciplinas.service';
import { ExcelExportService } from '../../../service/excel-export.service';
import { DashboardSociosResumen, Socio } from '../../../models/dashboard.model';
import { DisciplinaDto } from '../../../features/disciplinas.models';

Chart.register(DoughnutController, ArcElement, Tooltip, Legend);

type SortColumn =
  | 'apellido'
  | 'nombre'
  | 'dni'
  | 'disciplinaNombre'
  | 'activo'
  | 'estadoPago'
  | 'vigenciaHasta';

@Component({
  selector: 'app-admin-dashboard-socios',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterModule],
  templateUrl: './admin-dashboard-socios.component.html',
  styleUrls: ['./admin-dashboard-socios.component.css'],
  providers: [DatePipe],
})
export class AdminDashboardSociosComponent implements OnInit, AfterViewChecked {
  private dashboardService = inject(DashboardService);
  private disciplinasService = inject(DisciplinasService);
  private excelExportService = inject(ExcelExportService);
  private cdr = inject(ChangeDetectorRef);
  private datePipe = inject(DatePipe);

  @ViewChild('chartActivos') chartActivosRef?: ElementRef<HTMLCanvasElement>;
  @ViewChild('chartPagos') chartPagosRef?: ElementRef<HTMLCanvasElement>;

  private chartActivos?: Chart;
  private chartPagos?: Chart;
  private chartsPendientes = false;

  cargando = false;
  cargandoDisciplinas = false;
  error: string | null = null;

  resumen: DashboardSociosResumen | null = null;
  socios: Socio[] = [];
  disciplinas: DisciplinaDto[] = [];
  mesActual = '';

  filtros = {
    disciplinaId: '',
    activo: '',
    categoria: '',
    estadoPago: '',
    q: '',
  };

  categoriasFiltradas: string[] = [];
  arancelesPorDisciplina: Record<number, string[]> = {
    1: ['FEDERADO', 'INICIAL 1', 'INICIAL 2', 'TRES DISCIPLINAS'],
    2: ['MINI/U13'],
    3: [],
    4: ['INFANTILES', 'INFERIORES', 'PRIMERA'],
    5: [],
    6: [],
  };

  // ORDEN
  sortColumn: SortColumn = 'apellido';
  sortDirection: 'asc' | 'desc' = 'asc';

  // PAGINACION
  currentPage = 1;
  pageSize = 10;

  Math = Math;

  ngOnInit(): void {
    this.mesActual = this.datePipe.transform(new Date(), "MMMM 'de' yyyy", 'es-AR') ?? '';
    this.cargarDisciplinas();
    this.cargarDashboard();
  }

  ngAfterViewChecked(): void {
    if (this.chartsPendientes) {
      this.chartsPendientes = false;
      this.renderCharts();
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

    const activo = this.filtros.activo === '' ? null : this.filtros.activo === 'true';

    const estadoPago = this.filtros.estadoPago === '' ? null : this.filtros.estadoPago;

    this.dashboardService
      .obtenerDashboardSocios({
        disciplinaId,
        activo,
        categoria: this.filtros.categoria || null,
        estadoPago,
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
            this.error = res.mensaje || 'No se pudo cargar el dashboard';
            this.cdr.detectChanges();
            return;
          }

          this.resumen = res.data.resumen;
          this.socios = res.data.socios ?? [];
          this.currentPage = 1;
          this.chartsPendientes = true;
          this.cdr.detectChanges();
        },
        error: () => {
          this.error = 'Error al cargar el dashboard';
          this.cdr.detectChanges();
        },
      });
  }

  limpiarFiltros(): void {
    this.filtros = {
      disciplinaId: '',
      activo: '',
      categoria: '',
      estadoPago: '',
      q: '',
    };
    this.categoriasFiltradas = [];
    this.currentPage = 1;
    this.sortColumn = 'apellido';
    this.sortDirection = 'asc';
    this.cargarDashboard();
  }

  exportarSocios(): void {
    if (!this.socios || this.socios.length === 0) {
      return;
    }

    const nombreDisciplina = this.filtros.disciplinaId
      ? (this.disciplinas.find((d) => d.id === Number(this.filtros.disciplinaId))?.nombre ??
        'disciplina')
      : 'todos';

    const estadoSocio =
      this.filtros.activo === ''
        ? 'todos'
        : this.filtros.activo === 'true'
          ? 'activos'
          : 'inactivos';

    const estadoPago =
      this.filtros.estadoPago === ''
        ? 'todos'
        : this.filtros.estadoPago === 'AL_DIA'
          ? 'al_dia'
          : 'debe';

    const busqueda = this.filtros.q?.trim()
      ? this.filtros.q.trim().replace(/\s+/g, '_')
      : 'sin_busqueda';

    const data = this.sociosOrdenados.map((s, index) => ({
      Nro: index + 1,
      Apellido: s.apellido,
      Nombre: s.nombre,
      DNI: s.dni,
      Disciplina: s.disciplinaNombre ?? '-',
      Categoría: s.categoriaArancel ?? '-',
      'Estado socio': s.activo ? 'Activo' : 'Inactivo',
      'Estado pago': s.estadoPago === 'AL_DIA' ? 'Al día' : 'Debe',
      'Vigencia hasta': s.vigenciaHasta ?? '-',
    }));

    const fileName = `socios_${nombreDisciplina}_${estadoSocio}_${estadoPago}_${busqueda}`;

    this.excelExportService.exportToExcel(data, fileName, 'Socios');
  }

  private renderCharts(): void {
    if (!this.resumen) return;
    if (!this.chartActivosRef?.nativeElement || !this.chartPagosRef?.nativeElement) return;

    if (this.chartActivos) {
      this.chartActivos.destroy();
    }

    if (this.chartPagos) {
      this.chartPagos.destroy();
    }

    this.chartActivos = new Chart(this.chartActivosRef.nativeElement, {
      type: 'doughnut',
      data: {
        labels: ['Activos', 'Inactivos'],
        datasets: [
          {
            data: [this.resumen.activos, this.resumen.inactivos],
            backgroundColor: ['#198754', '#6c757d'],
            hoverOffset: 10,
            borderWidth: 2,
          },
        ],
      },
      options: {
        responsive: true,
        maintainAspectRatio: false,
        plugins: {
          legend: { position: 'bottom' },
        },
      },
    });

    this.chartPagos = new Chart(this.chartPagosRef.nativeElement, {
      type: 'doughnut',
      data: {
        labels: ['Al día', 'Con deuda'],
        datasets: [
          {
            data: [this.resumen.alDia, this.resumen.debe],
            backgroundColor: ['#0d6efd', '#dc3545'],
            hoverOffset: 10,
            borderWidth: 2,
          },
        ],
      },
      options: {
        responsive: true,
        maintainAspectRatio: false,
        plugins: {
          legend: { position: 'bottom' },
        },
      },
    });
  }

  getEstadoPagoClass(estado: string): string {
    return estado === 'AL_DIA' ? 'badge bg-success' : 'badge bg-danger';
  }

  getEstadoActivoClass(activo: boolean): string {
    return activo ? 'badge bg-primary' : 'badge bg-secondary';
  }

  actualizarCategorias(): void {
    const disciplinaId =
      this.filtros.disciplinaId === '' ? null : Number(this.filtros.disciplinaId);

    if (!disciplinaId) {
      this.categoriasFiltradas = [];
      this.filtros.categoria = '';
      return;
    }

    this.categoriasFiltradas = this.arancelesPorDisciplina[disciplinaId] ?? [];
    if (!this.categoriasFiltradas.includes(this.filtros.categoria)) {
      this.filtros.categoria = '';
    }
  }

  // -------------------------
  // ORDEN
  // -------------------------
  sortBy(column: SortColumn): void {
    if (this.sortColumn === column) {
      this.sortDirection = this.sortDirection === 'asc' ? 'desc' : 'asc';
    } else {
      this.sortColumn = column;
      this.sortDirection = 'asc';
    }

    this.currentPage = 1;
  }

  getSortIcon(column: SortColumn): string {
    if (this.sortColumn !== column) return 'bi bi-arrow-down-up';
    return this.sortDirection === 'asc' ? 'bi bi-sort-down-alt' : 'bi bi-sort-up';
  }

  get sociosOrdenados(): Socio[] {
    const lista = [...this.socios];

    lista.sort((a, b) => {
      const valueA = this.getSortValue(a, this.sortColumn);
      const valueB = this.getSortValue(b, this.sortColumn);

      if (valueA < valueB) return this.sortDirection === 'asc' ? -1 : 1;
      if (valueA > valueB) return this.sortDirection === 'asc' ? 1 : -1;
      return 0;
    });

    return lista;
  }

  private getSortValue(s: Socio, column: SortColumn): string | number {
    switch (column) {
      case 'apellido':
        return (s.apellido ?? '').toLowerCase();
      case 'nombre':
        return (s.nombre ?? '').toLowerCase();
      case 'dni':
        return (s.dni ?? '').toString();
      case 'disciplinaNombre':
        return (s.disciplinaNombre ?? '').toLowerCase();
      case 'activo':
        return s.activo ? 1 : 0;
      case 'estadoPago':
        return (s.estadoPago ?? '').toLowerCase();
      case 'vigenciaHasta':
        return s.vigenciaHasta ?? '';
      default:
        return '';
    }
  }

  // -------------------------
  // PAGINACION
  // -------------------------
  get totalPages(): number {
    return Math.max(1, Math.ceil(this.sociosOrdenados.length / this.pageSize));
  }

  get sociosPaginados(): Socio[] {
    const start = (this.currentPage - 1) * this.pageSize;
    return this.sociosOrdenados.slice(start, start + this.pageSize);
  }

  get paginas(): number[] {
    const total = this.totalPages;
    return Array.from({ length: total }, (_, i) => i + 1);
  }

  get paginasVisibles(): number[] {
    const total = this.totalPages;
    const maxVisible = 5;

    let start = Math.max(1, this.currentPage - 2);
    let end = start + maxVisible - 1;

    if (end > total) {
      end = total;
      start = Math.max(1, end - maxVisible + 1);
    }

    return Array.from({ length: end - start + 1 }, (_, i) => start + i);
  }

  irAPagina(page: number): void {
    if (page < 1 || page > this.totalPages) return;
    this.currentPage = page;
  }

  paginaAnterior(): void {
    if (this.currentPage > 1) {
      this.currentPage--;
    }
  }

  paginaSiguiente(): void {
    if (this.currentPage < this.totalPages) {
      this.currentPage++;
    }
  }
}
