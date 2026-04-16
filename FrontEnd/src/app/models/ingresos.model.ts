export interface IngresoDashboardItem {
  origen: string;
  id: number;
  fecha: string;
  socioNombreCompleto: string;
  disciplinaNombre: string | null;
  categoria: string | null;
  concepto: string | null;
  periodo: string | null;
  medio: string | null;
  monto: number;
  descripcion: string | null;
}

export interface DashboardIngresosResumen {
  totalMes: number;
  cantidadPagosMes: number;
  medioMasUsado: string;
}

export interface DashboardIngresosResponse {
  resumen: DashboardIngresosResumen;
  ingresos: IngresoDashboardItem[];
}

export interface BaseResponse<T> {
  mensaje: string;
  status: number;
  data: T;
}
