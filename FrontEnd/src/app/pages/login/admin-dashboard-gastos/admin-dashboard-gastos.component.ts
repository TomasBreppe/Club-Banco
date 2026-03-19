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
  private cdr = inject(ChangeDetectorRef);
  private datePipe = inject(DatePipe);

  @ViewChild('chartCategorias') chartCategoriasRef?: ElementRef<HTMLCanvasElement>;

  private chartCategorias?: Chart;
  private chartsPendientes = false;

  loading = false;
  cargando = false;
  guardando = false;
  error: string | null = null;
  ok: string | null = null;

  resumen: DashboardGastosResumen | null = null;
  gastos: Gasto[] = [];
  mesActual = '';

  categorias: GastoCategoria[] = [
    'IMPUESTOS',
    'DISCIPLINAS',
    'MANTENIMIENTO',
    'LIMPIEZA',
    'EVENTOS',
    'HONORARIOS',
    'OTROS',
  ];

  filtros = {
    categoria: '',
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

  ngOnInit(): void {
    this.mesActual = this.datePipe.transform(new Date(), "MMMM 'de' yyyy", 'es-AR') ?? '';
    this.cargarDashboard();
  }

  ngAfterViewChecked(): void {
    if (this.chartsPendientes) {
      this.chartsPendientes = false;
      this.renderChartCategorias();
    }
  }

  cargarDashboard(): void {
    this.error = null;
    this.cargando = true;
    this.cdr.detectChanges();

    this.gastosService
      .dashboard({
        categoria: this.filtros.categoria || null,
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
          this.gastos = res.data.gastos;
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

  limpiarFiltros(): void {
    this.filtros = {
      categoria: '',
      fechaDesde: '',
      fechaHasta: '',
      q: '',
    };
    this.cargarDashboard();
  }

  exportarGastos(): void {
    if (!this.gastos || this.gastos.length === 0) {
      return;
    }

    const categoria = this.filtros.categoria
      ? this.filtros.categoria.toLowerCase()
      : 'todas';

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
      'Medio de pago': g.medioPago || '-',
      Monto: Number(g.monto ?? 0),
    }));

    const fileName = `gastos_${categoria}_${fechaDesde}_${fechaHasta}_${busqueda}`;

    this.excelExportService.exportToExcel(data, fileName, 'Gastos');
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
}