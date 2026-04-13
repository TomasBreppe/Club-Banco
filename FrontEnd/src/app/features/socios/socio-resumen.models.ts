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

export interface SocioDisciplinaResumenDto {
  socioDisciplinaId?: number | null;
  disciplinaId: number;
  disciplinaNombre: string;
  arancelDisciplinaId?: number | null;
  categoriaArancel?: string | null;
  vigenciaHasta?: string | null;
  inscripcionPagada?: boolean | null;
  deuda: DeudaResponseDto | null;
}

export interface SocioResumenDto {
  socioId: number;
  dni: string;
  nombre: string;
  apellido: string;
  celular?: string | null;
  activo: boolean;
  vigenciaHasta?: string | null;
  disciplinaId: number;
  disciplinaNombre: string;
  arancelDisciplinaId?: number | null;
  categoriaArancel?: string | null;
  deuda: DeudaResponseDto | null;
  ultimosPagos: PagoDto[];

  // nuevo
  disciplinas?: SocioDisciplinaResumenDto[];
}
