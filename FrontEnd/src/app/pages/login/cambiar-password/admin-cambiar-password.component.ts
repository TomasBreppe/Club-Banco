import { Component, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { finalize } from 'rxjs/operators';
import { PerfilService } from '../../../service/perfil.service';

@Component({
  selector: 'app-admin-cambiar-password',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './admin-cambiar-password.component.html'
})
export class AdminCambiarPasswordComponent {
  guardando = false;
  error: string | null = null;
  mensaje: string | null = null;

  form = {
    currentPassword: '',
    newPassword: '',
    repetir: ''
  };

  constructor(
    private perfilService: PerfilService,
    private cdr: ChangeDetectorRef
  ) {}

  guardar(): void {
    this.error = null;
    this.mensaje = null;

    const actual = this.form.currentPassword.trim();
    const nueva = this.form.newPassword.trim();
    const repetir = this.form.repetir.trim();

    if (!actual || !nueva || !repetir) {
      this.error = 'Completá todos los campos.';
      this.cdr.detectChanges();
      return;
    }

    if (nueva !== repetir) {
      this.error = 'La nueva contraseña y la repetición no coinciden.';
      this.cdr.detectChanges();
      return;
    }

    this.guardando = true;
    this.cdr.detectChanges();

    this.perfilService.cambiarPassword({
      currentPassword: actual,
      newPassword: nueva
    })
    .pipe(
      finalize(() => {
        this.guardando = false;
        this.cdr.detectChanges();
      })
    )
    .subscribe({
      next: (res) => {
        this.mensaje = res?.mensaje || 'Contraseña actualizada correctamente.';
        this.form = {
          currentPassword: '',
          newPassword: '',
          repetir: ''
        };
        this.cdr.detectChanges();
      },
      error: (err) => {
        this.error = err?.error?.mensaje || 'Error actualizando contraseña.';
        this.cdr.detectChanges();
      }
    });
  }
}