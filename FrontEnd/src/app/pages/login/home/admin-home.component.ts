import { Component, OnInit, OnDestroy, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { Subscription } from 'rxjs';

import { AdminStatsService, AdminStats } from '../../../core/auth/stats/admin-stats.service';

@Component({
  standalone: true,
  selector: 'app-admin-home',
  imports: [CommonModule, RouterLink],
  templateUrl: './admin-home.component.html',
  styleUrls: ['./admin-home.component.css'],
})
export class AdminHomeComponent implements OnInit, OnDestroy {
  stats: AdminStats = {
    socios: 0,
    disciplinasActivas: 0,
    deudores: 0,
  };

  statsError: string | null = null;
  private sub?: Subscription;

  constructor(
    private statsSvc: AdminStatsService,
    private cdr: ChangeDetectorRef,
  ) {}

  ngOnInit(): void {
    this.sub = this.statsSvc.stats$.subscribe((stats) => {
      if (stats) {
        this.stats = {
          socios: stats.socios ?? 0,
          disciplinasActivas: stats.disciplinasActivas ?? 0,
          deudores: stats.deudores ?? 0,
        };

        this.cdr.detectChanges();
      }
    });

    this.statsSvc.refresh();
  }

  ngOnDestroy(): void {
    this.sub?.unsubscribe();
  }
}
