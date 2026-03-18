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

import { DashboardService } from '../../../service/dashboard.service';
import { DisciplinasService } from '../../../service/disciplinas.service';
import { DashboardSociosResumen, Socio } from '../../../models/dashboard.model';
import { DisciplinaDto } from '../../../features/disciplinas.models';

Chart.register(DoughnutController, ArcElement, Tooltip, Legend);

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
    estadoPago: '',
    q: '',
  };

  ngOnInit(): void {
    this.mesActual =
      this.datePipe.transform(new Date(), "MMMM 'de' yyyy", 'es-AR') ?? '';
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
          this.socios = res.data.socios;
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
      estadoPago: '',
      q: '',
    };
    this.cargarDashboard();
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
}