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
import {
  Chart,
  BarController,
  BarElement,
  CategoryScale,
  LinearScale,
  Tooltip,
  Legend,
} from 'chart.js';

import { GastosService } from '../../../service/gastos.service';
import { ExcelExportService } from '../../../service/excel-export.service';
import { DisciplinasService } from '../../../service/disciplinas.service';
import { DisciplinaDto } from '../../../features/disciplinas.models';
import {
  DashboardGastosResumen,
  Gasto,
  GastoCategoria,
  GastoCreateRequest,
} from '../../../models/gasto.model';

Chart.register(BarController, BarElement, CategoryScale, LinearScale, Tooltip, Legend);

@Component({
  selector: 'app-admin-dashboard-gastos',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterModule],
  templateUrl: './admin-dashboard-gastos.component.html',
  styleUrls: ['./admin-dashboard-gastos.component.css'],
  providers: [DatePipe],
})
export class AdminDashboardGastosComponent implements OnInit, AfterViewChecked {
  private gastosService = inject(GastosService);
  private excelExportService = inject(ExcelExportService);
  private disciplinasService = inject(DisciplinasService);
  private cdr = inject(ChangeDetectorRef);
  private datePipe = inject(DatePipe);

  @ViewChild('chartCategorias') chartCategoriasRef?: ElementRef<HTMLCanvasElement>;

  private chartCategorias?: Chart;
  private chartsPendientes = false;

  loading = false;
  cargando = false;
  cargandoDisciplinas = false;
  guardando = false;
  guardandoEdicion = false;
  error: string | null = null;
  ok: string | null = null;

  resumen: DashboardGastosResumen | null = null;
  gastos: Gasto[] = [];
  gastosFiltrados: Gasto[] = [];
  disciplinas: DisciplinaDto[] = [];
  mesActual = '';

  paginaActual = 1;
  tamanoPagina = 10;
  opcionesTamano = [5, 10, 20, 50];

  sortColumn: 'fecha' | 'categoria' | 'concepto' | 'medioPago' | 'monto' = 'fecha';
  sortDirection: 'asc' | 'desc' = 'desc';

  categorias: GastoCategoria[] = [
    'DISCIPLINAS',
    'EVENTOS',
    'HONORARIOS',
    'IMPUESTOS',
    'LIMPIEZA',
    'MANTENIMIENTO',
    'SUELDOS',
    'OTROS',
  ];

  conceptosPorCategoria: Record<string, string[]> = {
    IMPUESTOS: [
      'ADT',
      'Aguas cordobesas',
      'Arca',
      'Ecogas',
      'Epec',
      'Internet',
      'Seguros',
      'Telecom',
      'Otros',
    ],
    DISCIPLINAS: [],
    MANTENIMIENTO: ['Otros'],
    LIMPIEZA: ['Otros'],
    EVENTOS: ['Otros'],
    HONORARIOS: ['Aimar Alexis', 'Guerrero Santiago'],
    SUELDOS: [
      'Laso Marcos',
      'Maldonado Carmen',
      'Ramirez Patricia',
      'Rivas Yessica',
      'Toledo Gustavo',
    ],
    GASTOS_BANCARIOS: ['66', '82', '416', '417'],
    OTROS: ['Otros'],
  };

  filtros = {
    categoria: '',
    concepto: '',
    fechaDesde: '',
    fechaHasta: '',
    q: '',
  };

  form: GastoCreateRequest = {
    fecha: new Date().toISOString().slice(0, 10),
    categoria: '',
    concepto: '',
    descripcion: '',
    monto: null,
    medioPago: '',
  };

  mostrarModalEditar = false;
  editandoId: number | null = null;

  editForm: GastoCreateRequest = {
    fecha: '',
    categoria: '',
    concepto: '',
    descripcion: '',
    monto: null,
    medioPago: '',
  };

  ngOnInit(): void {
    this.mesActual = this.datePipe.transform(new Date(), "MMMM 'de' yyyy", 'es-AR') ?? '';
    this.cargarDisciplinas();
    this.cargarDashboard();
  }

  ngAfterViewChecked(): void {
    if (this.chartsPendientes) {
      this.chartsPendientes = false;
      this.renderChartCategorias();
    }
  }

  get conceptosDisponibles(): string[] {
    if (!this.form.categoria) return [];

    if (this.form.categoria === 'DISCIPLINAS') {
      return this.disciplinas
        .filter((d) => d?.nombre)
        .map((d) => d.nombre)
        .sort((a, b) => a.localeCompare(b));
    }

    return this.conceptosPorCategoria[this.form.categoria] || [];
  }

  get conceptosFiltroDisponibles(): string[] {
    if (!this.filtros.categoria) return [];

    if (this.filtros.categoria === 'DISCIPLINAS') {
      return this.disciplinas
        .filter((d) => d?.nombre)
        .map((d) => d.nombre)
        .sort((a, b) => a.localeCompare(b));
    }

    return this.conceptosPorCategoria[this.filtros.categoria] || [];
  }

  get conceptosEditarDisponibles(): string[] {
    if (!this.editForm.categoria) return [];

    if (this.editForm.categoria === 'DISCIPLINAS') {
      return this.disciplinas
        .filter((d) => d?.nombre)
        .map((d) => d.nombre)
        .sort((a, b) => a.localeCompare(b));
    }

    return this.conceptosPorCategoria[this.editForm.categoria] || [];
  }

  onCategoriaChange(): void {
    this.form.concepto = '';
  }

  onFiltroCategoriaChange(): void {
    this.filtros.concepto = '';
  }

  onEditarCategoriaChange(): void {
    this.editForm.concepto = '';
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
          } else {
            this.disciplinas = [];
          }
          this.cdr.detectChanges();
        },
        error: () => {
          this.disciplinas = [];
          this.cdr.detectChanges();
        },
      });
  }

  cargarDashboard(): void {
    this.error = null;
    this.cargando = true;
    this.cdr.detectChanges();

    this.gastosService
      .dashboard({
        categoria: this.filtros.categoria || null,
        concepto: this.filtros.concepto || null,
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
            this.error = res.mensaje || 'No se pudo cargar el dashboard de gastos';
            this.cdr.detectChanges();
            return;
          }

          this.resumen = res.data.resumen;
          this.gastos = res.data.gastos ?? [];
          this.gastosFiltrados = [...this.gastos];
          this.ordenarGastos();
          this.paginaActual = 1;
          this.chartsPendientes = true;
          this.cdr.detectChanges();
        },
        error: () => {
          this.error = 'Error al cargar el dashboard de gastos';
          this.cdr.detectChanges();
        },
      });
  }

  guardarGasto(): void {
    this.error = null;
    this.ok = null;

    if (!this.form.fecha) {
      this.error = 'La fecha es obligatoria';
      this.cdr.detectChanges();
      return;
    }

    if (!this.form.categoria) {
      this.error = 'La categoría es obligatoria';
      this.cdr.detectChanges();
      return;
    }

    if (!this.form.concepto || !this.form.concepto.trim()) {
      this.error = 'El concepto es obligatorio';
      this.cdr.detectChanges();
      return;
    }

    if (!this.form.monto || this.form.monto <= 0) {
      this.error = 'El monto debe ser mayor a 0';
      this.cdr.detectChanges();
      return;
    }

    if (!this.form.medioPago || !this.form.medioPago.trim()) {
      this.error = 'El medio de pago es obligatorio';
      this.cdr.detectChanges();
      return;
    }

    this.guardando = true;
    this.cdr.detectChanges();

    this.gastosService
      .crear({
        fecha: this.form.fecha,
        categoria: this.form.categoria,
        concepto: this.form.concepto.trim(),
        descripcion: this.form.descripcion?.trim() || '',
        monto: this.form.monto,
        medioPago: this.form.medioPago.trim(),
      })
      .pipe(
        finalize(() => {
          this.guardando = false;
          this.cdr.detectChanges();
        }),
      )
      .subscribe({
        next: (res) => {
          if (res.status !== 200 && res.status !== 201) {
            this.error = res.mensaje || 'No se pudo guardar el gasto';
            this.cdr.detectChanges();
            return;
          }

          this.ok = 'Gasto registrado correctamente';
          this.form = {
            fecha: new Date().toISOString().slice(0, 10),
            categoria: '',
            concepto: '',
            descripcion: '',
            monto: null,
            medioPago: '',
          };

          this.cargarDashboard();
          this.cdr.detectChanges();
        },
        error: () => {
          this.error = 'Error al registrar el gasto';
          this.cdr.detectChanges();
        },
      });
  }

  abrirEditar(g: Gasto): void {
    this.error = null;
    this.ok = null;
    this.editandoId = g.id ?? null;
    this.mostrarModalEditar = true;

    this.editForm = {
      fecha: g.fecha || '',
      categoria: g.categoria || '',
      concepto: g.concepto || '',
      descripcion: g.descripcion || '',
      monto: g.monto ?? null,
      medioPago: g.medioPago || '',
    };

    this.cdr.detectChanges();
  }

  cerrarEditar(): void {
    this.mostrarModalEditar = false;
    this.editandoId = null;
    this.editForm = {
      fecha: '',
      categoria: '',
      concepto: '',
      descripcion: '',
      monto: null,
      medioPago: '',
    };
    this.cdr.detectChanges();
  }

  guardarEdicion(): void {
    this.error = null;
    this.ok = null;

    if (!this.editandoId) {
      this.error = 'No se encontró el gasto a editar';
      this.cdr.detectChanges();
      return;
    }

    if (!this.editForm.fecha) {
      this.error = 'La fecha es obligatoria';
      this.cdr.detectChanges();
      return;
    }

    if (!this.editForm.categoria) {
      this.error = 'La categoría es obligatoria';
      this.cdr.detectChanges();
      return;
    }

    if (!this.editForm.concepto || !this.editForm.concepto.trim()) {
      this.error = 'El concepto es obligatorio';
      this.cdr.detectChanges();
      return;
    }

    if (!this.editForm.monto || this.editForm.monto <= 0) {
      this.error = 'El monto debe ser mayor a 0';
      this.cdr.detectChanges();
      return;
    }

    if (!this.editForm.medioPago || !this.editForm.medioPago.trim()) {
      this.error = 'El medio de pago es obligatorio';
      this.cdr.detectChanges();
      return;
    }

    this.guardandoEdicion = true;
    this.cdr.detectChanges();

    this.gastosService
      .actualizar(this.editandoId, {
        fecha: this.editForm.fecha,
        categoria: this.editForm.categoria,
        concepto: this.editForm.concepto.trim(),
        descripcion: this.editForm.descripcion?.trim() || '',
        monto: this.editForm.monto,
        medioPago: this.editForm.medioPago.trim(),
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
            this.error = res.mensaje || 'No se pudo actualizar el gasto';
            this.cdr.detectChanges();
            return;
          }

          this.ok = 'Gasto actualizado correctamente';
          this.cerrarEditar();
          this.cargarDashboard();
          this.cdr.detectChanges();
        },
        error: () => {
          this.error = 'Error al actualizar el gasto';
          this.cdr.detectChanges();
        },
      });
  }

  limpiarFiltros(): void {
    this.filtros = {
      categoria: '',
      concepto: '',
      fechaDesde: '',
      fechaHasta: '',
      q: '',
    };
    this.paginaActual = 1;
    this.cargarDashboard();
  }

  exportarGastos(): void {
    if (!this.gastos || this.gastos.length === 0) {
      return;
    }

    const categoria = this.filtros.categoria ? this.filtros.categoria.toLowerCase() : 'todas';

    const concepto = this.filtros.concepto?.trim()
      ? this.filtros.concepto.trim().replace(/\s+/g, '_')
      : 'todos';

    const fechaDesde = this.filtros.fechaDesde || 'sin_desde';
    const fechaHasta = this.filtros.fechaHasta || 'sin_hasta';
    const busqueda = this.filtros.q?.trim()
      ? this.filtros.q.trim().replace(/\s+/g, '_')
      : 'sin_busqueda';

    const data = this.gastos.map((g, index) => ({
      Nro: index + 1,
      Fecha: g.fecha,
      Categoría: this.formatCategoria(g.categoria),
      Concepto: g.concepto || '-',
      Descripción: g.descripcion || '-',
      'Medio de pago': this.formatMedio(g.medioPago),
      Monto: Number(g.monto ?? 0),
    }));

    const fileName = `gastos_${categoria}_${concepto}_${fechaDesde}_${fechaHasta}_${busqueda}`;

    this.excelExportService.exportToExcel(data, fileName, 'Gastos');
  }

  ordenarPor(columna: 'fecha' | 'categoria' | 'concepto' | 'medioPago' | 'monto'): void {
    if (this.sortColumn === columna) {
      this.sortDirection = this.sortDirection === 'asc' ? 'desc' : 'asc';
    } else {
      this.sortColumn = columna;
      this.sortDirection = columna === 'fecha' || columna === 'monto' ? 'desc' : 'asc';
    }

    this.ordenarGastos();
    this.paginaActual = 1;
    this.cdr.detectChanges();
  }

  ordenarGastos(): void {
    this.gastosFiltrados = [...this.gastosFiltrados].sort((a, b) => {
      let valorA: any;
      let valorB: any;

      switch (this.sortColumn) {
        case 'fecha':
          valorA = this.normalizar(a.fecha);
          valorB = this.normalizar(b.fecha);
          break;
        case 'categoria':
          valorA = this.normalizar(a.categoria);
          valorB = this.normalizar(b.categoria);
          break;
        case 'concepto':
          valorA = this.normalizar(a.concepto);
          valorB = this.normalizar(b.concepto);
          break;
        case 'medioPago':
          valorA = this.normalizar(a.medioPago);
          valorB = this.normalizar(b.medioPago);
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

  esColumnaActiva(columna: 'fecha' | 'categoria' | 'concepto' | 'medioPago' | 'monto'): boolean {
    return this.sortColumn === columna;
  }

  iconoOrden(columna: 'fecha' | 'categoria' | 'concepto' | 'medioPago' | 'monto'): string {
    if (this.sortColumn !== columna) {
      return 'bi-arrow-down-up';
    }

    return this.sortDirection === 'asc' ? 'bi-sort-down-alt' : 'bi-sort-down';
  }

  get totalPaginas(): number {
    return Math.max(1, Math.ceil(this.gastosFiltrados.length / this.tamanoPagina));
  }

  get gastosPaginados(): Gasto[] {
    const inicio = (this.paginaActual - 1) * this.tamanoPagina;
    const fin = inicio + this.tamanoPagina;
    return this.gastosFiltrados.slice(inicio, fin);
  }

  get inicioRegistro(): number {
    if (this.gastosFiltrados.length === 0) return 0;
    return (this.paginaActual - 1) * this.tamanoPagina + 1;
  }

  get finRegistro(): number {
    return Math.min(this.paginaActual * this.tamanoPagina, this.gastosFiltrados.length);
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

  private renderChartCategorias(): void {
    if (!this.chartCategoriasRef?.nativeElement) return;

    const acumulado = new Map<string, number>();

    for (const categoria of this.categorias) {
      acumulado.set(categoria, 0);
    }

    for (const gasto of this.gastos) {
      const actual = acumulado.get(gasto.categoria) ?? 0;
      acumulado.set(gasto.categoria, actual + Number(gasto.monto));
    }

    const labels = Array.from(acumulado.keys()).map((c) => this.formatCategoria(c));
    const data = Array.from(acumulado.values());

    if (this.chartCategorias) {
      this.chartCategorias.destroy();
    }

    this.chartCategorias = new Chart(this.chartCategoriasRef.nativeElement, {
      type: 'bar',
      data: {
        labels,
        datasets: [
          {
            label: 'Monto por categoría',
            data,
            backgroundColor: [
              '#0d6efd',
              '#dc3545',
              '#198754',
              '#ffc107',
              '#6f42c1',
              '#fd7e14',
              '#20c997',
              '#6c757d',
            ],
            borderWidth: 1,
          },
        ],
      },
      options: {
        responsive: true,
        maintainAspectRatio: false,
        plugins: {
          legend: { display: false },
        },
      },
    });
  }

  formatCategoria(categoria: string): string {
    return categoria
      .replaceAll('_', ' ')
      .toLowerCase()
      .replace(/\b\w/g, (l) => l.toUpperCase());
  }

  formatMedio(medio: string | null | undefined): string {
    if (!medio) return '-';
    return medio
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