import { ChangeDetectorRef, Component, OnInit, inject } from '@angular/core';
import { CommonModule, DatePipe } from '@angular/common';
import { RouterModule } from '@angular/router';
import { finalize } from 'rxjs/operators';
import { DashboardGeneralService } from '../../../service/dashboard-general.service';
import { DashboardGeneral } from '../../../models/dashboard-general.model';

@Component({
  selector: 'app-admin-dashboard-home',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './admin-dashboard-home.component.html',
  styleUrls: ['./admin-dashboard-home.component.css'],
  providers: [DatePipe],
})
export class AdminDashboardHomeComponent implements OnInit {
  private dashboardGeneralService = inject(DashboardGeneralService);
  private cdr = inject(ChangeDetectorRef);
  private datePipe = inject(DatePipe);

  cargando = false;
  error: string | null = null;
  data: DashboardGeneral | null = null;

  mesActual = '';

  ngOnInit(): void {
    this.mesActual =
      this.datePipe.transform(new Date(), "MMMM 'de' yyyy", 'es-AR') ?? '';
    this.cargarDashboardGeneral();
  }

  cargarDashboardGeneral(): void {
    this.error = null;
    this.cargando = true;
    this.cdr.detectChanges();

    this.dashboardGeneralService
      .obtenerDashboardGeneral()
      .pipe(
        finalize(() => {
          this.cargando = false;
          this.cdr.detectChanges();
        })
      )
      .subscribe({
        next: (res) => {
          if (res.status !== 200) {
            this.error = res.mensaje || 'No se pudo cargar el dashboard general';
            this.cdr.detectChanges();
            return;
          }

          this.data = res.data;
          this.cdr.detectChanges();
        },
        error: () => {
          this.error = 'Error al cargar el dashboard general';
          this.cdr.detectChanges();
        }
      });
  }

  getBalanceClass(): string {
    if (!this.data) return 'text-dark';
    return this.data.balanceMes >= 0 ? 'text-success' : 'text-danger';
  }
}