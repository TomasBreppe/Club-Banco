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
import {
  DashboardIngresosResumen,
  IngresoDashboardItem,
} from '../../../models/ingresos.model';
import { IngresosManualesService } from '../../../service/ingresos-manuales.service';
import { IngresoManualCreateRequest } from '../../../models/ingreso-manual.model';

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
  private cdr = inject(ChangeDetectorRef);
  private datePipe = inject(DatePipe);

  @ViewChild('chartMedios') chartMediosRef?: ElementRef<HTMLCanvasElement>;

  private chartMedios?: Chart;
  private chartsPendientes = false;

  cargando = false;
  guardandoManual = false;
  error: string | null = null;
  ok: string | null = null;

  resumen: DashboardIngresosResumen | null = null;
  ingresos: IngresoDashboardItem[] = [];
  mesActual = '';

  medios: string[] = ['EFECTIVO', 'TRANSFERENCIA', 'BANCO', 'MERCADO_PAGO', 'TARJETA', 'OTRO'];

  filtros = {
    medio: '',
    fechaDesde: '',
    fechaHasta: '',
    q: '',
  };

  categoriasIngresoManual: string[] = [
    'MANTOVANI',
    'DIEF',
    'MUTUAL',
    'EVENTOS',
    'FUTBOL_TORNEOS',
    'FUTBOL_ALQUILERES',
    'PLAYAS'
  ];

  mediosIngresoManual: string[] = ['EFECTIVO', 'BANCO'];

  formIngresoManual: IngresoManualCreateRequest = {
    fecha: new Date().toISOString().slice(0, 10),
    categoria: '',
    medioPago: '',
    monto: null,
    descripcion: '',
  };

  ngOnInit(): void {
    this.mesActual = this.datePipe.transform(new Date(), "MMMM 'de' yyyy", 'es-AR') ?? '';
    this.cargarDashboard();
  }

  ngAfterViewChecked(): void {
    if (this.chartsPendientes) {
      this.chartsPendientes = false;
      this.renderChartMedios();
    }
  }

  cargarDashboard(): void {
    this.error = null;
    this.cargando = true;
    this.cdr.detectChanges();

    this.dashboardIngresosService
      .obtenerDashboard({
        medio: this.filtros.medio || null,
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

  private renderChartMedios(): void {
    if (!this.chartMediosRef?.nativeElement) return;

    const acumulado = new Map<string, number>();

    for (const medio of this.medios) {
      acumulado.set(medio, 0);
    }

    for (const ingreso of this.ingresos) {
      const key = ingreso.medio || 'OTRO';
      const actual = acumulado.get(key) ?? 0;
      acumulado.set(key, actual + Number(ingreso.monto));
    }

    const labels = Array.from(acumulado.keys()).map((m) => this.formatMedio(m));
    const data = Array.from(acumulado.values());

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
            backgroundColor: ['#198754', '#0d6efd', '#6610f2', '#ffc107', '#dc3545', '#6c757d'],
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