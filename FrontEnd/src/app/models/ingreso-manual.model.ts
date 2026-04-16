export interface IngresoManualCreateRequest {
  fecha: string;
  categoria: string;
  medioPago: string;
  monto: number | null;
  descripcion: string;
}

export interface IngresoManualDto {
  id: number;
  fecha: string;
  categoria: string;
  medioPago: string;
  monto: number;
  descripcion: string | null;
}

export interface IngresoManual {
  id: number;
  fecha: string;
  categoria: string;
  medioPago: string;
  monto: number;
  descripcion: string;
  concepto?: string | null;
}

export interface IngresoManualCreateRequest {
  fecha: string;
  categoria: string;
  medioPago: string;
  monto: number | null;
  descripcion: string;
  concepto?: string | null;
}

export interface BaseResponse<T> {
  mensaje: string;
  status: number;
  data: T;
}
