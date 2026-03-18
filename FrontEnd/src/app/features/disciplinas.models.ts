export interface DisciplinaDto {
  id: number;
  nombre: string;
  activa: boolean;
}

export interface CuotaDto {
  id: number;
  disciplinaId: number;
  montoTotal: number;
  vigenteDesde: string;
  activa: boolean;
}

export interface CuotaCreateRequest {
  montoTotal: number;
  vigenteDesde?: string | null;
}

export interface DisciplinaDto {
  id: number;
  nombre: string;
  activa: boolean;
}

export interface ArancelDisciplinaDto {
  id: number;
  disciplinaId: number;
  disciplinaNombre: string;
  categoria: string;
  montoSocial: number;
  montoDeportivo: number;
  montoPreparacionFisica: number;
  montoTotal: number;
  vigenteDesde: string;
  activa: boolean;
}

export interface ArancelCreateRequest {
  disciplinaId: number;
  categoria: string;
  montoSocial: number;
  montoDeportivo: number;
  montoPreparacionFisica: number;
  vigenteDesde: string;
}