export interface DashboardGeneral {
  sociosActivos: number;
  ingresosMes: number;
  gastosMes: number;
  balanceMes: number;
}

export interface BaseResponse<T> {
  mensaje: string;
  status: number;
  data: T;
}