import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { PerfilService } from '../../../service/perfil.service';
import { finalize } from 'rxjs/operators';

@Component({
  selector: 'app-admin-foto-perfil',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './admin-foto-perfil.component.html'
})
export class AdminFotoPerfilComponent implements OnInit {

  guardando = false;
  mensaje: string | null = null;
  error: string | null = null;

  fotoUrl = '';

  constructor(
    private perfilService: PerfilService,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    this.perfilService.obtenerPerfil().subscribe({
      next: (res) => {
        const p = res?.data;
        this.fotoUrl = p?.fotoUrl || '';
        this.cdr.detectChanges();
      },
      error: (err) => {
        this.error = err?.error?.mensaje || 'Error cargando foto de perfil';
        this.cdr.detectChanges();
      }
    });
  }

  guardar(): void {
    this.mensaje = null;
    this.error = null;

    const url = this.fotoUrl.trim();

    if (!url) {
      this.error = 'Ingresá una URL de imagen.';
      this.cdr.detectChanges();
      return;
    }

    if (!url.startsWith('http://') && !url.startsWith('https://')) {
      this.error = 'La URL debe comenzar con http:// o https://';
      this.cdr.detectChanges();
      return;
    }

    this.guardando = true;
    this.cdr.detectChanges();

    this.perfilService.actualizarFoto({ fotoUrl: url })
      .pipe(
        finalize(() => {
          this.guardando = false;
          this.cdr.detectChanges();
        })
      )
      .subscribe({
        next: (res) => {
          this.mensaje = res?.mensaje || 'Foto actualizada correctamente';
          this.cdr.detectChanges();
        },
        error: (err) => {
          this.error = err?.error?.mensaje || 'Error actualizando foto de perfil';
          this.cdr.detectChanges();
        }
      });
  }
}