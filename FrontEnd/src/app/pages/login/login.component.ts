import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { AuthApiService } from '../../core/auth/api.service';
import { AuthService } from '../../core/auth/auth.service';

@Component({
  standalone: true,
  selector: 'app-login',
  imports: [CommonModule, FormsModule],
  templateUrl: './login.component.html'
})
export class LoginComponent {
  email = '';
  password = '';
  loading = false;
  error: string | null = null;

  constructor(
    private api: AuthApiService,
    private auth: AuthService,
    private router: Router
  ) {}

  submit() {
    this.error = null;
    this.loading = true;

    this.api.login({ email: this.email, password: this.password }).subscribe({
      next: (res) => {
        if (res.status !== 200 || !res.data?.token) {
          this.error = res.mensaje || 'Login inválido';
          this.loading = false;
          return;
        }

        this.auth.setToken(res.data.token);
        this.loading = false;
        this.router.navigateByUrl('/admin/home');
      },
      error: (err) => {
        this.error = err?.error?.mensaje || 'Error de conexión';
        this.loading = false;
      }
    });
  }
}
