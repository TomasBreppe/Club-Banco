export type GastoCategoria =
  | 'IMPUESTOS'
  | 'DISCIPLINAS'
  | 'MANTENIMIENTO'
  | 'LIMPIEZA'
  | 'EVENTOS'
  | 'HONORARIOS'
  | 'SUELDOS'
  | 'GASTOS_BANCARIOS'
  | 'OTROS';

export interface Gasto {
  id: number;
  fecha: string;
  categoria: GastoCategoria;
  concepto: string;
  descripcion: string | null;
  monto: number;
  medioPago: string | null;
  activo: boolean;
  createdAt: string;
}

export interface GastoUpdateRequest {
  fecha: string;
  categoria: string;
  concepto: string;
  descripcion: string | null;
  medioPago: string;
  monto: number;
}

export interface GastoCreateRequest {
  fecha: string;
  categoria: GastoCategoria | '';
  concepto: string;
  descripcion: string;
  monto: number | null;
  medioPago: string;
}

export interface DashboardGastosResumen {
  totalMes: number;
  cantidadGastosMes: number;
  categoriaMayorGasto: string;
}

export interface DashboardGastosResponse {
  resumen: DashboardGastosResumen;
  gastos: Gasto[];
}

export interface BaseResponse<T> {
  mensaje: string;
  status: number;
  data: T;
}
