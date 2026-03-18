import { PagoDto } from '../pagos/pago-manual.models';

export interface DeudaItemDto {
  periodo: string;
  monto: number;
  pagado: boolean;
}

export interface DeudaResponseDto {
  socioId: number;
  dni: string;
  nombreCompleto: string;
  disciplina: string;
  vigenciaHasta: string | null;
  montoMensual: number;
  mesesAdeudados: number;
  totalAdeudado: number;
  items: DeudaItemDto[];
}

export interface SocioResumenDto {
  socioId: number;
  dni: string;
  nombre: string;
  apellido: string;
  activo: boolean;
  vigenciaHasta?: string | null;
  disciplinaId: number;
  disciplinaNombre: string;
  arancelDisciplinaId?: number | null;
  categoriaArancel?: string | null;
  deuda: DeudaResponseDto | null;
  ultimosPagos: PagoDto[];
}