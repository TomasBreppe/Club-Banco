import { Component, Input } from '@angular/core';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-admin-dashboard-placeholder',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="container py-4">
      <h2 class="mb-2">{{ titulo }}</h2>
      <p class="text-muted mb-0">
        Este dashboard todavía no está implementado.
      </p>
    </div>
  `
})
export class AdminDashboardPlaceholderComponent {
  @Input() titulo = 'Dashboard';
}