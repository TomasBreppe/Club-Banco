import { Component, HostListener, OnInit, OnDestroy, ChangeDetectorRef } from '@angular/core';
import { Router, RouterLink, RouterLinkActive, RouterOutlet, NavigationEnd } from '@angular/router';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Observable, of, filter, Subscription } from 'rxjs';
import { PerfilService, MiPerfil } from '../service/perfil.service';
import { AuthService } from '../core/auth/auth.service';
import { AdminStatsService, AdminStats } from '../core/auth/stats/admin-stats.service';

@Component({
  standalone: true,
  selector: 'app-admin-layout',
  imports: [CommonModule, FormsModule, RouterOutlet, RouterLink, RouterLinkActive],
  templateUrl: './admin-layout.component.html',
  styleUrls: ['./admin-layout.component.css'],
})
export class AdminLayoutComponent implements OnInit, OnDestroy {
  stats$: Observable<AdminStats | null> = of(null);

  perfil: MiPerfil | null = null;
  searchText = '';
  userMenuOpen = false;

  private perfilSub?: Subscription;
  private routerSub?: Subscription;

  constructor(
    public auth: AuthService,
    private router: Router,
    private statsSvc: AdminStatsService,
    private perfilService: PerfilService,
    private cdr: ChangeDetectorRef,
  ) {}

  ngOnInit(): void {
    this.stats$ = this.statsSvc.stats$;
    this.statsSvc.loadOnce();

    this.perfilSub = this.perfilService.perfil$.subscribe((perfil) => {
      this.perfil = perfil;
      this.cdr.detectChanges();
    });

    this.perfilService.obtenerPerfil().subscribe();

    this.routerSub = this.router.events
      .pipe(filter((event) => event instanceof NavigationEnd))
      .subscribe(() => {
        const tree = this.router.parseUrl(this.router.url);
        this.searchText = tree.queryParams['q'] || '';
      });
  }

  ngOnDestroy(): void {
    this.perfilSub?.unsubscribe();
    this.routerSub?.unsubscribe();
  }

  toggleUserMenu(): void {
    this.userMenuOpen = !this.userMenuOpen;
  }

  buscarSocio(): void {
    const texto = this.searchText.trim();
    this.userMenuOpen = false;

    this.router.navigate(['/admin/socios'], {
      queryParams: texto ? { q: texto } : {},
    });
  }

  editarPerfil(): void {
    this.userMenuOpen = false;
    this.router.navigateByUrl('/admin/perfil');
  }

  cambiarPassword(): void {
    this.userMenuOpen = false;
    this.router.navigateByUrl('/admin/cambiar-password');
  }

  actualizarFoto(): void {
    this.userMenuOpen = false;
    this.router.navigateByUrl('/admin/foto-perfil');
  }

  logout(): void {
    this.auth.clear();
    this.router.navigateByUrl('/login');
  }

  @HostListener('document:click', ['$event'])
  onDocumentClick(event: MouseEvent): void {
    const target = event.target as HTMLElement;
    const clickedInsideMenu = target.closest('.user-dropdown');

    if (!clickedInsideMenu) {
      this.userMenuOpen = false;
    }
  }
}
