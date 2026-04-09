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
  styleUrls: ['./admin-socios.component.css'],
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
  sortColumn: 'id' | 'dni' | 'nombre' | 'disciplina' | 'activo' | 'estadoPago' = 'id';
  sortDirection: 'asc' | 'desc' = 'asc';
  error: string | null = null;
  textoBusqueda = '';

  paginaActual = 1;
  tamanoPagina = 10;
  opcionesTamano = [5, 10, 20, 50];

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
            inscripcionPagada: false,
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
    } else {
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

    this.ordenarSocios();
    this.paginaActual = 1;
  }

  ordenarPor(columna: 'id' | 'dni' | 'nombre' | 'disciplina' | 'activo' | 'estadoPago'): void {
    if (this.sortColumn === columna) {
      this.sortDirection = this.sortDirection === 'asc' ? 'desc' : 'asc';
    } else {
      this.sortColumn = columna;
      this.sortDirection = 'asc';
    }

    this.ordenarSocios();
    this.paginaActual = 1;
    this.cdr.detectChanges();
  }

  ordenarSocios(): void {
    this.sociosFiltrados = [...this.sociosFiltrados].sort((a, b) => {
      let valorA: any;
      let valorB: any;

      switch (this.sortColumn) {
        case 'id':
          valorA = a.id ?? 0;
          valorB = b.id ?? 0;
          break;

        case 'dni':
          valorA = this.normalizar(a.dni);
          valorB = this.normalizar(b.dni);
          break;

        case 'nombre':
          valorA = this.normalizar(`${a.apellido}, ${a.nombre}`);
          valorB = this.normalizar(`${b.apellido}, ${b.nombre}`);
          break;

        case 'disciplina':
          valorA = this.normalizar(a.disciplinaNombre || this.disciplinaNombre(a.disciplinaId));
          valorB = this.normalizar(b.disciplinaNombre || this.disciplinaNombre(b.disciplinaId));
          break;

        case 'activo':
          valorA = a.activo ? 1 : 0;
          valorB = b.activo ? 1 : 0;
          break;

        case 'estadoPago':
          valorA = this.normalizar(a.estadoPago || '');
          valorB = this.normalizar(b.estadoPago || '');
          break;

        default:
          valorA = a.id ?? 0;
          valorB = b.id ?? 0;
      }

      if (valorA < valorB) {
        return this.sortDirection === 'asc' ? -1 : 1;
      }

      if (valorA > valorB) {
        return this.sortDirection === 'asc' ? 1 : -1;
      }

      return 0;
    });
  }

  esColumnaActiva(
    columna: 'id' | 'dni' | 'nombre' | 'disciplina' | 'activo' | 'estadoPago',
  ): boolean {
    return this.sortColumn === columna;
  }

  iconoOrden(columna: 'id' | 'dni' | 'nombre' | 'disciplina' | 'activo' | 'estadoPago'): string {
    if (this.sortColumn !== columna) {
      return 'bi-arrow-down-up';
    }

    return this.sortDirection === 'asc' ? 'bi-sort-down-alt' : 'bi-sort-down';
  }

  get totalPaginas(): number {
    return Math.max(1, Math.ceil(this.sociosFiltrados.length / this.tamanoPagina));
  }

  get sociosPaginados(): SocioDto[] {
    const inicio = (this.paginaActual - 1) * this.tamanoPagina;
    const fin = inicio + this.tamanoPagina;
    return this.sociosFiltrados.slice(inicio, fin);
  }

  get inicioRegistro(): number {
    if (this.sociosFiltrados.length === 0) return 0;
    return (this.paginaActual - 1) * this.tamanoPagina + 1;
  }

  get finRegistro(): number {
    return Math.min(this.paginaActual * this.tamanoPagina, this.sociosFiltrados.length);
  }

  irAPagina(pagina: number): void {
    if (pagina < 1 || pagina > this.totalPaginas) return;
    this.paginaActual = pagina;
    this.cdr.detectChanges();
  }

  paginaAnterior(): void {
    this.irAPagina(this.paginaActual - 1);
  }

  paginaSiguiente(): void {
    this.irAPagina(this.paginaActual + 1);
  }

  cambiarTamanoPagina(): void {
    this.paginaActual = 1;
    this.cdr.detectChanges();
  }

  get paginasVisibles(): number[] {
    const total = this.totalPaginas;
    const actual = this.paginaActual;

    let desde = Math.max(1, actual - 2);
    let hasta = Math.min(total, actual + 2);

    if (actual <= 3) {
      hasta = Math.min(total, 5);
    }

    if (actual >= total - 2) {
      desde = Math.max(1, total - 4);
    }

    const paginas: number[] = [];
    for (let i = desde; i <= hasta; i++) {
      paginas.push(i);
    }

    return paginas;
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
        s.activo = nuevoEstado;
        this.cerrarModalCambioEstado();
        this.statsSvc.refresh();
        this.aplicarFiltro();
        this.cdr.detectChanges();
      },
      error: (err) => {
        this.error =
          err?.error?.mensaje ||
          err?.message ||
          `Error cambiando estado del socio (${err?.status || 'sin status'})`;

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
