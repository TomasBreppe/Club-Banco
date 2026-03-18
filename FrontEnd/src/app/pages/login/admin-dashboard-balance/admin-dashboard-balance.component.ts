import {
  AfterViewInit,
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

import { DashboardBalanceService } from '../../../service/dashboard-balance.service';
import { DashboardBalanceResumen } from '../../../models/balance.model';

Chart.register(BarController, BarElement, CategoryScale, LinearScale, Tooltip, Legend);

@Component({
  selector: 'app-admin-dashboard-balance',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterModule],
  templateUrl: './admin-dashboard-balance.component.html',
  styleUrls: ['./admin-dashboard-balance.component.css'],
  providers: [DatePipe],
})
export class AdminDashboardBalanceComponent implements OnInit, AfterViewInit {
  private dashboardBalanceService = inject(DashboardBalanceService);
  private cdr = inject(ChangeDetectorRef);
  private datePipe = inject(DatePipe);

  @ViewChild('chartBalance') chartBalanceRef?: ElementRef<HTMLCanvasElement>;

  private chartBalance?: Chart;

  cargando = false;
  error: string | null = null;
  mesActual = '';

  resumen: DashboardBalanceResumen | null = null;

  fechaDesde = '';
  fechaHasta = '';

  ngOnInit(): void {
    this.actualizarPeriodoVisual();
    this.cargarDashboard();
  }

  ngAfterViewInit(): void {}

  cargarDashboard(): void {
    this.error = null;
    this.cargando = true;

    this.dashboardBalanceService
      .obtenerDashboard(
        this.fechaDesde || null,
        this.fechaHasta || null
      )
      .pipe(
        finalize(() => {
          this.cargando = false;
          this.cdr.detectChanges();
        }),
      )
      .subscribe({
        next: (res) => {
          if (res.status !== 200) {
            this.error = res.mensaje || 'No se pudo cargar el dashboard de balance';
            return;
          }

          this.resumen = res.data.resumen;
          this.actualizarPeriodoVisual();

          setTimeout(() => {
            this.renderChart();
          }, 0);
        },
        error: () => {
          this.error = 'Error al cargar el dashboard de balance';
        },
      });
  }

  filtrar(): void {
    if (this.fechaDesde && this.fechaHasta && this.fechaDesde > this.fechaHasta) {
      this.error = 'La fecha desde no puede ser mayor que la fecha hasta';
      return;
    }

    this.cargarDashboard();
  }

  limpiarFiltros(): void {
    this.fechaDesde = '';
    this.fechaHasta = '';
    this.cargarDashboard();
  }

  private actualizarPeriodoVisual(): void {
    if (this.fechaDesde || this.fechaHasta) {
      const desde = this.fechaDesde
        ? this.formatearFecha(this.fechaDesde)
        : 'Inicio';
      const hasta = this.fechaHasta
        ? this.formatearFecha(this.fechaHasta)
        : 'Hoy';

      this.mesActual = `${desde} - ${hasta}`;
      return;
    }

    this.mesActual =
      this.datePipe.transform(new Date(), "MMMM 'de' yyyy", 'es-AR') ?? '';
  }

  private formatearFecha(fecha: string): string {
    return this.datePipe.transform(fecha, 'dd/MM/yyyy', 'es-AR') ?? fecha;
  }

  private renderChart(): void {
    if (!this.chartBalanceRef?.nativeElement || !this.resumen) return;

    if (this.chartBalance) {
      this.chartBalance.destroy();
    }

    this.chartBalance = new Chart(this.chartBalanceRef.nativeElement, {
      type: 'bar',
      data: {
        labels: ['Ingresos', 'Gastos', 'Neto'],
        datasets: [
          {
            label: 'Monto',
            data: [
              this.resumen.ingresosMes,
              this.resumen.gastosMes,
              this.resumen.netoMes,
            ],
            backgroundColor: ['#198754', '#dc3545', '#0d6efd'],
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

  getNetoClass(): string {
    if (!this.resumen) return 'text-dark';
    return this.resumen.netoMes >= 0 ? 'text-success' : 'text-danger';
  }
}