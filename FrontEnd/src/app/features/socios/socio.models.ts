export interface SocioDto {
  id: number;
  dni: string;
  nombre: string;
  apellido: string;
  genero: string;
  telefono?: string | null;
  celular: string;
  activo: boolean;
  disciplinaId: number;
  disciplinaNombre?: string;
  arancelDisciplinaId?: number | null;
  categoriaArancel?: string | null;
  estadoPago?: string;
}

export interface SocioCreateRequest {
  dni: string;
  nombre: string;
  apellido: string;
  genero: string;
  telefono?: string;
  celular: string;
  disciplinaId: number;
  arancelDisciplinaId: number;
  inscripcionPagada?: boolean;
}
