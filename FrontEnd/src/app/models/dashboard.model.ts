export interface DashboardSociosResumen {
  totalSocios: number;
  activos: number;
  inactivos: number;
  alDia: number;
  debe: number;
}

export interface Socio {
  id: number;
  dni: string;
  nombre: string;
  apellido: string;
  genero: string;
  telefono: string;
  celular: string;
  disciplinaId: number;
  disciplinaNombre: string;
  vigenciaHasta: string;
  estadoPago: string;
  activo: boolean;
  arancelDisciplinaId?: number | null;
  categoriaArancel?: string | null;
}

export interface DashboardSociosResponse {
  resumen: DashboardSociosResumen;
  socios: Socio[];
}

export interface BaseResponse<T> {
  mensaje: string;
  status: number;
  data: T;
}
