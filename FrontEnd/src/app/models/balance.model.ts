export interface DashboardBalanceResumen {
  ingresosMes: number;
  gastosMes: number;
  netoMes: number;
}

export interface DashboardBalanceResponse {
  resumen: DashboardBalanceResumen;
}

export interface BaseResponse<T> {
  mensaje: string;
  status: number;
  data: T;
}