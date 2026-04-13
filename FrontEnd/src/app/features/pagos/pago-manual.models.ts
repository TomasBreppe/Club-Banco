export interface PagoManualRequest {
  socioId: number;
  concepto: string;
  periodo: string | null;

  disciplinaId: number | null;
  arancelDisciplinaId: number | null;
  categoria: string | null;

  montoTotal: number;
  montoSocial: number;
  montoDisciplina: number;
  montoPreparacionFisica: number;

  medio: string;
  observacion: string;
}

export interface PagoDto {
  id: number;
  concepto: string;
  periodo: string | null;

  disciplinaId?: number | null;
  disciplinaNombre?: string | null;
  categoria?: string | null;

  montoTotal: number;
  montoSocial?: number | null;
  montoDisciplina?: number | null;
  montoPreparacionFisica?: number | null;

  medio: string | null;
  observacion?: string | null;
  fechaPago: string;
  mpPaymentId?: string | null;
  mpStatus?: string | null;

  anulado?: boolean;
  fechaAnulacion?: string | null;
  motivoAnulacion?: string | null;
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
