import { Routes } from '@angular/router';
import { LoginComponent } from './pages/login/login.component';
import { AdminDisciplinasComponent } from './pages/login/disciplinas/admin-disciplinas.component';
import { AdminSociosComponent } from './pages/login/socios/admin-socios.component';
import { AdminSocioDetalleComponent } from './pages/login/admin-socio-detalle/admin-socio-detalle.component';
import { authGuard } from './core/auth/auth.guard';
import { AdminLayoutComponent } from './layouts/admin-layout.component';
import { AdminHomeComponent } from './pages/login/home/admin-home.component';
import { adminSociosResolver } from './core/auth/resolvers/admin-socios.resolver';
import { adminDisciplinasResolver } from './core/auth/resolvers/admin-disciplinas.resolver';
import { AdminPerfilComponent } from './pages/login/perfil/admin-perfil.component';
import { AdminCambiarPasswordComponent } from './pages/login/cambiar-password/admin-cambiar-password.component';
import { AdminFotoPerfilComponent } from './pages/login/foto-perfil/admin-foto-perfil.component';
import { AdminDashboardSociosComponent } from './pages/login/admin-dashboard-socios/admin-dashboard-socios.component';
import { AdminDashboardHomeComponent } from './pages/login/admin-dashboard-home/admin-dashboard-home.component';
import { AdminDashboardGastosComponent } from './pages/login/admin-dashboard-gastos/admin-dashboard-gastos.component';
import { AdminDashboardIngresosComponent } from './pages/login/admin-dashboard-ingresos/admin-dashboard-ingresos.component';
import { AdminDashboardBalanceComponent } from './pages/login/admin-dashboard-balance/admin-dashboard-balance.component';

export const routes: Routes = [
  { path: 'login', component: LoginComponent },

  {
    path: 'admin',
    component: AdminLayoutComponent,
    canActivate: [authGuard],
    children: [
      { path: 'home', component: AdminHomeComponent },

      {
        path: 'socios',
        component: AdminSociosComponent,
        resolve: { prefetched: adminSociosResolver },
      },
      { path: 'socios/:id', component: AdminSocioDetalleComponent },

      {
        path: 'disciplinas',
        component: AdminDisciplinasComponent,
        resolve: { prefetched: adminDisciplinasResolver },
      },

      { path: 'perfil', component: AdminPerfilComponent },
      { path: 'cambiar-password', component: AdminCambiarPasswordComponent },
      { path: 'foto-perfil', component: AdminFotoPerfilComponent },

      { path: 'dashboard', component: AdminDashboardHomeComponent },
      { path: 'dashboard/socios', component: AdminDashboardSociosComponent },
      { path: 'dashboard/gastos', component: AdminDashboardGastosComponent },
      { path: 'dashboard/ingresos', component: AdminDashboardIngresosComponent },
      { path: 'dashboard/balance', component: AdminDashboardBalanceComponent },

      { path: '', pathMatch: 'full', redirectTo: 'home' },
    ],
  },

  { path: '', pathMatch: 'full', redirectTo: 'login' },
  { path: '**', redirectTo: 'login' },
];
