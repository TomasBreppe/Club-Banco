import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule, NgForm } from '@angular/forms';
import { RouterLink, ActivatedRoute } from '@angular/router';
import { forkJoin } from 'rxjs';
import { finalize } from 'rxjs/operators';

import { SociosService } from '../../../service/socio.service';
import { SocioCreateRequest, SocioDto } from '../../../features/socios/socio.models';
import { DisciplinasService } from '../../../service/disciplinas.service';
import { DisciplinaDto, ArancelDisciplinaDto } from '../../../features/disciplinas.models';
import { AdminStatsService } from '../../../core/auth/stats/admin-stats.service';

@Component({
  standalone: true,
  selector: 'app-admin-socios',
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './admin-socios.component.html',
})
export class AdminSociosComponent implements OnInit {
  socios: SocioDto[] = [];
  sociosFiltrados: SocioDto[] = [];
  disciplinas: DisciplinaDto[] = [];
  arancelesDisponibles: ArancelDisciplinaDto[] = [];
  socioPendiente: SocioDto | null = null;
  nuevoEstadoPendiente: boolean | null = null;
  loadingInit = false;
  loadingList = false;
  creating = false;

  error: string | null = null;
  textoBusqueda = '';

  form: SocioCreateRequest = {
    dni: '',
    nombre: '',
    apellido: '',
    genero: '',
    telefono: '',
    celular: '',
    disciplinaId: 0,
    arancelDisciplinaId: 0,
    inscripcionPagada: false,
  };

  constructor(
    private sociosApi: SociosService,
    private disciplinasApi: DisciplinasService,
    private route: ActivatedRoute,
    private statsSvc: AdminStatsService,
    private cdr: ChangeDetectorRef,
  ) {}

  ngOnInit(): void {
    const prefetched = this.route.snapshot.data['prefetched'];

    const disciplinas = prefetched?.disciplinas;
    const socios = prefetched?.socios;

    this.disciplinas = (disciplinas?.data ?? []).filter((d: any) => d.activa);
    this.socios = socios?.data ?? [];
    this.sociosFiltrados = [...this.socios];

    this.route.queryParamMap.subscribe((params) => {
      this.textoBusqueda = (params.get('q') ?? '').trim();
      this.aplicarFiltro();
    });
  }

  refrescar() {
    this.cargarTodo();
  }

  cargarTodo() {
    this.error = null;
    this.loadingInit = true;

    forkJoin({
      disciplinas: this.disciplinasApi.listar(),
      socios: this.sociosApi.listar(),
    })
      .pipe(finalize(() => (this.loadingInit = false)))
      .subscribe({
        next: ({ disciplinas, socios }) => {
          this.disciplinas = (disciplinas.data ?? []).filter((d) => d.activa);

          if (this.form.disciplinaId === 0 && this.disciplinas.length > 0) {
            this.form.disciplinaId = this.disciplinas[0].id;
          }

          this.socios = socios.data ?? [];
          this.aplicarFiltro();
        },
        error: (err) => {
          this.error =
            err?.error?.mensaje ||
            err?.message ||
            `Error refrescando (${err?.status || 'sin status'})`;
        },
      });
  }

  onDisciplinaChange(): void {
    const disciplinaId = Number(this.form.disciplinaId || 0);

    this.arancelesDisponibles = [];
    this.form.arancelDisciplinaId = 0;

    if (!disciplinaId) {
      this.cdr.detectChanges();
      return;
    }

    this.disciplinasApi.getArancelesPorDisciplina(disciplinaId).subscribe({
      next: (res) => {
        this.arancelesDisponibles = (res?.data ?? []).filter((a) => a.activa);

        if (this.arancelesDisponibles.length === 1) {
          this.form.arancelDisciplinaId = this.arancelesDisponibles[0].id;
        }

        this.cdr.detectChanges();
      },
      error: () => {
        this.arancelesDisponibles = [];
        this.form.arancelDisciplinaId = 0;
        this.cdr.detectChanges();
      },
    });
  }
  cargarSocios() {
    this.error = null;
    this.loadingList = true;
    this.cdr.detectChanges();

    this.sociosApi
      .listar()
      .pipe(
        finalize(() => {
          this.loadingList = false;
          this.cdr.detectChanges();
        }),
      )
      .subscribe({
        next: (res) => {
          this.socios = res.data ?? [];
          this.aplicarFiltro();
          this.cdr.detectChanges();
        },
        error: (err) => {
          this.error =
            err?.error?.mensaje ||
            err?.message ||
            `Error cargando socios (${err?.status || 'sin status'})`;
          this.cdr.detectChanges();
        },
      });
  }

  crearSocio(f: NgForm) {
    this.error = null;

    if (f.invalid) return;

    if (!this.form.arancelDisciplinaId || this.form.arancelDisciplinaId <= 0) {
      this.error = 'Debés seleccionar una categoría/arancel';
      this.cdr.detectChanges();
      return;
    }

    this.creating = true;
    this.cdr.detectChanges();

    const body = {
      ...this.form,
      dni: this.form.dni.trim(),
      nombre: this.form.nombre.trim(),
      apellido: this.form.apellido.trim(),
      celular: this.form.celular.trim(),
      telefono: (this.form.telefono ?? '').trim(),
    };

    this.sociosApi
      .crear(body)
      .pipe(
        finalize(() => {
          this.creating = false;
          this.cdr.detectChanges();
        }),
      )
      .subscribe({
        next: (res) => {
          if (res.status !== 200 && res.status !== 201) {
            this.error = res.mensaje || 'No se pudo crear el socio';
            this.cdr.detectChanges();
            return;
          }

          this.statsSvc.refresh();

          const genero = this.form.genero;
          const disciplinaId = this.form.disciplinaId;

          this.form = {
            dni: '',
            nombre: '',
            apellido: '',
            genero,
            telefono: '',
            celular: '',
            disciplinaId,
            arancelDisciplinaId: 0,
          };

          f.resetForm(this.form);

          this.cargarSocios();
          this.onDisciplinaChange();
          this.cdr.detectChanges();
        },
        error: (err) => {
          this.error =
            err?.error?.mensaje ||
            err?.message ||
            `Error creando socio (${err?.status || 'sin status'})`;
          this.cdr.detectChanges();
        },
      });
  }

  aplicarFiltro(): void {
    const texto = this.normalizar(this.textoBusqueda);

    if (!texto) {
      this.sociosFiltrados = [...this.socios];
      return;
    }

    this.sociosFiltrados = this.socios.filter((s) => {
      const dni = this.normalizar(s.dni);
      const nombre = this.normalizar(s.nombre);
      const apellido = this.normalizar(s.apellido);
      const disciplina = this.normalizar(
        s.disciplinaNombre || this.disciplinaNombre(s.disciplinaId),
      );

      const apellidoNombre = this.normalizar(`${s.apellido}, ${s.nombre}`);
      const nombreApellido = this.normalizar(`${s.nombre} ${s.apellido}`);
      const apellidoNombreSinComa = this.normalizar(`${s.apellido} ${s.nombre}`);

      return (
        dni.includes(texto) ||
        nombre.includes(texto) ||
        apellido.includes(texto) ||
        disciplina.includes(texto) ||
        apellidoNombre.includes(texto) ||
        nombreApellido.includes(texto) ||
        apellidoNombreSinComa.includes(texto)
      );
    });
  }

  private normalizar(valor: any): string {
    return (valor ?? '')
      .toString()
      .toLowerCase()
      .normalize('NFD')
      .replace(/[\u0300-\u036f]/g, '');
  }

  disciplinaNombre(id: number): string {
    return this.disciplinas.find((d) => d.id === id)?.nombre ?? `#${id}`;
  }

  abrirModalCambioEstado(s: SocioDto) {
    this.error = null;
    this.socioPendiente = s;
    this.nuevoEstadoPendiente = !s.activo;
  }

  confirmarCambioEstado() {
    if (!this.socioPendiente || this.nuevoEstadoPendiente === null) return;

    const s = this.socioPendiente;
    const nuevoEstado = this.nuevoEstadoPendiente;

    this.sociosApi.cambiarActivo(s.id, nuevoEstado).subscribe({
      next: () => {
        // actualizar estado en tabla
        s.activo = nuevoEstado;

        // cerrar modal inmediatamente
        this.cerrarModalCambioEstado();

        // refrescar datos
        this.statsSvc.refresh();
        this.aplicarFiltro();
        this.cdr.detectChanges();
      },
      error: (err) => {
        this.error =
          err?.error?.mensaje ||
          err?.message ||
          `Error cambiando estado del socio (${err?.status || 'sin status'})`;

        // cerrar modal aunque haya error
        this.cerrarModalCambioEstado();
        this.cdr.detectChanges();
      },
    });
  }

  cerrarModalCambioEstado() {
    this.socioPendiente = null;
    this.nuevoEstadoPendiente = null;
  }
}
