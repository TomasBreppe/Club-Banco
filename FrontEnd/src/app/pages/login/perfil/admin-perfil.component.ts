import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { PerfilService } from '../../../service/perfil.service';
import { finalize } from 'rxjs/operators';

@Component({
  selector: 'app-admin-perfil',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './admin-perfil.component.html',
})
export class AdminPerfilComponent implements OnInit {
  guardando = false;
  mensaje: string | null = null;
  error: string | null = null;

  form = {
    nombre: '',
    apellido: '',
    email: '',
    telefono: '',
  };

  constructor(
    private perfilService: PerfilService,
    private cdr: ChangeDetectorRef,
  ) {}

  ngOnInit(): void {
    this.perfilService.obtenerPerfil().subscribe({
      next: (res) => {
        this.perfilService.mergePerfil({
          nombre: this.form.nombre,
          apellido: this.form.apellido,
          email: this.form.email,
          telefono: this.form.telefono,
        });

        this.mensaje = res?.mensaje || 'Perfil actualizado correctamente';
        this.cdr.detectChanges();
      },
      error: (err) => {
        this.error = err?.error?.mensaje || 'Error cargando perfil';
        this.cdr.detectChanges();
      },
    });
  }

  guardar(): void {
    this.guardando = true;
    this.mensaje = null;
    this.error = null;
    this.cdr.detectChanges();

    this.perfilService
      .actualizarPerfil(this.form)
      .pipe(
        finalize(() => {
          this.guardando = false;
          this.cdr.detectChanges();
        }),
      )
      .subscribe({
        next: (res) => {
          this.perfilService.mergePerfil({
            nombre: this.form.nombre,
            apellido: this.form.apellido,
            email: this.form.email,
            telefono: this.form.telefono,
          });

          this.mensaje = res?.mensaje || 'Perfil actualizado correctamente';
          this.cdr.detectChanges();
        },

        error: (err) => {
          this.error = err?.error?.mensaje || 'Error actualizando perfil';
          this.cdr.detectChanges();
        },
      });
  }
}
